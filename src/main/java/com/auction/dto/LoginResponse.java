package com.auction.dto;

import java.io.Serializable;

public class LoginResponse{
    private boolean success;
    private String message;
    private String errorCode;
    private String token;
    private UserDTO user;

    public LoginResponse(boolean success, String message, String errorCode, String token, UserDTO user) {
        this.success = success;     /** Cho biet login thành công hay thất bại */
        this.message = message;     /** Chuứa thông báo để hiển thị cho người dùng*/
        this.errorCode = errorCode; /** Chứa mã loi ki thuat để code xử lí hoặc debug */
        this.token = token;         /** Sau khi dang nhap thanh cong, Sever tạo token và gửi về Client.
                                        Sau này, khi Client muốn đặt giá, thêm sản phẩm, .. nó gửi token kem request cho Sever biết*/
        this.user = user;           /** Chứa thông tin user an toàn để gui ve client */
    }

    public static LoginResponse success(UserDTO user, String token) {
        return new LoginResponse(true, "Đăng nhập thành công", null, token, user);
    }

    public static LoginResponse failure(String message, String errorCode) {
        return new LoginResponse(false, message, errorCode, null, null);
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

    public String getToken() {
        return token;
    }

    public UserDTO getUser() {
        return user;
    }
}
