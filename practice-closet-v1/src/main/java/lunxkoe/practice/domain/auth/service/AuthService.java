package lunxkoe.practice.domain.auth.service;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lunxkoe.practice.domain.auth.dto.request.SignInRequest;
import lunxkoe.practice.domain.auth.dto.response.JwtDto;
import lunxkoe.practice.domain.user.entity.User;
import lunxkoe.practice.domain.user.repository.UserRepository;
import lunxkoe.practice.global.exception.BusinessException;
import lunxkoe.practice.global.exception.ErrorCode;
import lunxkoe.practice.global.security.CustomUserDetails;
import lunxkoe.practice.global.security.jwt.JwtStatus;
import lunxkoe.practice.global.security.jwt.JwtUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;

    @Transactional
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

    @Transactional(readOnly = true)
    public JwtDto refresh(String refreshToken) {

        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException(ErrorCode.MISSING_TOKEN);
        }

        JwtStatus jwtStatus = jwtUtil.validateToken(refreshToken, false);
        if (jwtStatus != JwtStatus.VALID) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Claims claims = jwtUtil.getClaims(refreshToken, false);
        String externalId = claims.getSubject();
        String oldSessionId = claims.get("loginSessionId", String.class);

        String activeSessionId = redisTemplate.opsForValue().get("USER_SESSION:" + externalId);

        // 토큰 탈취 감지
        if (!StringUtils.hasText(activeSessionId) || !activeSessionId.equals(oldSessionId)) {
            log.warn("비정상적인 Refresh Token 접근 감지! 계정 보호를 위해 모든 세션을 파기합니다.");
            redisTemplate.delete("USER_SESSION:" + externalId);
            throw new BusinessException(ErrorCode.COMPROMISED_TOKEN);
        }

        // 정상 재발급 로직
        User foundUser = userRepository.findByExternalId(UUID.fromString(externalId))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // RTR: 새로운 세션 ID 발급
        String newLoginSessionId = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                "USER_SESSION:" + externalId,
                newLoginSessionId,
                jwtUtil.getRefreshTokenValidityInSeconds(),
                TimeUnit.SECONDS
        );

        // Access Token 발급
        String newAccessToken = jwtUtil.createAccessToken(foundUser, newLoginSessionId);

        // Refresh Token 발급
        String newRefreshToken = jwtUtil.createRefreshToken(foundUser, newLoginSessionId);

        return JwtDto.from(foundUser, newAccessToken, newRefreshToken);
    }

    public void signOut(CustomUserDetails userDetails) {
        String externalId = userDetails.getUser().getExternalId().toString();
        redisTemplate.delete("USER_SESSION:" + externalId);
        log.info("정상적인 로그아웃");
    }
}
