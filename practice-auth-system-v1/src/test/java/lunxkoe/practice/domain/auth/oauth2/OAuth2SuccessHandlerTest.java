package lunxkoe.practice.domain.auth.oauth2;

import lunxkoe.practice.domain.user.entity.User;
import lunxkoe.practice.global.jwt.JwtProvider;
import lunxkoe.practice.global.jwt.RefreshCookieProvider;
import lunxkoe.practice.global.security.SessionRegistry;
import lunxkoe.practice.global.security.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @Mock
    JwtProvider jwtProvider;
    @Mock
    SessionRegistry sessionRegistry;
    @Mock
    RefreshCookieProvider refreshCookieProvider;

    @InjectMocks
    OAuth2SuccessHandler oAuth2SuccessHandler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(oAuth2SuccessHandler, "frontendBaseUrl", "https://frontend.example.com");
    }

    @Test
    void 로그인_성공하면_세션을_발급하고_refresh_토큰만_쿠키에_담아_프론트로_리다이렉트한다() throws Exception {
        User user = User.createLocalUser("우디", "woody@example.com", "ENCODED_PW");
        CustomOAuth2User principal = new CustomOAuth2User(user, Map.of("sub", "google-sub-123"));
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        UserSession session = new UserSession(UUID.randomUUID(), UUID.randomUUID(), "chrome", Instant.now());
        given(sessionRegistry.issue(user.getId(), "chrome")).willReturn(session);
        given(jwtProvider.createRefreshToken(user.getId(), session.sessionId(), session.currentRefreshJti(), session.issuedAt()))
                .willReturn("refresh-token-value");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.USER_AGENT, "chrome");
        MockHttpServletResponse response = new MockHttpServletResponse();

        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

        verify(refreshCookieProvider).attach(response, "refresh-token-value");
        // accessToken은 더 이상 만들지도, URL에 싣지도 않는다 - 프론트가 착지 후 /api/auth/refresh를 호출하는 구조
        assertThat(response.getRedirectedUrl()).isEqualTo("https://frontend.example.com");
    }
}
