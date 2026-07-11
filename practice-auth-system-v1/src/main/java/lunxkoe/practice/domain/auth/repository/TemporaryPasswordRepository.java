package lunxkoe.practice.domain.auth.repository;

import lunxkoe.practice.domain.auth.entity.TemporaryPassword;
import org.springframework.data.repository.CrudRepository;

public interface TemporaryPasswordRepository extends CrudRepository<TemporaryPassword, String> {
}
