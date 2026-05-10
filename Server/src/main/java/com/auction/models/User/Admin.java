package com.auction.models.User;

import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Admin extends User {

    // Constructor 1: Mới
    public Admin(String username, String email, String password){
        super(username, email, password, UserRole.ADMIN);
    }

    // Constructor 2: Load từ DB
    protected Admin(String id, String username, String email, String password,
                    UserRole role, double balance, UserStatus status,
                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id, username, email, password, role, balance, status, createdAt, updatedAt);
    }

    // XÓA BỎ actionLogs ở đây.
    // Việc ghi Log (Ví dụ: "Admin A xóa phiên 001") sẽ do AdminService lo.
    // AdminService sẽ gọi LogDAO.insert(adminId, actionDetail, time);
}