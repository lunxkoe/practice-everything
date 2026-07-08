package lunxkoe.practice.domain.user.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lunxkoe.practice.domain.user.dto.request.SignUpRequest;
import lunxkoe.practice.domain.user.dto.response.UserDto;
import lunxkoe.practice.domain.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @PostMapping()
    public ResponseEntity<UserDto> signUp(@Validated @RequestBody SignUpRequest request) {
        UserDto response = userService.signUp(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
