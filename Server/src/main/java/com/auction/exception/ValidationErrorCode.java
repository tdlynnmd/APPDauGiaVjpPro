package com.auction.exception;

/**
 * Lớp biểu diễn thực thể ValidationErrorCode trong hệ thống.
 */
public enum ValidationErrorCode {
    BAD_REQUEST("VAL_REQ_001", "The request body format is invalid or cannot be parsed"),
    MISSING_REQUIRED_FIELD("VAL_REQ_002", "A required data field is missing from the payload"),
    INVALID_PARAMETER("VAL_REQ_003", "The provided parameter values violate domain business constraints"),

    INVALID_STEP_PRICE("VAL_REQ_004", "Step price must be strictly greater than zero"),
    START_TIME_IN_PAST("VAL_REQ_005", "Auction start time cannot be in the past"),
    INVALID_END_TIME("VAL_REQ_006", "End time must be after start time and meet the minimum duration requirement");

    private final String code;
    private final String message;

    ValidationErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return message;
    }
}