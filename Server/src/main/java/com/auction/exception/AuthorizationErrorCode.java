package com.auction.exception;

/**
 * Lớp biểu diễn thực thể AuthorizationErrorCode trong hệ thống.
 */
public enum AuthorizationErrorCode {
    NOT_AUTHENTICATED("AUT_PERM_001", "Authentication required: Please login to perform this action"),
    ROLE_ACCESS_DENIED("AUT_PERM_002", "Access denied: Your account role does not have permission to execute this command"),
    ADMIN_PRIVILEGE_REQUIRED("AUT_PERM_003", "Administrative privileges are required to perform this action"),
    ACTION_UNAUTHORIZED("AUT_PERM_004", "Access denied: This system action is restricted or under configuration"),

    RESOURCE_OWNERSHIP_VIOLATION("AUT_OWN_001", "Access denied: You do not own this item or auction resource"),
    CANNOT_BID_ON_OWN_ITEM("AUT_OWN_002", "Bidding violation: You cannot place a bid on your own listed product item");

    private final String code;
    private final String message;

    AuthorizationErrorCode(String code, String message) {
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