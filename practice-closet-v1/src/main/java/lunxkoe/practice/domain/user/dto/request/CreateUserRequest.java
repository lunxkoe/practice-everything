package lunxkoe.practice.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "이름은 필수 입력 값입니다.")
        @Size(min = 2, max = 20, message = "이름은 2~20자 사이로 입력해주세요.")
        String name,

        @NotBlank(message = "이메일은 필수 입력 값입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
//        @Pattern(
//                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$",
//                message = "비밀번호는 8~20자리이면서 영문, 숫자, 특수문자를 포함해야 합니다."
//        )
        String password
) {
}
