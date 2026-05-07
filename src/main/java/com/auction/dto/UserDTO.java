package com.auction.dto;
import com.auction.server.models.User.UserRole;

import java.io.Serializable;

public class UserDTO {
    private String id;
    private String username;
    private String email;
    private UserRole role;

    public UserDTO(String id, String username, String email, UserRole role){
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getId() {
        return id;
    }

    public UserRole getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}