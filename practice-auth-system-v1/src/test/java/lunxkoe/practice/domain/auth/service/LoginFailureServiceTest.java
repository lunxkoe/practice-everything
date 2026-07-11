package lunxkoe.practice.domain.auth.service;

import lunxkoe.practice.domain.user.entity.User;
import lunxkoe.practice.domain.user.repository.UserRepository;
import lunxkoe.practice.global.security.SessionRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginFailureServiceTest {

    @Mock
    StringRedisTemplate redisTemplate;
    @Mock
    ValueOperations<String, String> valueOperations;
    @Mock
    UserRepository userRepository;
    @Mock
    SessionRegistry sessionRegistry;

    @InjectMocks
    LoginFailureService loginFailureService;

    private User sampleUser() {
        return User.createLocalUser("우디", "woody@example.com", "ENCODED_PW");
    }

    @Test
    void 실패_횟수가_5회_미만이면_계정을_잠그지_않고_TTL만_갱신한다() {
        User user = sampleUser();
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("login-fail:" + user.getId())).willReturn(3L);

        loginFailureService.registerLoginFailure(user);

        assertThat(user.isLocked()).isFalse();
        verify(redisTemplate).expire("login-fail:" + user.getId(), Duration.ofMinutes(30));
        verify(userRepository, never()).save(any(User.class));
        verify(sessionRegistry, never()).revoke(any());
    }

    @Test
    void 실패_횟수가_정확히_5회에_도달하면_계정을_잠그고_세션을_폐기하고_카운터를_지운다() {
        User user = sampleUser();
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("login-fail:" + user.getId())).willReturn(5L);

        loginFailureService.registerLoginFailure(user);

        assertThat(user.isLocked()).isTrue();
        verify(userRepository).save(user);
        verify(sessionRegistry).revoke(user.getId());
        verify(redisTemplate).delete("login-fail:" + user.getId());
    }

    @Test
    void 카운트가_5회를_초과해도_잠금과_세션폐기는_여전히_수행된다_이미_잠긴_계정에_대한_추가_시도() {
        User user = sampleUser();
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("login-fail:" + user.getId())).willReturn(7L);

        loginFailureService.registerLoginFailure(user);

        assertThat(user.isLocked()).isTrue();
        verify(sessionRegistry).revoke(user.getId());
    }

    @Test
    void Redis_증가_결과가_null이어도_예외없이_잠그지_않고_넘어간다() {
        User user = sampleUser();
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("login-fail:" + user.getId())).willReturn(null);

        loginFailureService.registerLoginFailure(user);

        assertThat(user.isLocked()).isFalse();
        verify(userRepository, never()).save(any(User.class));
        verify(sessionRegistry, never()).revoke(any());
    }

    @Test
    void 로그인에_성공하면_실패_카운터를_삭제한다() {
        UUID userId = UUID.randomUUID();

        loginFailureService.clearLoginFailure(userId);

        verify(redisTemplate).delete("login-fail:" + userId);
        verifyNoInteractions(valueOperations, userRepository, sessionRegistry);
    }
}
