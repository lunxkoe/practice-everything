package lunxkoe.practice.domain.auth.oauth2;

import lunxkoe.practice.domain.auth.entity.SocialProvider;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthAttributesTest {

    @Test
    void 구글_속성을_sub_email_name_기준으로_매핑한다() {
        Map<String, Object> attributes = Map.of(
                "sub", "google-sub-123",
                "email", "woody@example.com",
                "name", "우디"
        );

        OAuthAttributes result = OAuthAttributes.ofGoogle(attributes);

        assertThat(result.provider()).isEqualTo(SocialProvider.GOOGLE);
        assertThat(result.providerUserId()).isEqualTo("google-sub-123");
        assertThat(result.email()).isEqualTo("woody@example.com");
        assertThat(result.name()).isEqualTo("우디");
    }

    @Test
    void registrationId가_google이면_ofGoogle로_라우팅된다() {
        Map<String, Object> attributes = Map.of("sub", "google-sub-123", "email", "woody@example.com", "name", "우디");

        OAuthAttributes result = OAuthAttributes.of("google", attributes);

        assertThat(result.provider()).isEqualTo(SocialProvider.GOOGLE);
    }

    @Test
    void registrationId가_kakao면_아직_미구현이라_null을_반환한다() {
        // TODO: 카카오 구현되면 이 테스트도 같이 고쳐야 함 (지금은 현재 상태를 고정해두는 용도)
        OAuthAttributes result = OAuthAttributes.of("kakao", Map.of());

        assertThat(result).isNull();
    }
}
