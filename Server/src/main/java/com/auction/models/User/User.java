package com.auction.models.User;
import at.favre.lib.crypto.bcrypt.BCrypt;
import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;
import com.auction.models.Entity.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;


public abstract class User extends Entity {
    private String username;
    private String password; // Chuỗi Hash
    private String email;
    private double balance;

    private UserRole role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor 1: Tạo mới (Đăng ký)
    protected User(String username, String email, String hashedPassword, UserRole role) {
        super();
        this.username = username;
        this.email = email;
        this.password = hashedPassword;
        this.role = role;
        this.balance = 0; // Khởi tạo tiền = 0
        this.status = UserStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Constructor 2: Load từ DB
    protected User(String id, String username, String email, String password,
                   UserRole role, double balance, UserStatus status,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id);
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.balance = balance;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public boolean checkPassword(String plainPasswordInput) {
        BCrypt.Result result = BCrypt.verifyer().verify(plainPasswordInput.toCharArray(), this.password);
        return result.verified;
    }

    // Nạp tiền (Dùng add và compareTo)
    public synchronized boolean addBalance(double amount) {
        // Kiểm tra amount > 0
        if (amount > 0) {
            this.balance += amount;
            return true;
        }
        return false;
    }

    // Trừ tiền khi đặt giá
    public synchronized boolean deductBalance(double amount) {

        // this.balance >= amount
        if (this.balance >= amount) {
            this.balance -= amount;
            return true;
        }
        return false;
    }

    // Getters
    public double getBalance() { return balance; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public String getRole(){
        return this.role.toString();
    }

    public com.auction.enums.UserRole getUserRole(){return this.role;}

    public String getUsername() {
        return this.username;
    }

    public String getEmail() {
        return this.email;
    }
}
