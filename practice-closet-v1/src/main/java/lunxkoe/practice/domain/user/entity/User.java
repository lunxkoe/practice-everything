package lunxkoe.practice.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lunxkoe.practice.global.entity.BaseTimeEntity;
import lunxkoe.practice.global.enums.UserRole;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private boolean locked;

    public User(String name, String email, String password, UserRole role, boolean locked) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.locked = locked;
    }

    public static User createUserAccount(String name, String email, String password) {
        return new User(name, email, password, UserRole.USER, false);
    }

    public static User createAdminAccount(String name, String email, String password) {
        return new User(name, email, password, UserRole.ADMIN, false);
    }
}
