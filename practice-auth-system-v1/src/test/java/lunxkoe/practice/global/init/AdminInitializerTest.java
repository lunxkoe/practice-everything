package lunxkoe.practice.global.init;

import lunxkoe.practice.domain.user.entity.User;
import lunxkoe.practice.domain.user.repository.UserRepository;
import lunxkoe.practice.global.common.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    AdminInitializer adminInitializer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminInitializer, "adminEmail", "admin@example.com");
        ReflectionTestUtils.setField(adminInitializer, "adminPassword", "admin-raw-pw");
    }

    @Test
    void 이미_관리자_이메일이_존재하면_아무것도_생성하지_않는다() {
        given(userRepository.existsByEmail("admin@example.com")).willReturn(true);

        adminInitializer.run(null);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void 관리자_계정이_없으면_이메일과_암호화된_비밀번호로_ADMIN_계정을_생성한다() {
        given(userRepository.existsByEmail("admin@example.com")).willReturn(false);
        given(passwordEncoder.encode("admin-raw-pw")).willReturn("ENCODED_ADMIN_PW");

        adminInitializer.run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        // 과거에 name/email/password 인자 순서가 뒤바뀌어 email 칸에 비밀번호 해시가 들어가던 버그의 회귀 테스트
        assertThat(saved.getEmail()).isEqualTo("admin@example.com");
        assertThat(saved.getPassword()).isEqualTo("ENCODED_ADMIN_PW");
        assertThat(saved.getRole()).isEqualTo(UserRole.ADMIN);
    }
}
