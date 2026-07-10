package lunxkoe.practice.global.jwt;

import lunxkoe.practice.global.common.enums.UserRole;

import java.util.UUID;

public record UserPrincipal(
        UUID userId,
        UserRole role
) {
}
