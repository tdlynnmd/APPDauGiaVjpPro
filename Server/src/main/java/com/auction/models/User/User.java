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

    // Tách đôi số dư
    private double availableBalance;
    private double frozenBalance;

    private UserRole role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor 1: Đăng ký mới
    protected User(String username, String email, String hashedPassword, UserRole role) {
        super();
        this.username = username;
        this.email = email;
        this.password = hashedPassword;
        this.role = role;
        this.availableBalance = 0;
        this.frozenBalance = 0;
        this.status = UserStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Constructor 2: Load từ DB
    protected User(String id, String username, String email, String password,
                   UserRole role, double availableBalance, double frozenBalance,
                   UserStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id);
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.availableBalance = availableBalance;
        this.frozenBalance = frozenBalance;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }


    public boolean checkPassword(String plainPasswordInput) {
        BCrypt.Result result = BCrypt.verifyer().verify(plainPasswordInput.toCharArray(), this.password);
        return result.verified;
    }

    // --- LOGIC NGHIỆP VỤ TRÊN RAM ---

    public synchronized void addAvailableBalance(double amount) {
        if (amount > 0) this.availableBalance += amount;
    }

    // Đóng băng tiền: Chuyển từ Khả dụng sang Đóng băng
    public synchronized boolean freeze(double amount) {
        if (this.availableBalance >= amount) {
            this.availableBalance -= amount;
            this.frozenBalance += amount;
            return true;
        }
        return false;
    }

    // Giải phóng tiền: Chuyển từ Đóng băng về lại Khả dụng
    public synchronized void unfreeze(double amount) {
        if (this.frozenBalance >= amount) {
            this.frozenBalance -= amount;
            this.availableBalance += amount;
        }
    }

    // Khấu trừ tiền: Trừ thẳng từ ví đóng băng (Khi thắng đấu giá)
    public synchronized void deductFrozen(double amount) {
        if (this.frozenBalance >= amount) {
            this.frozenBalance -= amount;
        }
    }

    // Getters
    public double getAvailableBalance() { return availableBalance; }
    public double getFrozenBalance() { return frozenBalance; }
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

    public String getPassword() {
        return this.password;
    }
}
