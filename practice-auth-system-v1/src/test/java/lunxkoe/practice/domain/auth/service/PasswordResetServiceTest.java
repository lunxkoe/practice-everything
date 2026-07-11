package lunxkoe.practice.domain.auth.service;

import lunxkoe.practice.domain.auth.entity.TemporaryPassword;
import lunxkoe.practice.domain.auth.password.TemporaryPasswordGenerator;
import lunxkoe.practice.domain.auth.repository.TemporaryPasswordRepository;
import lunxkoe.practice.domain.user.entity.User;
import lunxkoe.practice.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    TemporaryPasswordRepository temporaryPasswordRepository;
    @Mock
    TemporaryPasswordGenerator temporaryPasswordGenerator;

    @InjectMocks
    PasswordResetService passwordResetService;

    @Test
    void 존재하는_이메일이면_암호화된_임시비밀번호를_3분_TTL로_저장하고_평문을_반환한다() {
        User user = User.createLocalUser("우디", "woody@example.com", "ENCODED_PW");
        given(userRepository.findByEmail("woody@example.com")).willReturn(Optional.of(user));
        given(temporaryPasswordGenerator.generate()).willReturn("Temp1234!");
        given(passwordEncoder.encode("Temp1234!")).willReturn("ENCODED_TEMP");

        Optional<String> result = passwordResetService.requestReset("woody@example.com");

        assertThat(result).contains("Temp1234!");

        ArgumentCaptor<TemporaryPassword> captor = ArgumentCaptor.forClass(TemporaryPassword.class);
        verify(temporaryPasswordRepository).save(captor.capture());
        TemporaryPassword saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("woody@example.com");
        assertThat(saved.getEncodedPassword()).isEqualTo("ENCODED_TEMP"); // 평문이 아니라 암호화된 값이 저장돼야 함
        assertThat(saved.getTtlSeconds()).isEqualTo(180L);
    }

    @Test
    void 존재하지_않는_이메일이면_Optional_empty를_반환하고_아무것도_생성하지_않는다_계정_열거_공격_방지() {
        given(userRepository.findByEmail("ghost@example.com")).willReturn(Optional.empty());

        Optional<String> result = passwordResetService.requestReset("ghost@example.com");

        assertThat(result).isEmpty();
        verifyNoInteractions(passwordEncoder, temporaryPasswordGenerator, temporaryPasswordRepository);
    }
}
