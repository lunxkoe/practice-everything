package lunxkoe.practice.domain.user.service;

import lunxkoe.practice.domain.user.dto.request.UserCreateRequest;
import lunxkoe.practice.domain.user.dto.response.UserDto;
import lunxkoe.practice.domain.user.entity.User;
import lunxkoe.practice.domain.user.repository.UserRepository;
import lunxkoe.practice.global.exception.CustomException;
import lunxkoe.practice.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserService userService;

    @Test
    void 이미_존재하는_이메일이면_예외를_던진다() {
        // given
        String name = "testN";
        String email = "test@gmail.com";
        String password = "testP";
        UserCreateRequest request = new UserCreateRequest(name, email, password);

        given(userRepository.existsByEmail(email)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.signUp(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    void 정상_회원가입시_비밀번호는_암호화되어_저장된다() {
        // given
        String name = "testN";
        String email = "test@gmail.com";
        String password = "testP";
        UserCreateRequest request = new UserCreateRequest(name, email, password);

        given(userRepository.existsByEmail(email)).willReturn(false);
        given(passwordEncoder.encode(password)).willReturn("ENCODED");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            return invocation.getArgument(0);
        });

        // when
        UserDto result = userService.signUp(request);

        // then
        assertThat(result.email()).isEqualTo(email);
        assertThat(result.name()).isEqualTo(name);
        verify(passwordEncoder).encode(password);
    }
}