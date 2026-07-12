package lunxkoe.practice.domain.auth.oauth2;

import lunxkoe.practice.domain.auth.entity.SocialProvider;

import java.util.Map;

public record OAuthAttributes(
        SocialProvider provider,
        String providerUserId,
        String email,
        String name
) {

    public static OAuthAttributes of(String registrationId, Map<String, Object> attributes) {
        if (registrationId.equals("kakao")) {
            return ofKakao(attributes);
        } else {
            return ofGoogle(attributes);
        }
    }

    public static OAuthAttributes ofGoogle(Map<String, Object> attributes) {
        return new OAuthAttributes(
                SocialProvider.GOOGLE,
                (String) attributes.get("sub"),
                (String) attributes.get("email"),
                (String) attributes.get("name")
        );
    }

    // TODO
    public static OAuthAttributes ofKakao(Map<String, Object> attributes) {
        return null;
    }
}
