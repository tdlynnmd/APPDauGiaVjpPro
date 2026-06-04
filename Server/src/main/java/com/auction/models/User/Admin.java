package com.auction.models.User;

import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lớp biểu diễn thực thể Admin trong hệ thống.
 */
public class Admin extends User {

    public Admin(String username, String email, String password){
        super(username, email, password, UserRole.ADMIN);
    }

    public Admin(String id, String username, String email, String password,
                 UserRole role, double availableBalance, double frozenBalance, UserStatus status,
                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id, username, email, password, role, availableBalance, frozenBalance, status, createdAt, updatedAt);
    }

}