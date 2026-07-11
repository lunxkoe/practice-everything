package lunxkoe.practice.domain.auth.entity;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@Getter
@RedisHash("temporary-password")
public class TemporaryPassword {

    @Id
    private final String email;
    private final String encodedPassword;

    @TimeToLive
    private final Long ttlSeconds;

    public TemporaryPassword(String email, String encodedPassword, Long ttlSeconds) {
        this.email = email;
        this.encodedPassword = encodedPassword;
        this.ttlSeconds = ttlSeconds;
    }
}
