package lunxkoe.practice.global.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lunxkoe.practice.global.exception.CustomException;
import lunxkoe.practice.global.exception.ErrorCode;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final LettuceBasedProxyManager<String> bucketProxyManager;
    private final HttpServletRequest request;

    @Before("@annotation(rateLimit)")
    public void checkRateLimit(RateLimit rateLimit) {
        String clientKey = "rate-limit:" + rateLimit.key() + ":" + resolveClientIp();
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(rateLimit.capacity(), Duration.ofMinutes(rateLimit.refillMinutes())))
                .build();

        Bucket bucket = bucketProxyManager.builder().build(clientKey, () -> configuration);
        if (!bucket.tryConsume(1)) {
            throw new CustomException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
    }

    private String resolveClientIp() {
        return request.getRemoteAddr();
    }
}
