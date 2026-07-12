package lunxkoe.practice.domain.auth.oauth2;

import com.sun.net.httpserver.HttpServer;
import lunxkoe.practice.domain.auth.entity.SocialAccount;
import lunxkoe.practice.domain.auth.entity.SocialProvider;
import lunxkoe.practice.domain.auth.repository.SocialAccountRepository;
import lunxkoe.practice.domain.user.entity.User;
import lunxkoe.practice.domain.user.repository.UserRepository;
import lunxkoe.practice.global.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * loadUser()는 부모(DefaultOAuth2UserService)가 실제로 userinfo 엔드포인트에 HTTP 요청을 보내기 때문에,
 * 로컬 HttpServer로 구글 userinfo 응답을 흉내내서 진짜 요청-파싱 경로를 그대로 태운다.
 */
@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    SocialAccountRepository socialAccountRepository;
    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    CustomOAuth2UserService customOAuth2UserService;

    private HttpServer fakeGoogleServer;
    private int port;

    @BeforeEach
    void startFakeGoogleUserInfoServer() throws IOException {
        fakeGoogleServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        fakeGoogleServer.createContext("/userinfo", exchange -> {
            String json = "{\"sub\":\"google-sub-123\",\"email\":\"woody@example.com\",\"name\":\"우디\",\"email_verified\":true}";
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        fakeGoogleServer.start();
        port = fakeGoogleServer.getAddress().getPort();
    }

    @AfterEach
    void stopFakeGoogleUserInfoServer() {
        fakeGoogleServer.stop(0);
    }

    private OAuth2UserRequest googleUserRequest() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/login/oauth2/code/google")
                .authorizationUri("http://localhost:" + port + "/authorize")
                .tokenUri("http://localhost:" + port + "/token")
                .userInfoUri("http://localhost:" + port + "/userinfo")
                .userNameAttributeName("sub")
                .scope("profile", "email")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "fake-access-token", Instant.now(), Instant.now().plusSeconds(3600));

        return new OAuth2UserRequest(registration, accessToken);
    }

    private User sampleUser() {
        return User.createLocalUser("우디", "woody@example.com", "ENCODED_PW");
    }

    @Test
    void 이미_연결된_소셜계정이면_기존_User를_그대로_반환한다() {
        User existingUser = sampleUser();
        SocialAccount linked = SocialAccount.of(existingUser, SocialProvider.GOOGLE, "google-sub-123");
        given(socialAccountRepository.findByProviderAndProviderUserId(SocialProvider.GOOGLE, "google-sub-123"))
                .willReturn(Optional.of(linked));

        OAuth2User result = customOAuth2UserService.loadUser(googleUserRequest());

        CustomOAuth2User principal = (CustomOAuth2User) result;
        assertThat(principal.getUser()).isEqualTo(existingUser);
        verify(userRepository, never()).save(any());
        verify(socialAccountRepository, never()).save(any());
    }

    @Test
    void 연결된_계정이_잠겨있으면_OAuth2AuthenticationException을_던진다() {
        User lockedUser = sampleUser();
        lockedUser.lock();
        SocialAccount linked = SocialAccount.of(lockedUser, SocialProvider.GOOGLE, "google-sub-123");
        given(socialAccountRepository.findByProviderAndProviderUserId(SocialProvider.GOOGLE, "google-sub-123"))
                .willReturn(Optional.of(linked));

        assertThatThrownBy(() -> customOAuth2UserService.loadUser(googleUserRequest()))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(e -> {
                    OAuth2AuthenticationException oe = (OAuth2AuthenticationException) e;
                    assertThat(oe.getError().getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_LOCKED.name());
                });
    }

    @Test
    void 소셜계정은_없지만_같은_이메일의_로컬_계정이_있으면_그_계정에_연결한다() {
        User existingLocalUser = sampleUser();
        given(socialAccountRepository.findByProviderAndProviderUserId(SocialProvider.GOOGLE, "google-sub-123"))
                .willReturn(Optional.empty());
        given(userRepository.findByEmail("woody@example.com")).willReturn(Optional.of(existingLocalUser));

        OAuth2User result = customOAuth2UserService.loadUser(googleUserRequest());

        CustomOAuth2User principal = (CustomOAuth2User) result;
        assertThat(principal.getUser()).isEqualTo(existingLocalUser);
        verify(userRepository, never()).save(any()); // 새로 만들지 않고 기존 계정에 연결
        ArgumentCaptor<SocialAccount> captor = ArgumentCaptor.forClass(SocialAccount.class);
        verify(socialAccountRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(existingLocalUser);
        assertThat(captor.getValue().getProviderUserId()).isEqualTo("google-sub-123");
    }

    @Test
    void 처음_보는_계정이면_암호화된_임의_비밀번호로_새_User와_SocialAccount를_만든다() {
        given(socialAccountRepository.findByProviderAndProviderUserId(SocialProvider.GOOGLE, "google-sub-123"))
                .willReturn(Optional.empty());
        given(userRepository.findByEmail("woody@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode(any())).willReturn("ENCODED_RANDOM_PW");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        OAuth2User result = customOAuth2UserService.loadUser(googleUserRequest());

        CustomOAuth2User principal = (CustomOAuth2User) result;
        assertThat(principal.getUser().getEmail()).isEqualTo("woody@example.com");
        assertThat(principal.getUser().getName()).isEqualTo("우디");
        assertThat(principal.getUser().getPassword()).isEqualTo("ENCODED_RANDOM_PW"); // 평문이 아니라 암호화된 값이 저장되는지
        verify(socialAccountRepository).save(any(SocialAccount.class));
    }
}
