package lunxkoe.practice.global.security;

import lombok.RequiredArgsConstructor;
import lunxkoe.practice.global.jwt.JwtProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SessionRegistry {

    private static final String KEY_PREFIX = "session:";
    private static final String FIELD_SESSION_ID = "sessionId";
    private static final String FIELD_REFRESH_JTI = "refreshJti";
    private static final String FIELD_USER_AGENT = "userAgent";
    private static final String FIELD_ISSUED_AT = "issuedAt";

    private final StringRedisTemplate redisTemplate;
    private final JwtProvider jwtProvider;

    public UserSession issue(UUID userId, String userAgent) {
        return save(userId, UUID.randomUUID(), UUID.randomUUID(), userAgent);
    }

    public UserSession rotate(UUID userId, UUID sessionId, String userAgent) {
        return save(userId, sessionId, UUID.randomUUID(), userAgent);
    }

    public Optional<UserSession> find(UUID userId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key(userId));
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new UserSession(
                UUID.fromString((String) entries.get(FIELD_SESSION_ID)),
                UUID.fromString((String) entries.get(FIELD_REFRESH_JTI)),
                (String) entries.get(FIELD_USER_AGENT),
                Instant.parse((String) entries.get(FIELD_ISSUED_AT))
        ));
    }

    public void revoke(UUID userId) {
        redisTemplate.delete(key(userId));
    }

    private UserSession save(UUID userId, UUID sessionId, UUID refreshJti, String userAgent) {
        String redisKey = key(userId);
        Map<String, String> fields = Map.of(
                FIELD_SESSION_ID, sessionId.toString(),
                FIELD_REFRESH_JTI, refreshJti.toString(),
                FIELD_USER_AGENT, StringUtils.hasText(userAgent) ? userAgent : "unknown",
                FIELD_ISSUED_AT, Instant.now().toString()
        );

        redisTemplate.opsForHash().putAll(redisKey, fields);
        redisTemplate.expire(redisKey, Duration.ofSeconds(jwtProvider.getRefreshTokenExpirationSeconds()));

        return new UserSession(sessionId, refreshJti, userAgent, Instant.now());
        // TODO: 시간을 엄격하게 맞춰야하지 않나?
    }

    private String key(UUID userId) {
        return KEY_PREFIX + userId;
    }
}
