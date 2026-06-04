package com.auction.dto;

import com.auction.enums.UserRole;

/**
 * DTO gửi yêu cầu đăng ký tài khoản mới.
 */
public class RegisterRequest {
    private String username;
    private String password;
    private String email;

    private UserRole role;

    public RegisterRequest() {
    }

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
