package lunxkoe.practice.global.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lunxkoe.practice.global.exception.controller.CustomException;
import lunxkoe.practice.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtProvider {

    private static final String ROLE_CLAIM = "role";
    private static final String USER_ID = "userId";

    private final SecretKey accessSecretKey;
    private final SecretKey refreshSecretKey;
    private final long accessTokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;

    public JwtProvider(
            @Value("${jwt.access-secret}") String accessSecret,
            @Value("${jwt.refresh-secret}") String refreshSecret,
            @Value("${jwt.access-token-validity-in-seconds}") long accessTokenValidityInSeconds,
            @Value("${jwt.refresh-token-validity-in-seconds}") long refreshTokenValidityInSeconds
    ) {

        byte[] accessKeyBytes = Decoders.BASE64.decode(accessSecret);
        this.accessSecretKey = Keys.hmacShaKeyFor(accessKeyBytes);
        byte[] refreshKeyBytes = Decoders.BASE64.decode(refreshSecret);
        this.refreshSecretKey = Keys.hmacShaKeyFor(refreshKeyBytes);
        this.accessTokenValidityInMilliseconds = accessTokenValidityInSeconds * 1000;
        this.refreshTokenValidityInMilliseconds = refreshTokenValidityInSeconds * 1000;
    }

    public String createAccessToken(String email, String role, UUID userId) {

        long now = (new Date()).getTime();
        Date validity = new Date(now + this.accessTokenValidityInMilliseconds);

        return Jwts.builder()
                .subject(email)
                .claim(USER_ID, userId)
                .claim(ROLE_CLAIM, role)
                .issuedAt(new Date(now))
                .expiration(validity)
                .signWith(this.accessSecretKey)
                .compact();
    }

    public String createRefreshToken(String email, UUID userId) {

        long now = (new Date()).getTime();
        Date validity = new Date(now + this.refreshTokenValidityInMilliseconds);

        return Jwts.builder()
                .subject(email)
                .claim(USER_ID, userId)
                .issuedAt(new Date(now))
                .expiration(validity)
                .signWith(this.refreshSecretKey)
                .compact();
    }

    public JwtStatus validateToken(String token, boolean isAccessToken) {

        try {
            parseClaims(token, isAccessToken);
            return JwtStatus.VALID;
        } catch (SignatureException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            return JwtStatus.INVALID;
        } catch (ExpiredJwtException e) {
            return JwtStatus.EXPIRED;
        }
    }

    public Date getExpiration(String token, boolean isAccessToken) {

        try {
            return parseClaims(token, isAccessToken).getExpiration();
        } catch (ExpiredJwtException e) {
            // 이미 만료된 토큰의 경우 예외 객체에서 만료 시간을 꺼내야함
            return e.getClaims().getExpiration();
        } catch (SignatureException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_USER);
        }
    }

    public Claims getClaims(String token, boolean isAccessToken) {
        return parseClaims(token, isAccessToken);
    }

    private Claims parseClaims(String token, boolean isAccessToken) {

        SecretKey key = isAccessToken ? accessSecretKey : refreshSecretKey;
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
