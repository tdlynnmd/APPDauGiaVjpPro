package com.auction.server.exception;

public enum AuthErrorCode {
    USERNAME_NULL_EMPTY("AUTH_USERNAME_001", "Username must not be empty"),
    USERNAME_TOO_SHORT("AUTH_USERNAME_002", "Username must be at least 5 characters long"),
    USERNAME_TOO_LONG("AUTH_USERNAME_003", "Username must not exceed 20 characters"),
    USERNAME_INVALID_FORMAT("AUTH_USERNAME_004", "Username can only contain letters, numbers, dots (.) and underscores (_)"),
    USERNAME_ALREADY_EXISTS("AUTH_USERNAME_005", "Username is already taken"),

    EMAIL_NULL_EMPTY("AUTH_EMAIL_001", "Email must not be empty"),
    EMAIL_INVALID_FORMAT("AUTH_EMAIL_002", "Invalid email format, for example: user@example.com"),
    EMAIL_ALREADY_EXISTS("AUTH_EMAIL_003", "Email is already registered"),

    INPUT_NULL_EMPTY("AUTH_INPUT_001", "Username/email or password must not be empty"),

    PASSWORD_NULL_EMPTY("AUTH_PASSWORD_001", "Password must not be empty"),
    PASSWORD_TOO_SHORT("AUTH_PASSWORD_002", "Password must be at least 8 characters long"),
    PASSWORD_WEAK("AUTH_PASSWORD_003", "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character (@#$%^&+=!)"),

    ROLE_INVALID("AUTH_ROLE_001", "Invalid role. Allowed roles are ADMIN, SELLER, and BIDDER"),
    UNKNOWN_ERROR("AUTH_UNKNOWN_001", "Unknown error"),

    ACCOUNT_INACTIVE("AUTH_ACC_001", "This account has been deactivated"),
    ACCOUNT_LOCKED("AUTH_ACC_002", "This account has been locked because of too many failed login attempts"),

    USER_NOT_FOUND("AUTH_USER_001", "User does not exist"),
    USER_NOT_ONLINE("AUTH_USER_002", "User is not online"),
    USER_NULL("AUTH_USER_003", "User must not be null"),

    // ===== GENERAL ERRORS =====
    INVALID_CREDENTIALS("AUTH_GEN_001", "Invalid username/email or password");

    private final String code;
    private final String message;

    AuthErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}