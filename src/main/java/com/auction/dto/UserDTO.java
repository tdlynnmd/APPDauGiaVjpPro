package com.auction.dto;
import com.auction.server.models.User.UserRole;

public class UserDTO{
    private String username;
    private UserRole role;

    public UserDTO(String username, UserRole role){
        this.username = username;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}