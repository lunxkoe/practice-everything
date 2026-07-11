package lunxkoe.practice.domain.auth.service;

import io.jsonwebtoken.Claims;
import lunxkoe.practice.domain.auth.dto.request.SignInRequest;
import lunxkoe.practice.domain.user.entity.User;
import lunxkoe.practice.domain.user.repository.UserRepository;
import lunxkoe.practice.global.exception.CustomException;
import lunxkoe.practice.global.exception.ErrorCode;
import lunxkoe.practice.global.jwt.JwtProvider;
import lunxkoe.practice.global.security.SessionRegistry;
import lunxkoe.practice.global.security.UserSession;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    JwtProvider jwtProvider;
    @Mock
    SessionRegistry sessionRegistry;
    @Mock
    LoginFailureService loginFailureService;

    @InjectMocks
    AuthService authService;

    private User sampleUser() {
        return User.createLocalUser("우디", "woody@example.com", "ENCODED_PW");
    }

    private SignInRequest request(String email, String rawPassword) {
        return new SignInRequest(email, rawPassword);
    }

    // =========================================================================================
    // signIn()
    // =========================================================================================
    @Nested
    class SignIn {

        @Test
        void 정상적인_이메일과_비밀번호면_로그인에_성공하고_토큰을_발급한다() {
            User user = sampleUser();
            given(userRepository.findByEmail("woody@example.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("raw-pw", "ENCODED_PW")).willReturn(true);

            UserSession session = new UserSession(UUID.randomUUID(), UUID.randomUUID(), "chrome", Instant.now());
            given(sessionRegistry.issue(user.getId(), "chrome")).willReturn(session);
            given(jwtProvider.createAccessToken(user.getId(), user.getRole(), session.sessionId(), session.issuedAt()))
                    .willReturn("access-token");
            given(jwtProvider.createRefreshToken(user.getId(), session.sessionId(), session.currentRefreshJti(), session.issuedAt()))
                    .willReturn("refresh-token");

            AuthService.SignInResult result = authService.signIn(request("woody@example.com", "raw-pw"), "chrome");

            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token");
            assertThat(result.userDto().email()).isEqualTo("woody@example.com");
            verify(loginFailureService).clearLoginFailure(user.getId());
            verify(loginFailureService, never()).registerLoginFailure(any(User.class));
        }

        @Test
        void 존재하지_않는_이메일이면_INVALID_CREDENTIALS를_던지고_비밀번호는_검사하지_않는다() {
            given(userRepository.findByEmail("ghost@example.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.signIn(request("ghost@example.com", "아무거나"), "chrome"))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

            verifyNoInteractions(passwordEncoder); // 계정 열거 방지: 없는 계정은 비밀번호 비교 자체를 안 함
            verifyNoInteractions(loginFailureService); // 실패 카운트를 걸 유저 자체가 없음
        }

        @Test
        void 비밀번호가_틀리면_INVALID_CREDENTIALS를_던지고_실패_카운트_등록을_위임한다() {
            User user = sampleUser();
            given(userRepository.findByEmail("woody@example.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrong-pw", "ENCODED_PW")).willReturn(false);

            assertThatThrownBy(() -> authService.signIn(request("woody@example.com", "wrong-pw"), "chrome"))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

            verify(loginFailureService).registerLoginFailure(user);
            verify(sessionRegistry, never()).issue(any(), any());
        }

        @Test
        void 존재하지_않는_계정과_비밀번호_오류가_동일한_에러코드를_반환한다_계정_열거_공격_방지() {
            given(userRepository.findByEmail("ghost@example.com")).willReturn(Optional.empty());

            User user = sampleUser();
            given(userRepository.findByEmail("woody@example.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrong-pw", "ENCODED_PW")).willReturn(false);

            assertThatThrownBy(() -> authService.signIn(request("ghost@example.com", "아무거나"), "chrome"))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

            assertThatThrownBy(() -> authService.signIn(request("woody@example.com", "wrong-pw"), "chrome"))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }

        @Test
        void 잠긴_계정이면_비밀번호_검사_없이_ACCOUNT_LOCKED를_던진다() {
            User user = sampleUser();
            user.lock();
            given(userRepository.findByEmail("woody@example.com")).willReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.signIn(request("woody@example.com", "아무거나"), "chrome"))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ACCOUNT_LOCKED);

            verifyNoInteractions(passwordEncoder); // 잠긴 계정은 비밀번호가 맞든 틀리든 같은 결과여야 함(타이밍 정보 유출 방지)
            verifyNoInteractions(loginFailureService);
        }

        @Test
        void 로그인_실패_시마다_LoginFailureService에게_실패_등록을_위임한다() {
            User user = sampleUser();
            given(userRepository.findByEmail("woody@example.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrong-pw", "ENCODED_PW")).willReturn(false);

            for (int i = 0; i < 5; i++) {
                assertThatThrownBy(() -> authService.signIn(request("woody@example.com", "wrong-pw"), "chrome"))
                        .isInstanceOf(CustomException.class);
            }

            // 실제 5회 카운트→잠금 로직 자체는 LoginFailureServiceTest에서 검증. 여기서는 위임 횟수만 확인.
            verify(loginFailureService, times(5)).registerLoginFailure(user);
        }

        // =====================================================================================
        // 아래는 임시 비밀번호(TemporaryPassword) 로그인이 다시 활성화된 이후를 위한 테스트입니다.
        // AuthService에 `private final TemporaryPasswordRepository temporaryPasswordRepository;` 필드와
        // matchesTemporaryPassword(...) 주석을 해제하고,
        // signIn()의 authenticated 계산식에 `|| matchesTemporaryPassword(foundUser.getEmail(), request.password())`를
        // 다시 붙였을 때 사용하세요. 현재 상태로는 컴파일되지 않으므로 전부 주석 처리합니다.
        // =====================================================================================
        /*
        @Mock
        TemporaryPasswordRepository temporaryPasswordRepository;

        @Test
        void 정식_비밀번호는_틀렸지만_임시_비밀번호가_일치하면_로그인에_성공한다() {
            User user = sampleUser();
            given(userRepository.findByEmail("woody@example.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("temp-pw", "ENCODED_PW")).willReturn(false);

            TemporaryPassword tempPassword = new TemporaryPassword("woody@example.com", "ENCODED_TEMP_PW");
            given(temporaryPasswordRepository.findById("woody@example.com")).willReturn(Optional.of(tempPassword));
            given(passwordEncoder.matches("temp-pw", "ENCODED_TEMP_PW")).willReturn(true);

            UserSession session = new UserSession(UUID.randomUUID(), UUID.randomUUID(), "chrome", Instant.now());
            given(sessionRegistry.issue(user.getId(), "chrome")).willReturn(session);
            given(jwtProvider.createAccessToken(any(), any(), any(), any())).willReturn("access-token");
            given(jwtProvider.createRefreshToken(any(), any(), any(), any())).willReturn("refresh-token");

            AuthService.SignInResult result = authService.signIn(request("woody@example.com", "temp-pw"), "chrome");

            assertThat(result.accessToken()).isEqualTo("access-token");
            verify(loginFailureService).clearLoginFailure(user.getId());
        }

        @Test
        void 정식_비밀번호가_맞으면_단락평가로_임시_비밀번호_조회는_하지_않는다() {
            User user = sampleUser();
            given(userRepository.findByEmail("woody@example.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("raw-pw", "ENCODED_PW")).willReturn(true);

            UserSession session = new UserSession(UUID.randomUUID(), UUID.randomUUID(), "chrome", Instant.now());
            given(sessionRegistry.issue(user.getId(), "chrome")).willReturn(session);
            given(jwtProvider.createAccessToken(any(), any(), any(), any())).willReturn("access-token");
            given(jwtProvider.createRefreshToken(any(), any(), any(), any())).willReturn("refresh-token");

            authService.signIn(request("woody@example.com", "raw-pw"), "chrome");

            verifyNoInteractions(temporaryPasswordRepository); // || 앞쪽이 true라 뒤쪽은 평가되지 않아야 함
        }

        @Test
        void 정식_비밀번호도_임시_비밀번호도_모두_틀리면_INVALID_CREDENTIALS이고_실패카운트가_오른다() {
            User user = sampleUser();
            given(userRepository.findByEmail("woody@example.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrong-pw", "ENCODED_PW")).willReturn(false);
            given(temporaryPasswordRepository.findById("woody@example.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.signIn(request("woody@example.com", "wrong-pw"), "chrome"))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

            verify(loginFailureService).registerLoginFailure(user);
        }
        */
    }

    // =========================================================================================
    // refresh()
    // =========================================================================================
    @Nested
    class Refresh {

        @Test
        void refresh_토큰_쿠키가_없으면_TOKEN_NEEDED를_던진다() {
            assertThatThrownBy(() -> authService.refresh(null, "chrome"))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.TOKEN_NEEDED);

            verifyNoInteractions(jwtProvider);
        }

        @Test
        void 토큰_타입이_refresh가_아니면_INVALID_TOKEN을_던진다() {
            Claims claims = fakeClaims(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "access");
            given(jwtProvider.parseClaims("access-token-misused")).willReturn(claims);

            assertThatThrownBy(() -> authService.refresh("access-token-misused", "chrome"))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_TOKEN);

            verifyNoInteractions(sessionRegistry);
        }

        @Test
        void 세션이_존재하지_않으면_SESSION_EXPIRED를_던진다() {
            UUID userId = UUID.randomUUID();
            Claims claims = fakeClaims(userId, UUID.randomUUID(), UUID.randomUUID(), "refresh");
            given(jwtProvider.parseClaims("token")).willReturn(claims);
            given(sessionRegistry.find(userId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh("token", "chrome"))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.SESSION_EXPIRED);
        }

        @Test
        void sid가_현재_세션과_다르면_SESSION_EXPIRED이며_새로_로그인한_세션은_폐기하지_않는다() {
            UUID userId = UUID.randomUUID();
            UUID tokenSid = UUID.randomUUID();
            UUID currentSid = UUID.randomUUID(); // 다른 기기에서 재로그인해 세션이 이미 교체된 상태
            Claims claims = fakeClaims(userId, tokenSid, UUID.randomUUID(), "refresh");
            given(jwtProvider.parseClaims("old-device-token")).willReturn(claims);
            given(sessionRegistry.find(userId))
                    .willReturn(Optional.of(new UserSession(currentSid, UUID.randomUUID(), "chrome", Instant.now())));

            assertThatThrownBy(() -> authService.refresh("old-device-token", "old-chrome"))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.SESSION_EXPIRED);

            verify(sessionRegistry, never()).revoke(any());
        }

        @Test
        void jti가_다르면_재사용_공격으로_간주해_세션_전체를_폐기한다() {
            UUID userId = UUID.randomUUID();
            UUID sid = UUID.randomUUID();
            UUID oldJti = UUID.randomUUID();
            UUID currentJti = UUID.randomUUID();
            Claims claims = fakeClaims(userId, sid, oldJti, "refresh");
            given(jwtProvider.parseClaims("stolen-old-token")).willReturn(claims);
            given(sessionRegistry.find(userId))
                    .willReturn(Optional.of(new UserSession(sid, currentJti, "chrome", Instant.now())));

            assertThatThrownBy(() -> authService.refresh("stolen-old-token", "attacker-device"))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.REFRESH_TOKEN_REUSED);

            verify(sessionRegistry).revoke(userId);
            verify(sessionRegistry, never()).rotate(any(), any(), any());
        }

        @Test
        void 세션은_유효하지만_유저가_DB에_없으면_USER_NOT_FOUND를_던진다() {
            UUID userId = UUID.randomUUID();
            UUID sid = UUID.randomUUID();
            UUID jti = UUID.randomUUID();
            Claims claims = fakeClaims(userId, sid, jti, "refresh");
            given(jwtProvider.parseClaims("token")).willReturn(claims);
            given(sessionRegistry.find(userId)).willReturn(Optional.of(new UserSession(sid, jti, "chrome", Instant.now())));
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh("token", "chrome"))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        void 모든_검증을_통과하면_세션을_회전하고_새_토큰_쌍을_발급한다() {
            UUID userId = UUID.randomUUID();
            UUID sid = UUID.randomUUID();
            UUID jti = UUID.randomUUID();
            User user = sampleUser();
            Claims claims = fakeClaims(userId, sid, jti, "refresh");
            given(jwtProvider.parseClaims("valid-token")).willReturn(claims);
            given(sessionRegistry.find(userId)).willReturn(Optional.of(new UserSession(sid, jti, "chrome", Instant.now())));
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            UUID newJti = UUID.randomUUID();
            Instant rotatedAt = Instant.now();
            UserSession rotated = new UserSession(sid, newJti, "chrome", rotatedAt);
            given(sessionRegistry.rotate(userId, sid, "chrome")).willReturn(rotated);
            given(jwtProvider.createAccessToken(user.getId(), user.getRole(), sid, rotatedAt)).willReturn("new-access");
            given(jwtProvider.createRefreshToken(user.getId(), sid, newJti, rotatedAt)).willReturn("new-refresh");

            AuthService.SignInResult result = authService.refresh("valid-token", "chrome");

            assertThat(result.accessToken()).isEqualTo("new-access");
            assertThat(result.refreshToken()).isEqualTo("new-refresh");
            verify(sessionRegistry).rotate(userId, sid, "chrome");
        }

        @Test
        void 서명이_위조되었거나_만료된_토큰이면_JwtProvider의_예외가_그대로_전파된다() {
            given(jwtProvider.parseClaims("tampered"))
                    .willThrow(new CustomException(ErrorCode.INVALID_TOKEN, Map.of("reason", "malformed")));

            assertThatThrownBy(() -> authService.refresh("tampered", "chrome"))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_TOKEN);

            verifyNoInteractions(sessionRegistry);
            verifyNoInteractions(userRepository);
        }

        private Claims fakeClaims(UUID userId, UUID sid, UUID jti, String typ) {
            Claims claims = mock(Claims.class);
            lenient().when(claims.getSubject()).thenReturn(userId.toString());
            lenient().when(claims.get("sid", String.class)).thenReturn(sid.toString());
            lenient().when(claims.getId()).thenReturn(jti.toString());
            lenient().when(claims.get("typ", String.class)).thenReturn(typ);
            return claims;
        }
    }

    // =========================================================================================
    // signOut()
    // =========================================================================================
    @Nested
    class SignOut {

        @Test
        void 인증된_사용자의_세션을_폐기한다() {
            UUID userId = UUID.randomUUID();

            authService.signOut(userId);

            verify(sessionRegistry).revoke(userId);
            verifyNoInteractions(userRepository, passwordEncoder, jwtProvider);
        }

        @Test
        void 이미_세션이_없는_상태에서_다시_호출해도_예외없이_멱등하게_동작한다() {
            UUID userId = UUID.randomUUID();
            // sessionRegistry.revoke는 Redis DEL이라 키가 없어도 안전한 no-op이어야 함

            assertThatCode(() -> authService.signOut(userId)).doesNotThrowAnyException();
            verify(sessionRegistry).revoke(userId);
        }
    }
}
