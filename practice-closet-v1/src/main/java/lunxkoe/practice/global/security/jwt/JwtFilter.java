package lunxkoe.practice.global.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lunxkoe.practice.domain.user.entity.User;
import lunxkoe.practice.global.enums.UserRole;
import lunxkoe.practice.global.exception.ErrorCode;
import lunxkoe.practice.global.exception.ErrorResponse;
import lunxkoe.practice.global.security.CustomUserDetails;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring("Bearer ".length());
        JwtStatus jwtStatus = jwtUtil.validateToken(token, true);
        if (jwtStatus == JwtStatus.EXPIRED) {
            sendErrorResponse(response, ErrorCode.EXPIRED_TOKEN);
            return;
        } else if (jwtStatus == JwtStatus.INVALID) {
            sendErrorResponse(response, ErrorCode.INVALID_TOKEN);
            return;
        }

        // Redis Session 검증
        Claims claims = jwtUtil.getClaims(token, true);
        UUID userExternalId = UUID.fromString(claims.getSubject());
        String loginSessionId = claims.get("loginSessionId", String.class);
        String activeSessionId = redisTemplate.opsForValue().get("USER_SESSION:" + userExternalId);

        if (!StringUtils.hasText(activeSessionId) || !activeSessionId.equals(loginSessionId)) {
            log.warn("중복 로그인 감지 또는 만료된 세션 접근 - User: {}", userExternalId);
            sendErrorResponse(response, ErrorCode.CONCURRENT_LOGIN_DETECTED);
            return;
        }

        // 임시 로그인 시 토큰 남용 방어 로직
        Boolean isTemp = claims.get("isTemp", Boolean.class);
        log.info(isTemp.toString());
        String requestURI = request.getRequestURI();
        if (Boolean.TRUE.equals(isTemp) && !requestURI.equals("/api/auth/change-password")) {
            log.warn("임시 토큰으로 비정상적인 API 접근 시도 차단! URI: {}, User: {}", requestURI, userExternalId);
            sendErrorResponse(response, ErrorCode.ACCESS_DENIED);
            return;
        }

        // 임시 인증 객체 생성
        UserRole role = UserRole.valueOf(claims.get("role", String.class));

        CustomUserDetails userDetails = new CustomUserDetails(
                User.createUserByUsingJwtFilter(userExternalId, role)
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = ErrorResponse.of(errorCode);
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
