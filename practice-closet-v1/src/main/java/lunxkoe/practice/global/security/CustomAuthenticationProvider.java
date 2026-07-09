package lunxkoe.practice.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final CustomUserDetailsService customUserDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // 비밀번호(Credentials)가 없으면 처리 대상이 아님
        if (authentication.getCredentials() == null) {
            return null;
        }

        String email = authentication.getName();
        String password = authentication.getCredentials().toString();

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // 1. 임시 비밀번호 테이블 확인 (먼저 체크)
        String encodedTempPassword = redisTemplate.opsForValue().get("TEMP_PWD:" + email);
        if (StringUtils.hasText(encodedTempPassword)) {
            if (passwordEncoder.matches(password, encodedTempPassword)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());
                authToken.setDetails("TEMP_LOGIN");
                return authToken;
            }
        }

        // 2. 일반 로그인 (기존 로직)
        if (passwordEncoder.matches(password, userDetails.getPassword())) {
            return new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());
        }

        throw new BadCredentialsException("인증 실패");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
