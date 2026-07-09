package lunxkoe.practice.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lunxkoe.practice.domain.auth.dto.request.SignInRequest;
import lunxkoe.practice.domain.auth.dto.response.JwtDto;
import lunxkoe.practice.domain.user.entity.User;
import lunxkoe.practice.global.exception.BusinessException;
import lunxkoe.practice.global.exception.ErrorCode;
import lunxkoe.practice.global.security.CustomUserDetails;
import lunxkoe.practice.global.security.jwt.JwtUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;

    public JwtDto signIn(SignInRequest request) {

        // 로그인
        UsernamePasswordAuthenticationToken signInToken = new UsernamePasswordAuthenticationToken(request.username(), request.password());
        Authentication authentication = null;

        try {
            authentication = authenticationManager.authenticate(signInToken);
        } catch (BadCredentialsException e) {
            // 비밀번호 불일치
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        } catch (LockedException e) {
            // 계정 잠김 상태 처리 (ErrorCode에 ACCOUNT_LOCKED 등 추가 필요)
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        } catch (AuthenticationException e) {
            // 그 외 기타 시큐리티 예외
//            throw new BusinessException(ErrorCode.UNAUTHORIZED_USER);
        }

        User foundUser = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        String loginSessionId = UUID.randomUUID().toString();

        // Redis [Key = 유저아이디, Value = loginSessionId]
        redisTemplate.opsForValue().set(
                "USER_SESSION:" + foundUser.getExternalId().toString(),
                loginSessionId,
                jwtUtil.getRefreshTokenValidityInSeconds(),
                TimeUnit.SECONDS
        );

        // Access Token 발급
        String newAccessToken = jwtUtil.createAccessToken(foundUser, loginSessionId);

        // Refresh Token 발급
        String newRefreshToken = jwtUtil.createRefreshToken(foundUser, loginSessionId);

        return JwtDto.from(foundUser, newAccessToken, newRefreshToken);
    }
}
