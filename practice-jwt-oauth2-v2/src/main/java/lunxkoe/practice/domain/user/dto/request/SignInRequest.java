package lunxkoe.practice.domain.user.dto.request;

public record SignInRequest(
        String username,
        String password
) {
}
