package lunxkoe.practice.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableRedisRepositories(basePackages = "lunxkoe.practice.domain.auth.entity")
public class RedisConfig {
}
