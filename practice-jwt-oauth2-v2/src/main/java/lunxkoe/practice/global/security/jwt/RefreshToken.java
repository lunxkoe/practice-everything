package lunxkoe.practice.global.security.jwt;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lunxkoe.practice.global.common.entity.BaseTimeEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "refresh_token_id", updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Builder
    public RefreshToken(UUID userId, String token, LocalDateTime expiredAt) {
        this.userId = userId;
        this.token = token;
        this.expiredAt = expiredAt;
    }

    public void update(String token, LocalDateTime expiredAt) {
        this.token = token;
        this.expiredAt = expiredAt;
    }
}
