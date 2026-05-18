package com.auction.dto;

/**
 * LoginResultDTO là dữ liệu trả về khi LOGIN thành công.
   Do là LOGIN khi thành công cần trả về thêm cả token và user, 2 dữ liệu này đi cùng nhau, nên cần DTO để gom lại
 *
 * Lưu ý:
 * - Class này không chứa success/message/errorCode.
 * - Các thông tin đó thuộc về SocketResponse.
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