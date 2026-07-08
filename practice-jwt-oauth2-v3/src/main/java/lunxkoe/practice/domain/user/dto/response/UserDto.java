package lunxkoe.practice.domain.user.dto.response;

import lunxkoe.practice.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserDto(
        UUID Id,
        LocalDateTime createdAt,
        String email,
        String name,
        String role,
        boolean locked
) {

    public static UserDto from(User user) {
        return new UserDto(
            user.getId(),
            user.getCreatedAt(),
            user.getEmail(),
            user.getName(),
            user.getUserRole().name(),
            user.isLocked()
        );
    }
}
