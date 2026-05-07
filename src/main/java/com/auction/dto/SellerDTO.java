package com.auction.dto;

import com.auction.server.models.User.UserRole;

import java.io.Serializable;

/**
 * Data Transfer Object cho Seller
 * Chứa thông tin cơ bản của người bán và điểm uy tín của họ
 * Không chứa thông tin nhạy cảm như mật khẩu
 */
public class SellerDTO extends UserDTO{
    private double rating;

    public SellerDTO(String id, String username, String email, UserRole role, double rating) {
        super(id, username, email, role);
        this.rating = rating;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public void updateRating(double newRating) {
        this.rating = (this.rating + newRating) / 2.0;
    }
}

