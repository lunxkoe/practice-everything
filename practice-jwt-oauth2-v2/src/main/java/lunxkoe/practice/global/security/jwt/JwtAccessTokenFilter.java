package lunxkoe.practice.global.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lunxkoe.practice.domain.user.entity.User;
import lunxkoe.practice.global.common.enums.UserRole;
import lunxkoe.practice.global.exception.ErrorCode;
import lunxkoe.practice.global.exception.ErrorResponse;
import lunxkoe.practice.global.security.userdetails.CustomUserDetails;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@RequiredArgsConstructor
public class JwtAccessTokenFilter extends OncePerRequestFilter {

    private static final String ROLE = "role";
    private static final String USER_ID = "userId";
    private static final String ACCESS_TOKEN_HEADER = "Authorization";
    private static final String BEARER = "Bearer ";

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authorization = request.getHeader(ACCESS_TOKEN_HEADER);

        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(BEARER.length());
        JwtStatus jwtStatus = jwtProvider.validateToken(token, true); // True에 이미 AccessToken인지 검증이 된 것
        if (jwtStatus == JwtStatus.EXPIRED) {
            sendErrorResponse(response, ErrorCode.EXPIRED_TOKEN);
            // TODO: 클라언트에서 "/api/auth/refresh" 또는 서버 자체에서 리다이렉션? - 그런데 refreshToken이 없다? 그러면 재로그인
            return;
        } else if (jwtStatus == JwtStatus.INVALID) {
            sendErrorResponse(response, ErrorCode.UNAUTHORIZED_USER);
            return;
        }

        // 통과
        Claims claims = jwtProvider.getClaims(token, true);

        String email = claims.getSubject();
        String userId = claims.get(USER_ID, String.class);
        String role = claims.get(ROLE, String.class);

        CustomUserDetails userDetails = new CustomUserDetails(
                User.builder()
                        .id(UUID.fromString(userId))
                        .name("tmp_name")
                        .email(email)
                        .password("tmp_pwd")
                        .userRole(UserRole.valueOf(role))
                        .build()
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    // 쓰레기 Token 유입 방지
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 오직 "POST" 메서드일 때만 필터를 건너뛸 주소들
        String[] postExcludePaths = {
                "/api/users",           // 회원가입
                "/api/auth/sign-in",    // 로그인
                "/api/auth/refresh",     // 토큰 재발rmq
                "/api/auth/reset-password"
        };

        // 현재 요청이 POST이고, 위 주소들 중 하나와 일치한다면 true 반환 (필터 프리패스)
        if ("POST".equalsIgnoreCase(method) && PatternMatchUtils.simpleMatch(postExcludePaths, path)) {
            return true;
        }

        // HTTP 메서드 상관없이 무조건 필터를 건너뛸 주소 (예: 루트 경로나 Health Check)
        if (path.equals("/")) {
            return true;
        }

        // 위 조건들에 해당하지 않으면 false 반환 (정상적으로 JWT 검증 수행)
        return false;
    }

    private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = ErrorResponse.of(errorCode);
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
