package lunxkoe.practice.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lunxkoe.practice.global.common.enums.UserRole;
import lunxkoe.practice.global.exception.CustomException;
import lunxkoe.practice.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties jwtProperties;
    private SecretKey key;

    @PostConstruct
    void init() {
        byte[] bytes = Decoders.BASE64.decode(jwtProperties.secret());
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String createAccessToken(UUID userId, UserRole role, UUID sessionId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role.name())
                .claim("sid", sessionId.toString())
                .claim("typ", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(jwtProperties.accessTokenExpirationMinutes(), ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(UUID userId, UUID sessionId, UUID jti) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .id(jti.toString())
                .claim("sid", sessionId.toString())
                .claim("typ", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(jwtProperties.accessTokenExpirationMinutes(), ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, Map.of("reason", "expired"));
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, Map.of("reason", "malformed"));
        }
    }

    public long getRefreshTokenExpirationSeconds() {
        return Duration.ofDays(jwtProperties.refreshTokenExpirationDays()).toSeconds();
    }
}
