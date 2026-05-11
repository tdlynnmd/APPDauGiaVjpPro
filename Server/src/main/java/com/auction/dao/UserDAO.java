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

    // Hàm cập nhật tiền riêng biệt để tối ưu hóa, thay vì update toàn bộ User
    boolean updateBalance(String userId, double newBalance);
}