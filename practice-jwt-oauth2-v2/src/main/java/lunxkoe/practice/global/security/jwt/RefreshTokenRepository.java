package lunxkoe.practice.global.security.jwt;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByUserId(UUID userId);

    Optional<RefreshToken> findByTokenAndUserId(String token, UUID userId);

    void deleteByUserId(UUID userId);
}
