package lunxkoe.practice.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lunxkoe.practice.domain.auth.entity.TemporaryPassword;
import lunxkoe.practice.domain.auth.password.TemporaryPasswordGenerator;
import lunxkoe.practice.domain.auth.repository.TemporaryPasswordRepository;
import lunxkoe.practice.domain.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final long TTL_SECONDS = 180; // 3분

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordRepository temporaryPasswordRepository;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;

    @Transactional(readOnly = true)
    public Optional<String> requestReset(String email) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    String rawTemporaryPassword = temporaryPasswordGenerator.generate();
                    temporaryPasswordRepository.save(
                            new TemporaryPassword(user.getEmail(), passwordEncoder.encode(rawTemporaryPassword), TTL_SECONDS));
                    return rawTemporaryPassword;
                });
    }
}
