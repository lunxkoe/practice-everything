package lunxkoe.practice.domain.user.repository;

import lunxkoe.practice.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    // select user_id from users where email = :email limit 1
    boolean existsByEmail(String email);
}
