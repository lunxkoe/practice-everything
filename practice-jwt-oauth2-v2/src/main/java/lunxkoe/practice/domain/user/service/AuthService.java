package lunxkoe.practice.domain.user.service;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lunxkoe.practice.domain.user.dto.request.ResetPasswordRequest;
import lunxkoe.practice.domain.user.dto.request.SignInRequest;
import lunxkoe.practice.domain.user.dto.response.JwtDto;
import lunxkoe.practice.domain.user.entity.TmpPassword;
import lunxkoe.practice.domain.user.entity.User;
import lunxkoe.practice.domain.user.repository.TmpPasswordRepository;
import lunxkoe.practice.domain.user.repository.UserRepository;
import lunxkoe.practice.global.exception.controller.CustomException;
import lunxkoe.practice.global.exception.ErrorCode;
import lunxkoe.practice.global.security.jwt.JwtProvider;
import lunxkoe.practice.global.security.jwt.JwtStatus;
import lunxkoe.practice.global.security.jwt.RefreshToken;
import lunxkoe.practice.global.security.jwt.RefreshTokenRepository;
import lunxkoe.practice.global.security.jwt.exception.TokenHijackedException;
import lunxkoe.practice.global.security.userdetails.CustomUserDetails;
import lunxkoe.practice.global.security.util.TokenHashUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    private final UserRepository userRepository;
    private final TmpPasswordRepository tmpPasswordRepository;

    @Transactional
    public JwtDto signIn(SignInRequest request) {

        // 1. 로그인 수행
        UsernamePasswordAuthenticationToken tmpAuthToken = new UsernamePasswordAuthenticationToken(request.username(), request.password());
        Authentication authentication = null;
        try {
            authentication = authenticationManager.authenticate(tmpAuthToken);
        } catch (BadCredentialsException e) {
            // 비밀번호 불일치
            throw new CustomException(ErrorCode.INVALID_USERNAME_OR_PASSWORD);
        } catch (LockedException e) {
            // 계정 잠김 상태 처리 (ErrorCode에 ACCOUNT_LOCKED 등 추가 필요)
            throw new CustomException(ErrorCode.ACCOUNT_LOCKED);
        } catch (AuthenticationException e) {
            // 그 외 기타 시큐리티 예외
            throw new CustomException(ErrorCode.UNAUTHORIZED_USER);
        }

        User foundUser = ((CustomUserDetails) authentication.getPrincipal()).getUser();

        // 추가: 비밀번호 초기화시만 동작
        if ("TMP_LOGIN".equals(authentication.getDetails())) {
            return JwtDto.from(foundUser, null, null);
        }

        // 2. AccessToken 발급
        String newAccessToken = jwtProvider.createAccessToken(foundUser.getEmail(), foundUser.getUserRole().name(), foundUser.getId());

        // 3. RefreshToken 발급
        String newRefreshToken = jwtProvider.createRefreshToken(foundUser.getEmail(), foundUser.getId());
        String hashRefreshToken = TokenHashUtil.hash(newRefreshToken);
        LocalDateTime newExpiredAt = dateToLocalDateTime(jwtProvider.getExpiration(newRefreshToken, false));

        RefreshToken refreshTokenEntity = refreshTokenRepository.findByUserId(foundUser.getId())
                .map(entity -> {
                    entity.update(hashRefreshToken, newExpiredAt);
                    return entity;
                })
                .orElseGet(() -> {
                    return RefreshToken.builder()
                            .userId(foundUser.getId())
                            .token(hashRefreshToken)
                            .expiredAt(newExpiredAt)
                            .build();
                });
        refreshTokenRepository.save(refreshTokenEntity);

        return JwtDto.from(foundUser, newAccessToken, newRefreshToken);
    }

    @Transactional
    public JwtDto refresh(String refreshToken) {

        if (refreshToken == null) {
            throw new CustomException(ErrorCode.TOKEN_NULL); // 재로그인
        }

        JwtStatus jwtStatus = jwtProvider.validateToken(refreshToken, false);
        if (jwtStatus == JwtStatus.EXPIRED) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN); // 재로그인
        } else if (jwtStatus == JwtStatus.INVALID) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_USER); // 재로그인
        }

        Claims claims = jwtProvider.getClaims(refreshToken, false);
        String email = claims.getSubject();

        User foundUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // ?: 좀 과보호 같긴한데?
        // - 토큰 자체를 해당 서버에서 발급한게 맞다는 전제
        //      - 기록 자체가 없다. -> 보안 위협 (데이터가 애초에 없거나(무결성 문제) 누군가 이미 해당 토큰으로 다른 토큰을 받았다는 것)
        //      - 일치하는 토큰이 없다. -> 보안 위협 (데이터가 애초에 없거나(무결성 문제) 누군가 이미 해당 토큰으로 다른 토큰을 받았다는 것)
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByTokenAndUserId(TokenHashUtil.hash(refreshToken), foundUser.getId())
                .orElseThrow(() -> new TokenHijackedException(foundUser.getId()));
        // TODO: 이미 발급 된 Access Token 무효화를 어떻게 할 것인가 -> 다른 기기 로그인과도 연동 될 것 같은 문제

        // 통과 완료
        // AccessToken 재발급
        String newAccessToken = jwtProvider.createAccessToken(foundUser.getEmail(), foundUser.getUserRole().name(), foundUser.getId());

        // RefreshToken 재발급
        String newRefreshToken = jwtProvider.createRefreshToken(foundUser.getEmail(), foundUser.getId());
        String hashRefreshToken = TokenHashUtil.hash(newRefreshToken);
        LocalDateTime newExpiredAt = dateToLocalDateTime(jwtProvider.getExpiration(newRefreshToken, false));
        refreshTokenEntity.update(hashRefreshToken, newExpiredAt);

        return JwtDto.from(foundUser, newAccessToken, newRefreshToken);
    }

    @Transactional
    public String resetPassword(ResetPasswordRequest request) {

        userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String tmpPassword = generateTempPassword();
        String encodedTmpPassword = passwordEncoder.encode(tmpPassword);

        TmpPassword tmpPasswordEntity = tmpPasswordRepository.findByEmail(request.email())
                .map(entity -> {
                    entity.update(encodedTmpPassword);
                    return entity;
                })
                .orElseGet(() ->
                        TmpPassword.builder()
                                .email(request.email())
                                .tmpPassword(encodedTmpPassword)
                                .build()
                );

        tmpPasswordRepository.save(tmpPasswordEntity);

        return tmpPassword;
    }

    private LocalDateTime dateToLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private String generateTempPassword() {
        char[] charSet = new char[] {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
                'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
                'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
                'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
        };

        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        // 10자리의 임시 비밀번호 생성
        int idx = 0;
        for (int i = 0; i < 10; i++) {
            idx = random.nextInt(charSet.length);
            sb.append(charSet[idx]);
        }
        return sb.toString();
    }
}
