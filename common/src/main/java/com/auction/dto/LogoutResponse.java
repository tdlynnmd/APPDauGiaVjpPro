package com.auction.dto;

public class LogoutResponse {
    /**
     LogoutResponse là DTO phản hồi kết quả đăng xuất từ Server về Client.
     Cho Client biết Server đã xử lý logout thành công hay chưa
     Mang message để Dshboard hiển thị nếu có lỗi
     */
    private boolean success;    // true nếu Server xử lý logout thành công.
    private String message;     // Thông báo trả về cho Client.
    private String errorCode;   // Mã lỗi kỹ thuật nếu logout thất bại.

    public LogoutResponse(boolean success, String message, String errorCode) {
        this.success = success;
        this.message = message;
        this.errorCode = errorCode;
    }

    /**
     Response thành công khi Server đã xóa connection/session.
     */
    public static LogoutResponse success() {
        return new LogoutResponse(true, "Đăng xuất thành công", null);
    }

    /**
     * Response thất bại khi userId không hợp lệ hoặc có lỗi Server.
     */
    public static LogoutResponse failure(String message, String errorCode) {
        return new LogoutResponse(false, message, errorCode);
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
}
