package com.auction.dto;

import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;

/**
 * DTO đại diện cho thông tin tài khoản Admin hệ thống.
 */
public class AdminDTO extends UserDTO {
    private static final long serialVersionUID = 1L;

    public AdminDTO(String id, String username, String email, UserRole role, UserStatus status) {
        super(id, username, email, role, status, 0.0, 0.0);
    }

}