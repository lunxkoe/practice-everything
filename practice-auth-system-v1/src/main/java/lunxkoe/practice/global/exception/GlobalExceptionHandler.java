package lunxkoe.practice.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * @Valid 또는 @Validated 조건에 맞지 않을 때 발생 (DTO 검증 실패)
     * @ModelAttribute 또는 @RequestBody
     */
    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(BindException e) {
        log.error("handleMethodArgumentNotValidException", e);
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        ErrorResponse response = ErrorResponse.of(errorCode, e.getBindingResult());
        return new ResponseEntity<>(response, errorCode.getStatus());
    }

    /**
     * JSON 형식 오류, 타입 불일치 (String 자리에 Number가 오는 등) 발생 시
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("handleHttpMessageNotReadableException", e);
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        ErrorResponse response = ErrorResponse.of(errorCode);
        return new ResponseEntity<>(response, errorCode.getStatus());
    }

    /**
     * 비즈니스 로직 수행 중 발생하는 커스텀 예외 처리
     */
    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        log.error("handleCustomException", e);
        ErrorResponse response = ErrorResponse.of(e);
        return new ResponseEntity<>(response, e.getErrorCode().getStatus());
    }

    /**
     * 그 외 예기치 못한 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("handleException", e);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        ErrorResponse response = ErrorResponse.of(errorCode);
        return new ResponseEntity<>(response, errorCode.getStatus());
    }
}