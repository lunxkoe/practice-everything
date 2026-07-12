package lunxkoe.practice.domain.auth.repository;

import lunxkoe.practice.domain.auth.entity.SocialAccount;
import lunxkoe.practice.domain.auth.entity.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    // user는 지연 로딩이라 세션 밖(예: OAuth2 로그인 콜백)에서 접근하면 LazyInitializationException이 남 -> join fetch로 즉시 로딩
    @Query("select sa from SocialAccount sa join fetch sa.user where sa.provider = :provider and sa.providerUserId = :providerUserId")
    Optional<SocialAccount> findByProviderAndProviderUserId(@Param("provider") SocialProvider provider, @Param("providerUserId") String providerUserId);
}
