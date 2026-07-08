package lunxkoe.practice.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Global
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "올바르지 않은 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버에 오류가 발생했습니다."),

    // User
    EMAIL_DUPLICATE(HttpStatus.CONFLICT, "U001", "이미 가입된 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U002", "사용자를 찾을 수 없습니다."),

    // Security
    UNAUTHORIZED_USER(HttpStatus.UNAUTHORIZED, "S001", "인증되지 않은 요청입니다. (로그인 필요)"),
    FORBIDDEN_USER(HttpStatus.FORBIDDEN, "S002", "해당 리소스에 접근할 권한이 없습니다."),
    INVALID_USERNAME_OR_PASSWORD(HttpStatus.UNAUTHORIZED, "S003", "아이디 혹은 비밀번호가 일치하지 않습니다."),
    ACCOUNT_LOCKED(HttpStatus.UNAUTHORIZED, "S004", "계정이 잠긴 상태입니다. 관리자에게 문의하세요."),
    RE_LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "S005", "다른 계정에서 로그인하였습니다."),

    // Token
    EXPIRED_TOKEN(HttpStatus.BAD_REQUEST, "T001", "만료된 토큰입니다."),
    NOT_ACCESS_TOKEN(HttpStatus.BAD_REQUEST, "T002", "엑세스 토큰이 아닙니다."),
    TOKEN_NULL(HttpStatus.BAD_REQUEST, "T003", "토큰이 없습니다."),
    NOT_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "T004", "리프레시 토큰이 아닙니다."),
    TOKEN_HIJACKED(HttpStatus.UNAUTHORIZED, "T005", "토큰 탈취 위협");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
