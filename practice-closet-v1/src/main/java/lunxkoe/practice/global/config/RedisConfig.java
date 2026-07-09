package lunxkoe.practice.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(host, port);
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();

        // 위에서 만든 커넥션 팩토리를 템플릿에 주입
        redisTemplate.setConnectionFactory(redisConnectionFactory());

        // Redis의 Key를 평문 문자열(String)로 저장하기 위한 직렬화 설정. (안 하면 깨진 글자로 저장됨)
        redisTemplate.setKeySerializer(new StringRedisSerializer());

        // Redis의 Value를 평문 문자열(String)로 저장하기 위한 직렬화 설정.
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        return redisTemplate;
    }
}
