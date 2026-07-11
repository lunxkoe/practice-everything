package lunxkoe.practice.global.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lunxkoe.practice.global.common.enums.UserRole;
import lunxkoe.practice.global.exception.CustomException;
import lunxkoe.practice.global.exception.ErrorCode;
import lunxkoe.practice.global.exception.ErrorResponse;
import lunxkoe.practice.global.exception.ErrorResponseWriter;
import lunxkoe.practice.global.security.SessionRegistry;
import lunxkoe.practice.global.security.UserSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_ = "Bearer ";

    private final JwtProvider jwtProvider;
    private final SessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            authenticateAccessToken(token);
        } catch (CustomException e) {
            ErrorResponseWriter.write(response, objectMapper, e);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (StringUtils.hasText(authorization) && authorization.startsWith(BEARER_)) {
            return authorization.substring(BEARER_.length());
        }

        return null;
    }

    private void authenticateAccessToken(String token) {

        Claims claims = jwtProvider.parseClaims(token);
        if (!"access".equals(claims.get("typ", String.class))) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        UUID userId = UUID.fromString(claims.getSubject());
        UUID sid = UUID.fromString(claims.get("sid", String.class));

        UserSession session = sessionRegistry.find(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_EXPIRED));
        if (!session.sessionId().equals(sid)) {
            throw new CustomException(ErrorCode.SESSION_EXPIRED);
        }

        UserRole role = UserRole.valueOf(claims.get("role", String.class));
        UserPrincipal principal = new UserPrincipal(userId, role);
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role.name()));
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
