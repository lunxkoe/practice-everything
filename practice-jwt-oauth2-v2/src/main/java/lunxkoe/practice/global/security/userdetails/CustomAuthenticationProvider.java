package lunxkoe.practice.global.security.userdetails;

import lombok.RequiredArgsConstructor;
import lunxkoe.practice.domain.user.entity.TmpPassword;
import lunxkoe.practice.domain.user.repository.TmpPasswordRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final CustomUserDetailsService customUserDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final TmpPasswordRepository tmpPasswordRepository;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // 1. 확실한 방어: 비밀번호(Credentials)가 없으면 처리 대상이 아님
        if (authentication.getCredentials() == null) {
            return null;
        }

        String email = authentication.getName();
        String password = authentication.getCredentials().toString();

        UserDetails user = customUserDetailsService.loadUserByUsername(email);

        // 1. 임시 비밀번호 테이블 확인 (먼저 체크)
        Optional<TmpPassword> tmp = tmpPasswordRepository.findByEmail(email);
        if (tmp.isPresent() && !tmp.get().isExpired()) {
            // 암호화하지 않은 원본 비밀번호와 비교 (TmpPassword에 저장할 때 인코딩 안한 원본 저장 추천)
            if (passwordEncoder.matches(password, tmp.get().getTmpPassword())) {
                // 임시 비번 로그인 성공 시 파기
                tmpPasswordRepository.delete(tmp.get());
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(user, password, user.getAuthorities());
                authToken.setDetails("TMP_LOGIN");
                return authToken;
            }
        }

        // 2. 일반 로그인 (기존 로직)
        if (passwordEncoder.matches(password, user.getPassword())) {
            return new UsernamePasswordAuthenticationToken(user, password, user.getAuthorities());
        }

        throw new BadCredentialsException("인증 실패");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

