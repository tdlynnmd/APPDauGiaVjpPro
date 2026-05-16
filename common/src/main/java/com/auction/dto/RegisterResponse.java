package com.auction.dto;

/**
 Cho Client biết đăng ký thành công hay thất bại.
 Mang thông báo lỗi/thành công để hiển thị trên giao diện.
 Nếu thành công thì có thể trả thêm UserDTO.
 */

public class RegisterResponse {
    private boolean success;        //true nếu đăng ki thành công, false nếu thất bại
    private String message;         // thông báo lỗi cho người dùng
    private String errorCode;       // mã lõi kĩ thuật để debug
    private UserDTO user;           // thông tin user ko chứa password;

    public RegisterResponse(boolean success, String message, String errorCode, UserDTO user) {
        this.success = success;
        this.message = message;
        this.errorCode = errorCode;
        this.user = user;
    }
    /**
     * Tạo response thành công.
     * Dùng khi Server đã validate, tạo user và lưu user vào UserManage thành công.
     */
    public static RegisterResponse success(UserDTO user) {
        return new RegisterResponse(true, "Đăng ký thành công", null, user);
    }

    /**
     * Tạo response thất bại.
     * Dùng khi username/email/password không hợp lệ hoặc có lỗi hệ thống.
     */
    public static RegisterResponse failure(String message, String errorCode) {
        return new RegisterResponse(false, message, errorCode, null);
    }
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public UserDTO getUser() {
        return user;
    }
}
