package lunxkoe.practice.global.jwt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshCookieProvider {

    @Value("${app.cookie.secure}")
    private boolean secure;

    private final JwtProperties jwtProperties;

    public void attach(HttpServletResponse response, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, build(refreshToken, Duration.ofDays(jwtProperties.refreshTokenExpirationDays())).toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, build("", Duration.ZERO).toString());
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from("REFRESH_TOKEN", value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
    }
}
