package com.auction.dto;

/**
 * DTO chứa kết quả trả về sau khi đăng nhập thành công bao gồm token/session và thông tin User.
 */
public class LoginResultDTO {
    private String token;
    private UserDTO user;

    public LoginResultDTO() {
    }

    public LoginResultDTO(String token, UserDTO user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public UserDTO getUser() {
        return user;
    }
}