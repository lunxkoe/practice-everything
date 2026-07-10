package lunxkoe.practice.domain.auth.dto.response;

import lunxkoe.practice.domain.user.dto.response.UserDto;

public record JwtDto(
        UserDto userDto,
        String accessToken
) {
}
