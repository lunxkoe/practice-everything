package lunxkoe.practice.domain.user.service;

import lombok.RequiredArgsConstructor;
import lunxkoe.practice.domain.auth.repository.TemporaryPasswordRepository;
import lunxkoe.practice.domain.user.dto.request.UserCreateRequest;
import lunxkoe.practice.domain.user.dto.response.UserDto;
import lunxkoe.practice.domain.user.entity.User;
import lunxkoe.practice.domain.user.repository.UserRepository;
import lunxkoe.practice.global.exception.CustomException;
import lunxkoe.practice.global.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final TemporaryPasswordRepository temporaryPasswordRepository;

    @Transactional
    public UserDto signUp(UserCreateRequest request) {

        // 이메일 중복 검사
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 사용자 생성
        User newUser = User.createLocalUser(
                request.name(),
                request.email(),
                passwordEncoder.encode(request.password())
        );
        User savedUser = userRepository.save(newUser);

        return UserDto.from(savedUser);
    }

    @Transactional
    public void changePassword(UUID requestUserId, UUID userId, String newRawPassword) {

        if (!requestUserId.equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.changePassword(passwordEncoder.encode(newRawPassword));
        temporaryPasswordRepository.deleteById(user.getEmail()); // v파기
    }
}
