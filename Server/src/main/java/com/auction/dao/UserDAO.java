package com.auction.dao;

import com.auction.models.User.User;
import java.math.BigDecimal;
import java.util.Optional;

public interface UserDAO {
    // Dùng Optional để chống lỗi NullPointerException (Best practice)
    Optional<User> findById(String id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    boolean insertUser(User user);

    // --- 4 Hàm Nghiệp Vụ Tài Chính Quan Trọng ---

    // 1. Đóng băng tiền (Từ Available -> Frozen)
    boolean freezeMoney(String userId, double amount);

    // 2. Giải phóng tiền (Từ Frozen -> Available)
    boolean unfreezeMoney(String userId, double amount);

    // 3. Khấu trừ tiền thắng cuộc (Trừ hẳn khỏi Frozen)
    boolean deductFrozenMoney(String userId, double amount);

    // 4. Nạp tiền/Nhận tiền (Cộng vào Available)
    boolean addAvailableBalance(String userId, double amount);

}