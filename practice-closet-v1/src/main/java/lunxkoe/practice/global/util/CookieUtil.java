package lunxkoe.practice.global.util;

import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    private static final String REFRESH_TOKEN = "REFRESH_TOKEN";

    @Value("${jwt.refresh-token-validity-in-seconds}")
    private int refreshTokenValidityInSeconds;

    private Cookie createCookie(String key, String value, String path, int maxAge) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(maxAge);
//        cookie.setSecure(true);
        cookie.setPath(path);
        cookie.setHttpOnly(true);
        return cookie;
    }

    public Cookie createRefreshCookie(String refreshToken) {
        return createCookie(REFRESH_TOKEN, refreshToken, "/api/auth/refresh", refreshTokenValidityInSeconds);
    }
}
