package lunxkoe.practice.global.jwt;

import io.jsonwebtoken.Claims;
import lunxkoe.practice.global.common.enums.UserRole;
import lunxkoe.practice.global.exception.CustomException;
import lunxkoe.practice.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    private static final String TEST_SECRET =
            Base64.getEncoder().encodeToString("test-secret-key-for-jwt-unit-tests-only-do-not-use-in-prod".getBytes());

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(TEST_SECRET, 30, 7);
        jwtProvider = new JwtProvider(properties);
        jwtProvider.init();
    }

    @Test
    void access_token을_발급하면_subject_role_sid_typ_클레임이_그대로_들어간다() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();

        String token = jwtProvider.createAccessToken(userId, UserRole.ADMIN, sessionId, now);
        Claims claims = jwtProvider.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.get("sid", String.class)).isEqualTo(sessionId.toString());
        assertThat(claims.get("typ", String.class)).isEqualTo("access");
    }

    @Test
    void refresh_token을_발급하면_subject_sid_jti_typ_클레임이_그대로_들어간다() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID jti = UUID.randomUUID();
        Instant now = Instant.now();

        String token = jwtProvider.createRefreshToken(userId, sessionId, jti, now);
        Claims claims = jwtProvider.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("sid", String.class)).isEqualTo(sessionId.toString());
        assertThat(claims.getId()).isEqualTo(jti.toString());
        assertThat(claims.get("typ", String.class)).isEqualTo("refresh");
    }

    @Test
    void 만료된_토큰을_파싱하면_INVALID_TOKEN_expired로_예외를_던진다() {
        UUID userId = UUID.randomUUID();
        // 발급 시각을 1일 전으로 잡으면 accessTokenExpirationMinutes(30분)이 지나도 이미 과거라 즉시 만료된 토큰이 된다.
        String expiredToken = jwtProvider.createAccessToken(
                userId, UserRole.USER, UUID.randomUUID(), Instant.now().minus(1, ChronoUnit.DAYS));

        assertThatThrownBy(() -> jwtProvider.parseClaims(expiredToken))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN);
                    assertThat(ce.getDetails()).containsEntry("reason", "expired");
                });
    }

    @Test
    void 다른_키로_서명된_토큰을_파싱하면_INVALID_TOKEN_malformed로_예외를_던진다() {
        JwtProperties otherProperties = new JwtProperties(
                Base64.getEncoder().encodeToString("completely-different-secret-key-value-for-this-test-case".getBytes()),
                30, 7);
        JwtProvider otherProvider = new JwtProvider(otherProperties);
        otherProvider.init();
        String tokenSignedByOtherKey = otherProvider.createAccessToken(
                UUID.randomUUID(), UserRole.USER, UUID.randomUUID(), Instant.now());

        assertThatThrownBy(() -> jwtProvider.parseClaims(tokenSignedByOtherKey))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN);
                    assertThat(ce.getDetails()).containsEntry("reason", "malformed");
                });
    }

    @Test
    void 형식이_아예_깨진_문자열을_파싱하면_INVALID_TOKEN_malformed로_예외를_던진다() {
        assertThatThrownBy(() -> jwtProvider.parseClaims("this-is-not-a-jwt"))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN);
                    assertThat(ce.getDetails()).containsEntry("reason", "malformed");
                });
    }

    @Test
    void getRefreshTokenExpirationSeconds는_설정된_일수를_초로_환산한다() {
        assertThat(jwtProvider.getRefreshTokenExpirationSeconds()).isEqualTo(7 * 24 * 60 * 60L);
    }
}
