package lunxkoe.practice.domain.user.service;

import lunxkoe.practice.domain.auth.repository.TemporaryPasswordRepository;
import lunxkoe.practice.domain.user.dto.request.UserCreateRequest;
import lunxkoe.practice.domain.user.dto.response.UserDto;
import lunxkoe.practice.domain.user.entity.User;
import lunxkoe.practice.domain.user.repository.UserRepository;
import lunxkoe.practice.global.common.enums.UserRole;
import lunxkoe.practice.global.exception.CustomException;
import lunxkoe.practice.global.exception.ErrorCode;
import lunxkoe.practice.global.security.SessionRegistry;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    TemporaryPasswordRepository temporaryPasswordRepository;

    @Mock
    SessionRegistry sessionRegistry;

    @InjectMocks
    UserService userService;

    private User sampleUser() {
        return User.createLocalUser("우디", "woody@example.com", "ENCODED_PW");
    }

    @Nested
    class SignUp {

        @Test
        void 이미_존재하는_이메일이면_예외를_던진다() {
            String name = "testN";
            String email = "test@gmail.com";
            String password = "testP";
            UserCreateRequest request = new UserCreateRequest(name, email, password);

            given(userRepository.existsByEmail(email)).willReturn(true);

            assertThatThrownBy(() -> userService.signUp(request))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
        }

        @Test
        void 정상_회원가입시_비밀번호는_암호화되어_저장된다() {
            String name = "testN";
            String email = "test@gmail.com";
            String password = "testP";
            UserCreateRequest request = new UserCreateRequest(name, email, password);

            given(userRepository.existsByEmail(email)).willReturn(false);
            given(passwordEncoder.encode(password)).willReturn("ENCODED");
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

            UserDto result = userService.signUp(request);

            assertThat(result.email()).isEqualTo(email);
            assertThat(result.name()).isEqualTo(name);
            verify(passwordEncoder).encode(password);
        }
    }

    @Nested
    class ChangePassword {

        @Test
        void 본인이_아닌_userId로_요청하면_ACCESS_DENIED를_던지고_아무것도_조회하지_않는다() {
            UUID requestUserId = UUID.randomUUID();
            UUID targetUserId = UUID.randomUUID(); // 다른 사람의 ID

            assertThatThrownBy(() -> userService.changePassword(requestUserId, targetUserId, "new-pw"))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ACCESS_DENIED);

            verifyNoInteractions(userRepository, passwordEncoder, temporaryPasswordRepository);
        }

        @Test
        void 본인_비밀번호를_변경하면_암호화되어_저장되고_임시비밀번호를_파기한다() {
            User user = sampleUser();
            UUID userId = user.getId();
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(passwordEncoder.encode("new-raw-pw")).willReturn("NEW_ENCODED_PW");

            userService.changePassword(userId, userId, "new-raw-pw");

            assertThat(user.getPassword()).isEqualTo("NEW_ENCODED_PW");
            verify(temporaryPasswordRepository).deleteById(user.getEmail());
        }

        @Test
        void 존재하지_않는_유저면_USER_NOT_FOUND를_던진다() {
            UUID userId = UUID.randomUUID();
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.changePassword(userId, userId, "new-pw"))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);

            verifyNoInteractions(temporaryPasswordRepository);
        }
    }

    @Nested
    class UpdateLock {

        @Test
        void 존재하지_않는_유저면_USER_NOT_FOUND를_던진다() {
            UUID userId = UUID.randomUUID();
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateLock(userId, true))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);

            verifyNoInteractions(sessionRegistry);
        }

        @Test
        void locked_true면_계정을_잠그고_세션을_폐기한다() {
            User user = sampleUser();
            given(userRepository.findById(user.getId())).willReturn(Optional.of(user));

            UserDto result = userService.updateLock(user.getId(), true);

            assertThat(user.isLocked()).isTrue();
            assertThat(result.locked()).isTrue();
            verify(sessionRegistry).revoke(user.getId());
        }

        @Test
        void locked_false면_계정_잠금을_해제하고_세션을_폐기한다() {
            User user = sampleUser();
            user.lock();
            given(userRepository.findById(user.getId())).willReturn(Optional.of(user));

            UserDto result = userService.updateLock(user.getId(), false);

            assertThat(user.isLocked()).isFalse();
            assertThat(result.locked()).isFalse();
            verify(sessionRegistry).revoke(user.getId());
        }
    }

    @Nested
    class UpdateRole {

        @Test
        void 존재하지_않는_유저면_USER_NOT_FOUND를_던진다() {
            UUID userId = UUID.randomUUID();
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateRole(userId, UserRole.ADMIN))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);

            verifyNoInteractions(sessionRegistry);
        }

        @Test
        void 역할을_변경하고_세션을_폐기해_기존_액세스_토큰의_role_클레임을_무효화한다() {
            User user = sampleUser();
            given(userRepository.findById(user.getId())).willReturn(Optional.of(user));

            UserDto result = userService.updateRole(user.getId(), UserRole.ADMIN);

            assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
            assertThat(result.role()).isEqualTo(UserRole.ADMIN);
            verify(sessionRegistry).revoke(user.getId());
        }
    }
}
