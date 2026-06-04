package com.auction.exception;

/**
 * Ngoại lệ xảy ra do vi phạm phân quyền truy cập.
 */
public class AuthorizationException extends BaseException {

    public AuthorizationException(AuthorizationErrorCode errorCode) {
        super(errorCode.getDefaultMessage(), errorCode.getCode());
    }

    public AuthorizationException(AuthorizationErrorCode errorCode, String customMessage) {
        super(customMessage, errorCode.getCode());
    }
}