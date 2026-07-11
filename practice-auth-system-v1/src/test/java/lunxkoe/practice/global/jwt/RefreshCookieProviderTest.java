package lunxkoe.practice.global.jwt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RefreshCookieProviderTest {

    @Mock
    JwtProperties jwtProperties;

    @Test
    void attach는_리프레시_토큰_만료일수만큼_maxAge를_갖는_httpOnly_쿠키를_내려준다() {
        given(jwtProperties.refreshTokenExpirationDays()).willReturn(7L);
        RefreshCookieProvider provider = new RefreshCookieProvider(jwtProperties);
        ReflectionTestUtils.setField(provider, "secure", true);

        MockHttpServletResponse response = new MockHttpServletResponse();
        provider.attach(response, "refresh-token-value");

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("REFRESH_TOKEN=refresh-token-value");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Secure");
        assertThat(setCookie).contains("SameSite=Strict");
        assertThat(setCookie).contains("Path=/api/auth");
        assertThat(setCookie).contains("Max-Age=" + Duration.ofDays(7).getSeconds());
    }

    @Test
    void secure값이_설정되지_않으면_기본값_false로_Secure_속성이_빠진다() {
        given(jwtProperties.refreshTokenExpirationDays()).willReturn(7L);
        RefreshCookieProvider provider = new RefreshCookieProvider(jwtProperties); // secure 필드 미설정 -> 기본값 false

        MockHttpServletResponse response = new MockHttpServletResponse();
        provider.attach(response, "token");

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).doesNotContain("Secure");
    }

    @Test
    void clear는_값을_비우고_Max_Age_0으로_즉시_만료시킨다() {
        RefreshCookieProvider provider = new RefreshCookieProvider(jwtProperties);

        MockHttpServletResponse response = new MockHttpServletResponse();
        provider.clear(response);

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("REFRESH_TOKEN=");
        assertThat(setCookie).contains("Max-Age=0");
    }
}
