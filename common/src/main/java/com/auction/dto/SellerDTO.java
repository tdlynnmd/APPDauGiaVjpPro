package com.auction.dto;

import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;

public class SellerDTO extends UserDTO {
    private static final long serialVersionUID = 1L;

    private double rating; // Hiển thị số sao/uy tín

    public SellerDTO(String id, String username, String email, UserRole role, UserStatus status,
                     double availableBalance, double frozenBalance, double rating) {
        // Truyền 2 trường số dư lên lớp cha UserDTO
        super(id, username, email, role, status, availableBalance, frozenBalance);
        this.rating = rating;
    }

    // Getters
    public double getRating() { return rating; }

    // Setters
    public void setRating(double rating) { this.rating = rating; }
}