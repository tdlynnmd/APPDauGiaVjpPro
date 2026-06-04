package com.auction.dto;

import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;

import java.io.Serializable;

/**
 * DTO cơ sở đại diện cho thông tin tài khoản người dùng chung trong hệ thống.
 */
public class UserDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String username;
    private String email;
    private UserRole role;
    private UserStatus status;
    private double availableBalance;
    private double frozenBalance;

    public UserDTO(String id, String username, String email, UserRole role, UserStatus status, double availableBalance, double frozenBalance) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.status = status;
        this.availableBalance = availableBalance;
        this.frozenBalance = frozenBalance;
    }

    public double getAvailableBalance() { return availableBalance; }
    public double getFrozenBalance() { return frozenBalance; }
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public UserRole getRole() { return role; }
    public UserStatus getStatus() { return status; }

    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(UserRole role) { this.role = role; }
    public void setStatus(UserStatus status) { this.status = status; }
    public void setAvailableBalance(double availableBalance) { this.availableBalance = availableBalance; }
    public void setFrozenBalance(double frozenBalance) { this.frozenBalance = frozenBalance; }
}