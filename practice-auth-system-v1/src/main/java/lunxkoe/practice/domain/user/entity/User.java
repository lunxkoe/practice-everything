package lunxkoe.practice.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lunxkoe.practice.global.common.entity.BaseTimeEntity;
import lunxkoe.practice.global.common.enums.UserRole;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_internal_id")
    private Long internal_id;

    @Column(name = "user_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private boolean locked;

    @Builder
    public User(String name, String email, String password, UserRole role) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.locked = false;
    }

    public static User createLocalUser(String name, String email, String encodedPassword) {
        return User.builder()
                .name(name)
                .email(email)
                .password(encodedPassword)
                .role(UserRole.USER)
                .build();
    }

    public static User createSocialUser(String name, String email, String unusableEncodedPassword) {
        return User.builder()
                .name(name)
                .email(email)
                .password(unusableEncodedPassword)
                .role(UserRole.USER)
                .build();
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void changeRole(UserRole role) {
        this.role = role;
    }

    public void lock() {
        this.locked = true;
    }

    public void unlock() {
        this.locked = false;
    }
}
