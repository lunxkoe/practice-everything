package lunxkoe.practice.domain.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lunxkoe.practice.domain.auth.dto.request.ResetPasswordRequest;
import lunxkoe.practice.domain.auth.dto.request.SignInRequest;
import lunxkoe.practice.domain.auth.dto.response.JwtDto;
import lunxkoe.practice.domain.auth.mail.MailService;
import lunxkoe.practice.domain.auth.service.AuthService;
import lunxkoe.practice.domain.auth.service.PasswordResetService;
import lunxkoe.practice.global.jwt.RefreshCookieProvider;
import lunxkoe.practice.global.ratelimit.RateLimit;
import lunxkoe.practice.global.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieProvider refreshCookieProvider;
    private final PasswordResetService passwordResetService;
    private final MailService mailService;

    @RateLimit(key = "sign-in", capacity = 5, refillMinutes = 1)
    @PostMapping("/sign-in")
    public ResponseEntity<JwtDto> signIn(
            @Valid @ModelAttribute SignInRequest request,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletResponse response
    ) {
        AuthService.SignInResult result = authService.signIn(request, userAgent);
        refreshCookieProvider.attach(response, result.refreshToken());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new JwtDto(result.userDto(), result.accessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtDto> refresh(
        @CookieValue(value = "REFRESH_TOKEN", required = false) String refreshToken,
        @RequestHeader(value = "User-Agent", required = false) String userAgent,
        HttpServletResponse response
    ) {
        AuthService.SignInResult result = authService.refresh(refreshToken, userAgent);
        refreshCookieProvider.attach(response, result.refreshToken());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new JwtDto(result.userDto(), result.accessToken()));
    }

    @PostMapping("/sign-out")
    public ResponseEntity<Void> signOut(
            HttpServletResponse response
    ) {
        authService.signOut(SecurityUtils.getCurrentUserId());
        refreshCookieProvider.clear(response);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @GetMapping("/csrf-token")
    public ResponseEntity<Void> csrfToken(CsrfToken csrfToken) {
        csrfToken.getToken();
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @RateLimit(key = "reset-password", capacity = 3, refillMinutes = 10)
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.requestReset(request.email())
                .ifPresent(temporaryPassword -> mailService.sendTemporaryPassword(request.email(), temporaryPassword));
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}
