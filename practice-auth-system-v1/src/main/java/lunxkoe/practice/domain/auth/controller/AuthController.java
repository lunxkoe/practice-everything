package lunxkoe.practice.domain.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lunxkoe.practice.domain.auth.dto.request.SignInRequest;
import lunxkoe.practice.domain.auth.dto.response.JwtDto;
import lunxkoe.practice.domain.auth.service.AuthService;
import lunxkoe.practice.global.jwt.RefreshCookieProvider;
import lunxkoe.practice.global.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieProvider refreshCookieProvider;

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
}
