package lunxkoe.practice.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Global
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "G001", "올바르지 않은 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G002", "서버 내부 오류가 발생했습니다."),

    // User
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U001", "이미 가입된 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U002", "존재하지 않는 사용자입니다."),

    // Security
    UNAUTHORIZED_REQUEST(HttpStatus.UNAUTHORIZED, "S001", "로그인이 필요한 요청입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "S002", "해당 리소스에 접근할 권한이 없습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "S003", "아이디 또는 비밀번호가 일치하지 않습니다."),
    ACCOUNT_LOCKED(HttpStatus.UNAUTHORIZED, "S004", "계정이 잠겨 있습니다. 관리자에게 문의하세요."),
    CONCURRENT_LOGIN_DETECTED(HttpStatus.UNAUTHORIZED, "S005", "다른 기기에서 로그인되어 접속이 차단되었습니다."),

    // Token (모두 401 UNAUTHORIZED 로 통일)
    MISSING_TOKEN(HttpStatus.UNAUTHORIZED, "T001", "요청에 인증 토큰이 포함되지 않았습니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "T002", "만료된 인증 토큰입니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "T003", "유효하지 않은 액세스 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "T004", "유효하지 않은 리프레시 토큰입니다."),
    COMPROMISED_TOKEN(HttpStatus.UNAUTHORIZED, "T005", "토큰 탈취가 의심되어 접근이 차단되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
