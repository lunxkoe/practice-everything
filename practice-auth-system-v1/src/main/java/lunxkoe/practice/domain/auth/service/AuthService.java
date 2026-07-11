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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final SessionRegistry sessionRegistry;
//    private final TemporaryPasswordRepository temporaryPasswordRepository;

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
        boolean authenticated = passwordEncoder.matches(request.password(), foundUser.getPassword());
//                || matchesTemporaryPassword(user.getEmail(), rawPassword);

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

//    private boolean matchesTemporaryPassword(String email, String rawPassword) {
//        return temporaryPasswordRepository.findById(email)
//                .map(TemporaryPassword::getEncodedPassword)
//                .map(encoded -> passwordEncoder.matches(rawPassword, encoded))
//                .orElse(false);
//    }
}
