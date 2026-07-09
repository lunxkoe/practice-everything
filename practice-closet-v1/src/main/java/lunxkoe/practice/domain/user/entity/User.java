package lunxkoe.practice.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
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

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true, columnDefinition = "BINARY(16)")
    private UUID externalId;

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
        this.externalId = UUID.randomUUID();
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.locked = locked;
    }

    public User(UUID externalId, UserRole role) {
        this.externalId = externalId;
        this.role = role;
    }

    public static User createUserByUsingJwtFilter(UUID externalId, UserRole role) {
        return new User(externalId, role);
    }

    public static User createUserAccount(String name, String email, String password) {
        return new User(name, email, password, UserRole.USER, false);
    }

    public static User createAdminAccount(String name, String email, String password) {
        return new User(name, email, password, UserRole.ADMIN, false);
    }
}
