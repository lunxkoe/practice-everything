package lunxkoe.practice.global.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TokenRevocationService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void revokeToken(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}