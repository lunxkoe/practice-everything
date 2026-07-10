package lunxkoe.practice.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.springframework.validation.BindingResult;

import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class ErrorResponse {

    private final String exceptionName;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final Map<String, String> details;

    @Builder
    private ErrorResponse(String exceptionName, String message, Map<String, String> details) {
        this.exceptionName = exceptionName;
        this.message = message;
        this.details = details;
    }

    // 1. 기본 예외 응답 (details 없음)
    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .exceptionName(errorCode.name())
                .message(errorCode.getMessage())
                .build();
    }

    // 2. 커스텀 예외 응답 (CustomException 내부의 details Map을 그대로 사용)
    public static ErrorResponse of(CustomException e) {
        return ErrorResponse.builder()
                .exceptionName(e.getErrorCode().name())
                .message(e.getErrorCode().getMessage())
                .details(e.getDetails())
                .build();
    }

    // 3. Validation 검증 실패 응답 (BindingResult를 Map으로 변환하여 사용)
    public static ErrorResponse of(ErrorCode errorCode, BindingResult bindingResult) {
        return ErrorResponse.builder()
                .exceptionName(errorCode.name())
                .message(errorCode.getMessage())
                .details(createDetails(bindingResult))
                .build();
    }

    // BindingResult의 필드 에러들을 Map<필드명, 에러메시지> 형태로 변환
    private static Map<String, String> createDetails(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .collect(Collectors.toMap(
                        org.springframework.validation.FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        // 동일한 필드에 여러 에러가 있을 경우 첫 번째 메시지 유지
                        (existingMessage, newMessage) -> existingMessage
                ));
    }
}