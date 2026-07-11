package lunxkoe.practice.domain.auth.service;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lunxkoe.practice.domain.auth.dto.request.SignInRequest;
import lunxkoe.practice.domain.auth.entity.TemporaryPassword;
import lunxkoe.practice.domain.auth.repository.TemporaryPasswordRepository;
import lunxkoe.practice.domain.user.dto.response.UserDto;
import lunxkoe.practice.domain.user.entity.User;
import lunxkoe.practice.domain.user.repository.UserRepository;
import lunxkoe.practice.global.exception.CustomException;
import lunxkoe.practice.global.exception.ErrorCode;
import lunxkoe.practice.global.jwt.JwtProvider;
import lunxkoe.practice.global.security.SessionRegistry;
import lunxkoe.practice.global.security.UserSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final SessionRegistry sessionRegistry;
    private final TemporaryPasswordRepository temporaryPasswordRepository;

    private final LoginFailureService loginFailureService;

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
        boolean authenticated = passwordEncoder.matches(request.password(), foundUser.getPassword())
                || matchesTemporaryPassword(request.username(), request.password());

        // 로그인 실패 처리 (최대 시도 제한 - 계정 잠금)
        if (!authenticated) {
            loginFailureService.registerLoginFailure(foundUser);
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        loginFailureService.clearLoginFailure(foundUser.getId());

        // 정상 로그인
        UserSession session = sessionRegistry.issue(foundUser.getId(), userAgent);
        String accessToken = jwtProvider.createAccessToken(foundUser.getId(), foundUser.getRole(), session.sessionId(), session.issuedAt());
        String refreshToken = jwtProvider.createRefreshToken(foundUser.getId(), session.sessionId(), session.currentRefreshJti(), session.issuedAt());

        return new SignInResult(UserDto.from(foundUser), accessToken, refreshToken);
    }

    @Transactional
    public SignInResult refresh(String refreshToken, String userAgent) {

        if (refreshToken == null) {
            throw new CustomException(ErrorCode.TOKEN_NEEDED);
        }

        Claims claims = jwtProvider.parseClaims(refreshToken);
        if (!"refresh".equals(claims.get("typ", String.class))) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        UUID userId = UUID.fromString(claims.getSubject());
        UUID sid = UUID.fromString(claims.get("sid", String.class));
        UUID jti = UUID.fromString(claims.getId());

        UserSession session = sessionRegistry.find(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_EXPIRED));

        if (!session.sessionId().equals(sid)) {
            throw new CustomException(ErrorCode.SESSION_EXPIRED); // 재로그인 강제
        }

        if (!session.currentRefreshJti().equals(jti)) {
            sessionRegistry.revoke(userId);
            log.warn("Refresh Token 재사용 감지: userId = {}, userAgent = {}", userId, userAgent);
            throw new CustomException(ErrorCode.REFRESH_TOKEN_REUSED);
            // TODO: 재사용 감지 시 사용자의 이메일로 전송하는 것도 괜찮을지도? (비동기 이메일 서비스)
        }

        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        UserSession rotated = sessionRegistry.rotate(userId, sid, userAgent);
        String newAccessToken = jwtProvider.createAccessToken(foundUser.getId(), foundUser.getRole(), rotated.sessionId(), rotated.issuedAt());
        String newRefreshToken = jwtProvider.createRefreshToken(foundUser.getId(), rotated.sessionId(), rotated.currentRefreshJti(), rotated.issuedAt());

        return new SignInResult(UserDto.from(foundUser), newAccessToken, newRefreshToken);
    }

    @Transactional
    public void signOut(UUID userId) {
        sessionRegistry.revoke(userId);
    }

    private boolean matchesTemporaryPassword(String email, String rawPassword) {
        return temporaryPasswordRepository.findById(email)
                .map(TemporaryPassword::getEncodedPassword)
                .map(encoded -> passwordEncoder.matches(rawPassword, encoded))
                .orElse(false);
    }
}
