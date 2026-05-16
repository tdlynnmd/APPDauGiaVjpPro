package com.auction.dto;

import com.auction.enums.UserRole;
import com.auction.enums.UserStatus; // Bổ sung Status

import java.io.Serializable;

public class UserDTO implements Serializable {
    private static final long serialVersionUID = 1L; // Đảm bảo an toàn khi truyền qua mạng

    private String id;
    private String username;
    private String email;
    private UserRole role;
    private UserStatus status;
    // Thêm 2 trường này
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

    // Getters
    public double getAvailableBalance() { return availableBalance; }
    public double getFrozenBalance() { return frozenBalance; }
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public UserRole getRole() { return role; }
    public UserStatus getStatus() { return status; }

    // Setters
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(UserRole role) { this.role = role; }
    public void setStatus(UserStatus status) { this.status = status; }
}