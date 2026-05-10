package com.auction.dto;

import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;
import java.math.BigDecimal;

public class SellerDTO extends UserDTO {
    private double balance; // Seller cũng cần xem tiền
    private double rating;      // Hiển thị số sao/uy tín

    public SellerDTO(String id, String username, String email, UserRole role, UserStatus status,
                     double balance, double rating) {
        super(id, username, email, role, status);
        this.balance = balance;
        this.rating = rating;
    }

    // Getters
    public double getBalance() { return balance; }
    public double getRating() { return rating; }

    // Setters (Chỉ dùng để gán data thuần túy)
    public void setBalance(double balance) { this.balance = balance; }
    public void setRating(double rating) { this.rating = rating; }

    // ĐÃ XÓA hàm updateRating() có chứa phép toán chia trung bình
}