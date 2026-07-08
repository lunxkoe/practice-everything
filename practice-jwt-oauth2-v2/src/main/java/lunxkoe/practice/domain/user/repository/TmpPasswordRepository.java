package lunxkoe.practice.domain.user.repository;

import lunxkoe.practice.domain.user.entity.TmpPassword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TmpPasswordRepository extends JpaRepository<TmpPassword, UUID> {
    Optional<TmpPassword> findByEmail(String email);
}
