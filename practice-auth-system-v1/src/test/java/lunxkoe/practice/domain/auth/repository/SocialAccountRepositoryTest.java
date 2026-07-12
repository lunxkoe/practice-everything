package lunxkoe.practice.domain.auth.repository;

import jakarta.persistence.EntityManager;
import lunxkoe.practice.domain.auth.entity.SocialAccount;
import lunxkoe.practice.domain.auth.entity.SocialProvider;
import lunxkoe.practice.domain.user.entity.User;
import lunxkoe.practice.domain.user.repository.UserRepository;
import lunxkoe.practice.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * findByProviderAndProviderUserId가 join fetch로 User를 즉시 로딩하는지 검증한다.
 * 클래스 레벨에서 트랜잭션을 강제로 비활성화(NOT_SUPPORTED)해서, 예전에 실제로 버그가 났던 상황
 * (레포지토리 호출마다 별도 세션이 열렸다 닫히는 상황)을 그대로 재현한다.
 * (@DataJpaTest 기본값인 "테스트 전체를 트랜잭션 하나로 묶고 롤백"을 쓰면, 세션이 안 끊겨서
 * join fetch가 없어도 우연히 통과해버려 이 버그를 못 잡는다.)
 */
// application.yaml에 spring.jpa.database-platform이 MySQL8Dialect로 고정되어 있어서,
// @DataJpaTest가 DataSource는 H2로 바꿔줘도 방언은 그대로 MySQL을 써버려 DDL이 깨진다. 테스트에서만 H2 방언으로 덮어씀.
@DataJpaTest(properties = "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect")
@Import(JpaAuditingConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SocialAccountRepositoryTest {

    @Autowired
    UserRepository userRepository;
    @Autowired
    SocialAccountRepository socialAccountRepository;
    @Autowired
    EntityManager entityManager;

    @AfterEach
    void cleanUp() {
        socialAccountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void join_fetch_덕분에_세션이_끊긴_뒤에도_User_필드에_접근할_수_있다() {
        User user = userRepository.save(User.createLocalUser("우디", "woody@example.com", "ENCODED_PW"));
        socialAccountRepository.save(SocialAccount.of(user, SocialProvider.GOOGLE, "google-sub-123"));

        Optional<SocialAccount> found = socialAccountRepository.findByProviderAndProviderUserId(SocialProvider.GOOGLE, "google-sub-123");

        assertThat(found).isPresent();
        // 예전에 CustomOAuth2UserService.loadUser()에서 LazyInitializationException이 나던 바로 그 지점
        assertThatCode(() -> found.get().getUser().isLocked()).doesNotThrowAnyException();
        assertThat(found.get().getUser().getEmail()).isEqualTo("woody@example.com");
    }

    @Test
    void 존재하지_않는_provider_providerUserId_조합이면_빈_Optional을_반환한다() {
        Optional<SocialAccount> found = socialAccountRepository.findByProviderAndProviderUserId(SocialProvider.GOOGLE, "no-such-id");

        assertThat(found).isEmpty();
    }
}
