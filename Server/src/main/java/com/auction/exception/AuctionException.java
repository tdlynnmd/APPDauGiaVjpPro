package com.auction.exception;

/**
 * Ngoại lệ đặc thù xảy ra trong nghiệp vụ đấu giá.
 */
public class AuctionException extends BaseException {

    public AuctionException(AuctionErrorCode errorCode) {
        super(errorCode.getDefaultMessage(), errorCode.getCode());
    }

    public AuctionException(AuctionErrorCode errorCode, String customMessage) {
        super(customMessage, errorCode.getCode());
    }
}