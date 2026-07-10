package lunxkoe.practice.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lunxkoe.practice.domain.auth.dto.request.SignInRequest;
import lunxkoe.practice.domain.user.dto.response.UserDto;
import lunxkoe.practice.domain.user.entity.User;
import lunxkoe.practice.domain.user.repository.UserRepository;
import lunxkoe.practice.global.exception.CustomException;
import lunxkoe.practice.global.exception.ErrorCode;
import lunxkoe.practice.global.jwt.JwtProvider;
import lunxkoe.practice.global.security.SessionRegistry;
import lunxkoe.practice.global.security.UserSession;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final int MAX_LOGIN_FAIL_COUNT = 5;
    private static final Duration LOGIN_FAIL_TTL = Duration.ofMinutes(30);
    private static final String LOGIN_FAIL_KEY_PREFIX = "login-fail:";

    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final SessionRegistry sessionRegistry;
    private final StringRedisTemplate redisTemplate;
//    private final TemporaryPasswordRepository temporaryPasswordRepository;

    public record SignInResult(UserDto userDto, String accessToken, String refreshToken) {}

    @Transactional
    public SignInResult signIn(SignInRequest request, String userAgent) {

        // 아이디 검증
        User foundUser = userRepository.findByEmail(request.username())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        // 계정 잠금 확인
        if (foundUser.isLocked()) {
            throw new CustomException(ErrorCode.ACCOUNT_LOCKED);
        }

        // 비밀번호 검증
        boolean authenticated = passwordEncoder.matches(request.password(), foundUser.getPassword());
//                || matchesTemporaryPassword(user.getEmail(), rawPassword);

        // 로그인 실패 처리 (최대 시도 제한 - 계정 잠금)
        if (!authenticated) {
            registerLoginFailure(foundUser);
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        clearLoginFailure(foundUser.getId());

        // 정상 로그인
        UserSession session = sessionRegistry.issue(foundUser.getId(), userAgent);
        String accessToken = jwtProvider.createAccessToken(foundUser.getId(), foundUser.getRole(), session.sessionId(), session.issuedAt());
        String refreshToken = jwtProvider.createRefreshToken(foundUser.getId(), session.sessionId(), session.currentRefreshJti(), session.issuedAt());

        return new SignInResult(UserDto.from(foundUser), accessToken, refreshToken);
    }

//    private boolean matchesTemporaryPassword(String email, String rawPassword) {
//        return temporaryPasswordRepository.findById(email)
//                .map(TemporaryPassword::getEncodedPassword)
//                .map(encoded -> passwordEncoder.matches(rawPassword, encoded))
//                .orElse(false);
//    }

    // NOW: 트랜잭션 문제 해결
    private void registerLoginFailure(User user) {
        String key = key(user.getId());
        Long count = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, LOGIN_FAIL_TTL);
        if (count != null && count >= MAX_LOGIN_FAIL_COUNT) {
            user.lock();
            // 세션도 닫아버림
            // - 누군가 로그인 시도 감지
            sessionRegistry.revoke(user.getId());
            log.warn("5회 연속 로그인 실패로 계정이 잠겼습니다. email={}", user.getEmail());
        }
    }

    private void clearLoginFailure(UUID userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(UUID userId) {
        return LOGIN_FAIL_KEY_PREFIX + userId.toString();
    }
}
