package lunxkoe.practice.domain.auth.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        String errorMessage = exception instanceof OAuth2AuthenticationException oauthEx
                ? oauthEx.getError().getErrorCode()
                : "unknown_error";

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl + "/#/auth/login")
                .queryParam("error", "oauth_failed")
                .queryParam("error_message", errorMessage)
                .build().toUriString();

        response.sendRedirect(redirectUrl);
    }
}
