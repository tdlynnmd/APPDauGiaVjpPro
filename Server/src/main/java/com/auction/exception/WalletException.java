package com.auction.exception;

/**
 * Ngoại lệ xảy ra khi xử lý giao dịch hoặc số dư ví tài chính.
 */
public class WalletException extends BaseException {
    public WalletException(WalletErrorCode errorCode) {
        super(errorCode.getDefaultMessage(), errorCode.getCode());
    }

    public WalletException(WalletErrorCode errorCode, String customMessage) {
        super(customMessage, errorCode.getCode());
    }
}