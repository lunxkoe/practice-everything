package lunxkoe.practice.global.exception;

import lombok.Getter;

import java.util.Collections;
import java.util.Map;

@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, String> details;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = Collections.emptyMap();
    }

    public CustomException(ErrorCode errorCode, Map<String, String> details) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = details;
    }
}