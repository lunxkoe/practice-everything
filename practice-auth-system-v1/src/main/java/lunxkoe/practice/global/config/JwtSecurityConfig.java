package lunxkoe.practice.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lunxkoe.practice.global.common.enums.UserRole;
import lunxkoe.practice.global.exception.ErrorCode;
import lunxkoe.practice.global.exception.ErrorResponse;
import lunxkoe.practice.global.exception.ErrorResponseWriter;
import lunxkoe.practice.global.jwt.JwtAuthenticationFilter;
import lunxkoe.practice.global.jwt.JwtProvider;
import lunxkoe.practice.global.security.SessionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class JwtSecurityConfig {

    private final JwtProvider jwtProvider;
    private final SessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);

        http.csrf(AbstractHttpConfigurer::disable); // 개발 전용
        // TODO: CSRF 설정 추가
//        http.csrf(csrf -> csrf
//                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
//                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
//        );

        http.sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/favicon.ico", "/css/**", "/js/**", "/images/**", "/assets/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                .requestMatchers("/main").permitAll()
                .requestMatchers("/my/**").hasAnyAuthority(UserRole.USER.name(), UserRole.ADMIN.name())
                .requestMatchers("/admin/**").hasAuthority(UserRole.ADMIN.name())

                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/sign-in").permitAll()

                .anyRequest().authenticated()
        );

        // TODO: Exception 설정 추가
        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                        ErrorResponseWriter.write(response, objectMapper, ErrorCode.UNAUTHORIZED_REQUEST))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                        ErrorResponseWriter.write(response, objectMapper, ErrorCode.ACCESS_DENIED))
        );

        // TODO: JwtFilter 설정 추가
        http.addFilterBefore(new JwtAuthenticationFilter(jwtProvider, sessionRegistry, objectMapper), UsernamePasswordAuthenticationFilter.class);

        // TODO: OAuth2 설정 추가

        return http.build();
    }

    // TODO: CORS 설정 추가
}
