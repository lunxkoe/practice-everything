package lunxkoe.practice.domain.auth.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lunxkoe.practice.domain.user.entity.User;
import lunxkoe.practice.global.jwt.JwtProvider;
import lunxkoe.practice.global.jwt.RefreshCookieProvider;
import lunxkoe.practice.global.security.SessionRegistry;
import lunxkoe.practice.global.security.UserSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final SessionRegistry sessionRegistry;
    private final RefreshCookieProvider refreshCookieProvider;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();
        User user = principal.getUser();

        UserSession session = sessionRegistry.issue(user.getId(), request.getHeader("User-Agent"));
        String refreshToken = jwtProvider.createRefreshToken(user.getId(), session.sessionId(), session.currentRefreshJti(), session.issuedAt());

        refreshCookieProvider.attach(response, refreshToken);

        response.sendRedirect(frontendBaseUrl);
    }
}
