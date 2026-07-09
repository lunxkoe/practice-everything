package lunxkoe.practice.domain.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lunxkoe.practice.domain.auth.dto.request.SignInRequest;
import lunxkoe.practice.domain.auth.dto.response.JwtDto;
import lunxkoe.practice.domain.auth.service.AuthService;
import lunxkoe.practice.global.util.CookieUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final CookieUtil cookieUtil;
    private final AuthService authService;

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
}
