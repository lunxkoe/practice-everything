package lunxkoe.practice.domain.user.dto.request;

public record SignUpRequest(
        String name,
        String email,
        String password
) {
}
