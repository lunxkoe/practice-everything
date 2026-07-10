package lunxkoe.practice.domain.user.dto.response;

import lunxkoe.practice.domain.user.entity.User;
import lunxkoe.practice.global.common.enums.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserDto(
        UUID id,
        LocalDateTime createdAt,
        String name,
        String email,
        UserRole role,
        boolean locked
) {

    public static UserDto from(User user) {
        return new UserDto(
                user.getExternalId(),
                user.getCreatedAt(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isLocked()
        );
    }
}
