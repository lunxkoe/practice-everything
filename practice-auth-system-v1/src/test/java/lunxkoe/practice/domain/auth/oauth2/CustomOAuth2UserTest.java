package lunxkoe.practice.domain.auth.oauth2;

import lunxkoe.practice.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CustomOAuth2UserTest {

    private User sampleUser() {
        return User.createLocalUser("우디", "woody@example.com", "ENCODED_PW");
    }

    @Test
    void getName은_유저의_외부용_UUID를_반환한다() {
        User user = sampleUser();
        CustomOAuth2User customOAuth2User = new CustomOAuth2User(user, Map.of("sub", "google-sub-123"));

        assertThat(customOAuth2User.getName()).isEqualTo(user.getId().toString());
    }

    @Test
    void getAuthorities는_유저의_role을_권한으로_반환한다() {
        User user = sampleUser();
        CustomOAuth2User customOAuth2User = new CustomOAuth2User(user, Map.of());

        assertThat(customOAuth2User.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("USER");
    }

    @Test
    void getAttributes는_생성자로_받은_원본_맵을_그대로_반환한다() {
        User user = sampleUser();
        Map<String, Object> attributes = Map.of("sub", "google-sub-123", "email", "woody@example.com");
        CustomOAuth2User customOAuth2User = new CustomOAuth2User(user, attributes);

        assertThat(customOAuth2User.getAttributes()).isEqualTo(attributes);
    }
}
