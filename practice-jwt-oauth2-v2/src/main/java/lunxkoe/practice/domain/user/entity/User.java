package lunxkoe.practice.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lunxkoe.practice.global.common.entity.BaseTimeEntity;
import lunxkoe.practice.global.common.enums.UserRole;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "member_id", updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole userRole;

    @Column(nullable = false)
    private boolean locked;

    @Builder
    public User(UUID id, String name, String email, String password, UserRole userRole, boolean locked) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.userRole = userRole;
        this.locked = locked;
    }

    public static User createUser(String name, String email, String password) {
        return User.builder()
                .name(name)
                .email(email)
                .password(password)
                .userRole(UserRole.USER)
                .locked(false)
                .build();
    }

    public static User createAdmin(String name, String email, String password) {
        return User.builder()
                .name(name)
                .email(email)
                .password(password)
                .userRole(UserRole.ADMIN)
                .locked(false)
                .build();
    }

    // Domain Method

}
