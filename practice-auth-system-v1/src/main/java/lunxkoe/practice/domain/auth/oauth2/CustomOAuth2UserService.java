package lunxkoe.practice.domain.auth.oauth2;

import lombok.RequiredArgsConstructor;
import lunxkoe.practice.domain.auth.entity.SocialAccount;
import lunxkoe.practice.domain.auth.repository.SocialAccountRepository;
import lunxkoe.practice.domain.user.entity.User;
import lunxkoe.practice.domain.user.repository.UserRepository;
import lunxkoe.practice.global.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // "google" / "kako"

        OAuthAttributes attributes = OAuthAttributes.of(registrationId, oAuth2User.getAttributes());

        User user = socialAccountRepository.findByProviderAndProviderUserId(attributes.provider(), attributes.providerUserId())
                .map(SocialAccount::getUser)
                .orElseGet(() -> provisionUser(attributes));

        if (user.isLocked()) {
            throw new OAuth2AuthenticationException(new OAuth2Error(ErrorCode.ACCOUNT_LOCKED.name(), ErrorCode.ACCOUNT_LOCKED.getMessage(), null));
        }

        return new CustomOAuth2User(user, oAuth2User.getAttributes());
    }

    private User provisionUser(OAuthAttributes attributes) {
        User user = userRepository.findByEmail(attributes.email())
                .orElseGet(() -> userRepository.save(
                        User.createSocialUser(
                                attributes.name(),
                                attributes.email(),
                                passwordEncoder.encode(UUID.randomUUID().toString())
                        )
                ));

        socialAccountRepository.save(SocialAccount.of(user, attributes.provider(), attributes.providerUserId()));
        return user;
    }
}
