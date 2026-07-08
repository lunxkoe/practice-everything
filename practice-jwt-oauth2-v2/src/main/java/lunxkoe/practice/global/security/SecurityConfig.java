package lunxkoe.practice.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lunxkoe.practice.global.common.enums.UserRole;
import lunxkoe.practice.global.exception.security.JwtAccessDeniedHandler;
import lunxkoe.practice.global.security.jwt.JwtAccessTokenFilter;
import lunxkoe.practice.global.security.jwt.JwtProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j
@Configuration
@EnableWebSecurity
// TODO: Method 접근 제어 추가
@RequiredArgsConstructor
public class SecurityConfig {

    private final AccessDeniedHandler accessDeniedHandler;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);

        // TODO: CSRF 설정 추가
        http.csrf(AbstractHttpConfigurer::disable);

        http.sessionManagement((session) -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        http.authorizeHttpRequests((api) -> api
                .requestMatchers("/").permitAll()
                .requestMatchers(HttpMethod.GET, "/my").hasAnyAuthority(UserRole.USER.name(), UserRole.ADMIN.name())
                .requestMatchers(HttpMethod.GET, "/admin").hasAnyAuthority(UserRole.ADMIN.name())

                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/sign-in").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/reset-password").permitAll()

                .anyRequest().authenticated()
        );

        // TODO: Spring Security Exception 추가
        http.exceptionHandling((exception) -> exception
                .accessDeniedHandler(accessDeniedHandler)
                .authenticationEntryPoint(authenticationEntryPoint)
        );

        // TODO: JwtFilter 추가
        http.addFilterBefore(new JwtAccessTokenFilter(jwtProvider, objectMapper), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // TODO: CORS 설정 추가
}
