package com.auction.dto;


import com.auction.enums.UserRole;

import java.io.Serializable;
import java.util.List;

/**
 * Data Transfer Object cho Admin
 * Chứa thông tin cơ bản của quản trị viên
 * Không chứa thông tin nhạy cảm như mật khẩu hoặc lịch sử hoạt động chi tiết
 */
public class AdminDTO extends UserDTO{
    private final List<String> actionLogs;

    public AdminDTO(String id, String username, String email, UserRole role, List<String> actionLogs) {
        super(id, username, email, role);
        this.actionLogs = actionLogs;
    }

    public List<String> getActionLogs() {
        return actionLogs;
    }

}

