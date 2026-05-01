package com.auction.server.exception;

public enum AuthErrorCode {
    USERNAME_NULL_EMPTY("AUTH_USERNAME_001", "Username không được để trống"),
    USERNAME_TOO_SHORT("AUTH_USERNAME_002", "Username tối thiểu 5 ký tự"),
    USERNAME_TOO_LONG("AUTH_USERNAME_003", "Username tối đa 20 ký tự"),
    USERNAME_INVALID_FORMAT("AUTH_USERNAME_004", "Username chỉ chứa chữ cái, số, dấu chấm (.) và gạch dưới (_)"),
    USERNAME_ALREADY_EXISTS("AUTH_USERNAME_005", "Username đã được sử dụng"),

    EMAIL_NULL_EMPTY("AUTH_EMAIL_001", "Email không được để trống"),
    EMAIL_INVALID_FORMAT("AUTH_EMAIL_002", "Email không đúng định dạng (ví dụ: user@example.com)"),
    EMAIL_ALREADY_EXISTS("AUTH_EMAIL_003", "Email đã được đăng ký"),

    INPUT_NULL_EMPTY("AUTH_INPUT_001", "Username/Email hoặc mật khẩu không được để trống"),

    PASSWORD_NULL_EMPTY("AUTH_PASSWORD_001", "Mật khẩu không được để trống"),
    PASSWORD_TOO_SHORT("AUTH_PASSWORD_002", "Mật khẩu tối thiểu 8 ký tự"),
    PASSWORD_WEAK("AUTH_PASSWORD_003", "Mật khẩu phải chứa ít nhất: chữ hoa, chữ thường, số, ký tự đặc biệt (@#$%^&+=!)"),

    ROLE_INVALID("AUTH_ROLE_001", "Vai trò không hợp lệ (ADMIN, SELLER, BIDDER)"),
    UNKNOWN_ERROR("AUTH_UNKNOWN_001", "Lỗi KHÔNG XÁC ĐỊNH"),

    ACCOUNT_INACTIVE("AUTH_ACC_001", "Tài khoản đã bị vô hiệu hóa"),
    ACCOUNT_LOCKED("AUTH_ACC_002", "Tài khoản bị khóa do nhập sai mật khẩu nhiều lần"),

    USER_NOT_FOUND("AUTH_USER_001", "Người dùng không tồn tại"),
    USER_NOT_ONLINE("AUTH_USER_002", "Người dùng không ở trạng thái online"),
    USER_NULL("AUTH_USER_003", "Người dùng không được trống"),
    // ===== GENERAL ERRORS =====
    INVALID_CREDENTIALS("AUTH_GEN_001", "Thông tin đăng nhập hoặc mật khẩu không đúng");

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
