package lunxkoe.practice.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lunxkoe.practice.domain.user.entity.User;

public record JwtDto(
        UserDto userDto,
        String accessToken,
        @JsonIgnore
        String refreshToken
) {

    public static JwtDto from(User user, String accessToken, String refreshToken) {
        return new JwtDto(
                UserDto.from(user),
                accessToken,
                refreshToken
        );
    }
}
