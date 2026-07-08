package lunxkoe.practice.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lunxkoe.practice.domain.user.dto.request.SignUpRequest;
import lunxkoe.practice.domain.user.dto.response.UserDto;
import lunxkoe.practice.domain.user.entity.User;
import lunxkoe.practice.domain.user.repository.UserRepository;
import lunxkoe.practice.global.exception.controller.CustomException;
import lunxkoe.practice.global.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Transactional
    public UserDto signUp(SignUpRequest request) {

        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.EMAIL_DUPLICATE);
        }

        // 일반 사용자 생성
        User newUser = userRepository.save(
                User.createUser(
                    request.name(),
                    request.email(),
                    passwordEncoder.encode(request.password())
            )
        );

        return UserDto.from(newUser);
    }
}
