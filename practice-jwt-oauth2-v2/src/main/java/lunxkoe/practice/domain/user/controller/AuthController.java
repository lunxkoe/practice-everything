package lunxkoe.practice.domain.user.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lunxkoe.practice.domain.email.service.EmailService;
import lunxkoe.practice.domain.user.dto.request.ResetPasswordRequest;
import lunxkoe.practice.domain.user.dto.request.SignInRequest;
import lunxkoe.practice.domain.user.dto.response.JwtDto;
import lunxkoe.practice.domain.user.service.AuthService;
import lunxkoe.practice.global.exception.ErrorCode;
import lunxkoe.practice.global.exception.controller.CustomException;
import lunxkoe.practice.global.security.jwt.TokenRevocationService;
import lunxkoe.practice.global.security.jwt.exception.TokenHijackedException;
import lunxkoe.practice.global.security.userdetails.CustomUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final String REFRESH_TOKEN = "REFRESH_TOKEN";

    private final AuthService authService;
    private final TokenRevocationService tokenRevocationService;
    private final EmailService emailService;

    @Value("${jwt.refresh-token-validity-in-seconds}")
    private int refreshTokenValidityInSeconds;

    @PostMapping("/sign-in")
    public ResponseEntity<JwtDto> signIn(
            @Validated @ModelAttribute SignInRequest request,
            HttpServletResponse servletResponse)
    {
        JwtDto response = authService.signIn(request);

        servletResponse.addCookie(createCookie(REFRESH_TOKEN, response.refreshToken(), refreshTokenValidityInSeconds));

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtDto> refresh(
            @CookieValue(value = REFRESH_TOKEN, required = false) String refreshToken,
            HttpServletResponse servletResponse
    ) {
        try {
            JwtDto response = authService.refresh(refreshToken);

            servletResponse.addCookie(createCookie(REFRESH_TOKEN, response.refreshToken(), refreshTokenValidityInSeconds));

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(response);
        } catch (TokenHijackedException e) {
            log.warn("토큰 탈취 시도 감지! 해당 계정의 세션을 강제 종료합니다. UserId: {}", e.getUserId());

            tokenRevocationService.revokeToken(e.getUserId());

            throw new CustomException(ErrorCode.TOKEN_HIJACKED);
        }
    }

    @PostMapping("/sign-out")
    public ResponseEntity<Void> signOut(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletResponse servletResponse
    ) {
        tokenRevocationService.revokeToken(userDetails.getUser().getId());

        servletResponse.addCookie(createCookie(REFRESH_TOKEN, null, 0));

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Validated @RequestBody ResetPasswordRequest request) {
        String tmpPassword = authService.resetPassword(request);
        log.info("tmpPassword = {}", tmpPassword);
        emailService.sendPasswordResetEmail(request.email(), tmpPassword);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    private Cookie createCookie(String key, String value, int maxAge) {

        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(maxAge);
        //cookie.setSecure(true);                 // HTTPS 통신 시 넣어주면 됨
        cookie.setPath("/api/auth/refresh");      // 적용될 범위를 지정할 수 있음
        cookie.setHttpOnly(true);

        return cookie;
    }
}
