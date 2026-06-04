package com.auction.exception;

/**
 * Lớp biểu diễn thực thể AuthenticationException trong hệ thống.
 */
public class AuthenticationException extends BaseException {

    public AuthenticationException(AuthErrorCode errorCode) {
        super(errorCode.getDefaultMessage(), errorCode.getCode());
    }

    public AuthenticationException(AuthErrorCode errorCode, String customMessage) {
        super(customMessage, errorCode.getCode());
    }
}