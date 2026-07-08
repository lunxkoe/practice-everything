package lunxkoe.practice.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lunxkoe.practice.global.common.entity.BaseTimeEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tmp_passwords")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TmpPassword extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tmp_password_id")
    private UUID id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    private String tmpPassword;

    private LocalDateTime expiredAt;

    @Builder
    public TmpPassword(String email, String tmpPassword) {
        this.email = email;
        this.tmpPassword = tmpPassword;
        this.expiredAt = LocalDateTime.now().plusMinutes(3);
    }

    public void update(String tmpPassword) {
        this.tmpPassword = tmpPassword;
        this.expiredAt = LocalDateTime.now().plusMinutes(3);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiredAt);
    }
}
