package lunxkoe.practice.global.exception.controller;

import lombok.Getter;
import lunxkoe.practice.global.exception.ErrorCode;

@Getter
public class CustomException extends RuntimeException{

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
