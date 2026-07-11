package lunxkoe.practice.global.ratelimit;

import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import lunxkoe.practice.global.exception.CustomException;
import lunxkoe.practice.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    @Mock
    LettuceBasedProxyManager<String> bucketProxyManager;
    @Mock
    HttpServletRequest request;
    @Mock
    RemoteBucketBuilder<String> remoteBucketBuilder;
    @Mock
    BucketProxy bucket;

    @InjectMocks
    RateLimitAspect rateLimitAspect;

    @RateLimit(key = "sign-in", capacity = 5, refillMinutes = 1)
    private void dummyLimitedMethod() {
    }

    private RateLimit rateLimitAnnotation() throws NoSuchMethodException {
        Method method = RateLimitAspectTest.class.getDeclaredMethod("dummyLimitedMethod");
        return method.getAnnotation(RateLimit.class);
    }

    @Test
    void 버킷에_토큰이_남아있으면_통과시킨다() throws NoSuchMethodException {
        given(request.getRemoteAddr()).willReturn("127.0.0.1");
        given(bucketProxyManager.builder()).willReturn(remoteBucketBuilder);
        given(remoteBucketBuilder.build(eq("rate-limit:sign-in:127.0.0.1"), any(Supplier.class))).willReturn(bucket);
        given(bucket.tryConsume(1)).willReturn(true);

        assertThatCode(() -> rateLimitAspect.checkRateLimit(rateLimitAnnotation()))
                .doesNotThrowAnyException();
    }

    @Test
    void 버킷이_고갈되면_RATE_LIMIT_EXCEEDED를_던진다() throws NoSuchMethodException {
        given(request.getRemoteAddr()).willReturn("127.0.0.1");
        given(bucketProxyManager.builder()).willReturn(remoteBucketBuilder);
        given(remoteBucketBuilder.build(eq("rate-limit:sign-in:127.0.0.1"), any(Supplier.class))).willReturn(bucket);
        given(bucket.tryConsume(1)).willReturn(false);

        assertThatThrownBy(() -> rateLimitAspect.checkRateLimit(rateLimitAnnotation()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.RATE_LIMIT_EXCEEDED);
    }

    @Test
    void 요청자_IP별로_서로_다른_버킷_키를_사용한다() throws NoSuchMethodException {
        given(request.getRemoteAddr()).willReturn("203.0.113.9");
        given(bucketProxyManager.builder()).willReturn(remoteBucketBuilder);
        given(remoteBucketBuilder.build(eq("rate-limit:sign-in:203.0.113.9"), any(Supplier.class))).willReturn(bucket);
        given(bucket.tryConsume(1)).willReturn(true);

        rateLimitAspect.checkRateLimit(rateLimitAnnotation());

        verify(remoteBucketBuilder).build(eq("rate-limit:sign-in:203.0.113.9"), any(Supplier.class));
    }
}
