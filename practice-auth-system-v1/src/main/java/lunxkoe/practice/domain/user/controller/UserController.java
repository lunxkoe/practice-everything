package lunxkoe.practice.domain.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lunxkoe.practice.domain.user.dto.request.ChangePasswordRequest;
import lunxkoe.practice.domain.user.dto.request.UserCreateRequest;
import lunxkoe.practice.domain.user.dto.response.UserDto;
import lunxkoe.practice.domain.user.service.UserService;
import lunxkoe.practice.global.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @PostMapping()
    public ResponseEntity<UserDto> signUp(@Valid @RequestBody UserCreateRequest request) {
        UserDto response = userService.signUp(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{userId}/password")
    public ResponseEntity<Void> changePassword(
            @PathVariable UUID userId,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(SecurityUtils.getCurrentUserId(), userId, request.password());
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}
