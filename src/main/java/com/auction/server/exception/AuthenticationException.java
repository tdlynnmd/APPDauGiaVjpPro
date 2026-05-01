package com.auction.server.exception;

public class AuthenticationException extends BaseException {
    private final AuthErrorCode errorCodeEnum;

    /**     * Constructor với AuthErrorCode enum (Recommended)     */
    public AuthenticationException(AuthErrorCode errorCode) {
        super(errorCode.getMessage(), errorCode.getCode());
        this.errorCodeEnum = errorCode;
    }
}
