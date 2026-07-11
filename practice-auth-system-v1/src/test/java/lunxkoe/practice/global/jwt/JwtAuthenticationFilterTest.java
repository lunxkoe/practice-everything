package lunxkoe.practice.global.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import lunxkoe.practice.global.common.enums.UserRole;
import lunxkoe.practice.global.exception.CustomException;
import lunxkoe.practice.global.exception.ErrorCode;
import lunxkoe.practice.global.security.SessionRegistry;
import lunxkoe.practice.global.security.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    JwtProvider jwtProvider;
    @Mock
    SessionRegistry sessionRegistry;

    JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtProvider, sessionRegistry, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void Authorization_헤더가_없으면_인증없이_체인만_진행한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtProvider);
    }

    @Test
    void 유효한_access_token이면_SecurityContext에_인증정보를_설정하고_체인을_진행한다() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sid = UUID.randomUUID();

        Claims mockClaims = fakeClaims(userId, sid, "access", UserRole.USER);
        given(jwtProvider.parseClaims("valid-token")).willReturn(mockClaims);

        given(sessionRegistry.find(userId))
                .willReturn(Optional.of(new UserSession(sid, UUID.randomUUID(), "chrome", Instant.now())));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.userId()).isEqualTo(userId);
        assertThat(principal.role()).isEqualTo(UserRole.USER);
    }

    @Test
    void 만료되거나_위조된_토큰이어도_필터가_요청을_막지_않는다_permitAll_회귀테스트() throws Exception {
        given(jwtProvider.parseClaims("garbage"))
                .willThrow(new CustomException(ErrorCode.INVALID_TOKEN, Map.of("reason", "malformed")));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer garbage");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void refresh_토큰을_Authorization_헤더에_넣으면_typ_검증에서_막혀_인증되지_않는다() throws Exception {
        UUID userId = UUID.randomUUID();

        Claims mockClaims = fakeClaims(userId, UUID.randomUUID(), "refresh", UserRole.USER);
        given(jwtProvider.parseClaims("refresh-as-access")).willReturn(mockClaims);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer refresh-as-access");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(sessionRegistry);
    }

    @Test
    void 세션이_Redis에_없으면_로그아웃된_사용자로_취급해_인증하지_않는다() throws Exception {
        UUID userId = UUID.randomUUID();

        Claims mockClaims = fakeClaims(userId, UUID.randomUUID(), "access", UserRole.USER);
        given(jwtProvider.parseClaims("no-session-token")).willReturn(mockClaims);

        given(sessionRegistry.find(userId)).willReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer no-session-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void sid가_현재_세션과_다르면_다른_기기_재로그인으로_간주해_인증하지_않는다() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tokenSid = UUID.randomUUID();
        UUID currentSid = UUID.randomUUID();

        Claims mockClaims = fakeClaims(userId, tokenSid, "access", UserRole.USER);
        given(jwtProvider.parseClaims("stale-sid-token")).willReturn(mockClaims);

        given(sessionRegistry.find(userId))
                .willReturn(Optional.of(new UserSession(currentSid, UUID.randomUUID(), "chrome", Instant.now())));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer stale-sid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private Claims fakeClaims(UUID userId, UUID sid, String typ, UserRole role) {
        Claims claims = mock(Claims.class);
        lenient().when(claims.getSubject()).thenReturn(userId.toString());
        lenient().when(claims.get("sid", String.class)).thenReturn(sid.toString());
        lenient().when(claims.get("typ", String.class)).thenReturn(typ);
        lenient().when(claims.get("role", String.class)).thenReturn(role.name());
        return claims;
    }
}