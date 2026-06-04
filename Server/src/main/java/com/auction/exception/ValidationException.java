package com.auction.exception;

/**
 * Ngoại lệ xảy ra khi dữ liệu đầu vào không hợp lệ.
 */
public class ValidationException extends BaseException {

    public ValidationException(ValidationErrorCode errorCode) {
        super(errorCode.getDefaultMessage(), errorCode.getCode());
    }

    public ValidationException(ValidationErrorCode errorCode, String customMessage) {
        super(customMessage, errorCode.getCode());
    }
}