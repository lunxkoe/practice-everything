package lunxkoe.practice.domain.auth.oauth2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2FailureHandlerTest {

    private final OAuth2FailureHandler oAuth2FailureHandler = new OAuth2FailureHandler();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(oAuth2FailureHandler, "frontendBaseUrl", "https://frontend.example.com");
    }

    @Test
    void OAuth2AuthenticationException이면_에러코드를_error_message로_실어_리다이렉트한다() throws Exception {
        OAuth2AuthenticationException exception =
                new OAuth2AuthenticationException(new OAuth2Error("account_locked", "계정이 잠겨 있습니다.", null));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        oAuth2FailureHandler.onAuthenticationFailure(request, response, exception);

        // 쿼리 파라미터가 '#' 앞(루트 경로)에 붙는 구조 - 의도된 것으로 확인된 현재 동작을 고정해둠
        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://frontend.example.com/?error=oauth_failed&error_message=account_locked#/auth/login");
    }

    @Test
    void OAuth2AuthenticationException이_아니면_unknown_error로_리다이렉트한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        oAuth2FailureHandler.onAuthenticationFailure(request, response, new BadCredentialsException("bad"));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://frontend.example.com/?error=oauth_failed&error_message=unknown_error#/auth/login");
    }
}
