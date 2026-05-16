package com.auction.dto;
/**
    Chứa dữ liệu người dùng nhập khi đăng kí
    Được Client bọc vào SocketRequest với action "REGISTER"
    Sever đọc object này trong RequestDispatcher rồi chuyển tiếp cho AuthController
 */

import com.auction.enums.UserRole;

/**
 Luồng sử dụng:
 * RegisterController
 * -> clientAuthApi.register()
 * -> SocketRequest(action = "REGISTER", body = RegisterRequest)
 * -> Server RequestDispatcher
 * -> AuthController.register()
 */
public class RegisterRequest {
    private String username;
    private String password;
    private String email;

    // Vai trò tài khoản: BIDDER hoặc SELLER.
    // ADMIN không nên cho đăng ký tự do từ giao diện client.
    private UserRole role;

    public RegisterRequest() {      // Constructor rỗng cần cho Gson
                                    // Khi Server nhận JSON, Gson cần constructor rỗng  tạo object
    }

    /**
     * Constructor dùng khi Client tạo request đăng ký.
     */
    public RegisterRequest(String username, String password, String email, UserRole role) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
}
}
