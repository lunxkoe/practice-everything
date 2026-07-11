package lunxkoe.practice.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lunxkoe.practice.domain.user.entity.User;
import lunxkoe.practice.domain.user.repository.UserRepository;
import lunxkoe.practice.global.security.SessionRegistry;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoginFailureService {

    private static final int MAX_LOGIN_FAIL_COUNT = 5;
    private static final Duration LOGIN_FAIL_TTL = Duration.ofMinutes(30);
    private static final String LOGIN_FAIL_KEY_PREFIX = "login-fail:";

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final SessionRegistry sessionRegistry;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerLoginFailure(User user) {
        String key = key(user.getId());
        Long count = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, LOGIN_FAIL_TTL); // 시간을 연장
        if (count != null && count >= MAX_LOGIN_FAIL_COUNT) {
            user.lock();
            userRepository.save(user); // update 쿼리가 나감
            sessionRegistry.revoke(user.getId());
            redisTemplate.delete(key);
            log.warn("5회 연속 로그인 실패로 계정이 잠겼습니다. email={}", user.getEmail());
        }
    }

    public void clearLoginFailure(UUID userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(UUID userId) {
        return LOGIN_FAIL_KEY_PREFIX + userId.toString();
    }
}
