package com.auction.dto;

import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;

/**
 * DTO đại diện cho thông tin tài khoản người bán sản phẩm (Seller).
 */
public class SellerDTO extends UserDTO {
    private static final long serialVersionUID = 1L;

    private double rating;

    public SellerDTO(String id, String username, String email, UserRole role, UserStatus status,
                     double availableBalance, double frozenBalance, double rating) {
        super(id, username, email, role, status, availableBalance, frozenBalance);
        this.rating = rating;
    }

    public double getRating() { return rating; }

    public void setRating(double rating) { this.rating = rating; }
}