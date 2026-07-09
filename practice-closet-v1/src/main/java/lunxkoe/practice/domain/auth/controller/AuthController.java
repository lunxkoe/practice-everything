package lunxkoe.practice.domain.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lunxkoe.practice.domain.auth.dto.request.ResetPasswordRequest;
import lunxkoe.practice.domain.auth.dto.request.SignInRequest;
import lunxkoe.practice.domain.auth.dto.response.JwtDto;
import lunxkoe.practice.domain.auth.service.AuthService;
import lunxkoe.practice.domain.auth.service.MailService;
import lunxkoe.practice.global.security.CustomUserDetails;
import lunxkoe.practice.global.util.CookieUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final CookieUtil cookieUtil;
    private final AuthService authService;
    private final MailService mailService;

    @PostMapping("/sign-in")
    public ResponseEntity<JwtDto> signIn(
            @Valid @ModelAttribute SignInRequest request,
            HttpServletResponse servletResponse
    ) {
        JwtDto response = authService.signIn(request);

        servletResponse.addCookie(cookieUtil.createRefreshCookie(response.refreshToken()));

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtDto> refresh(
            @CookieValue(value = "REFRESH_TOKEN", required = false) String refreshToken,
            HttpServletResponse servletResponse
    ) {
        JwtDto response = authService.refresh(refreshToken);

        servletResponse.addCookie(cookieUtil.createRefreshCookie(response.refreshToken()));

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PostMapping("/sign-out")
    public ResponseEntity<Void> signOut(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        authService.signOut(userDetails);
        return ResponseEntity
                .status(HttpStatus.OK)
                .build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        String tempPassword = authService.resetPassword(request);
        mailService.sendPasswordResetEmail(request.email(), tempPassword);
        return ResponseEntity
                .status(HttpStatus.OK)
                .build();
    }
}
