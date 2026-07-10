package lunxkoe.practice.global.security;

import java.time.Instant;
import java.util.UUID;

public record UserSession(
        UUID sessionId,
        UUID currentRefreshJti,
        String userAgent,
        Instant issuedAt
) {
}
