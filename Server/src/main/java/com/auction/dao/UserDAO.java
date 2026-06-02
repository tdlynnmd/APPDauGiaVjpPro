package com.auction.dao;

import com.auction.models.User.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface UserDAO {
    // 🔥 SỬA: Nhận Connection từ ngoài truyền vào và ném SQLException lên Service điều phối
    boolean insertUser(Connection conn, User user) throws SQLException;

    // Dùng Optional để chống lỗi NullPointerException (Best practice)
    Optional<User> findById(String id);

    java.util.Map<String, String> findUsernamesByIds(List<String> ids);

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);


    // --- 4 Hàm Nghiệp Vụ Tài Chính Quan Trọng ---

    // 1. Đóng băng tiền (Từ Available -> Frozen)
    boolean freezeMoney(Connection conn, String userId, double amount) throws SQLException;

    // 2. Giải phóng tiền (Từ Frozen -> Available)
    void unfreezeMoney(Connection conn, String userId, double amount) throws SQLException;

    // 3. Khấu trừ tiền thắng cuộc (Trừ hẳn khỏi Frozen)
    boolean deductFrozenMoney(Connection conn, String userId, double amount) throws SQLException;

    // 4. Nạp tiền/Nhận tiền (Cộng vào Available)
    boolean addAvailableBalance(Connection conn, String userId, double amount) throws SQLException;


    // 🔥 SỬA: Nhận Connection từ ngoài truyền vào để bọc vào Transaction quản lý tài chính chung, ném SQLException lên Service
    boolean withdrawAvailableBalance(Connection conn, String userId, double amount) throws SQLException;

    boolean addJoinedAuction(Connection conn, String userId, String auctionId) throws SQLException;


    // 🔥 SỬA: Nhận Connection từ ngoài truyền vào, loại bỏ khối try-catch, ném SQLException lên trên
    void removeJoinedAuction(Connection conn, String userId, String auctionId) throws SQLException;

    List<User> findPaginated(int limit, int offset);

    /**
     * Đếm tổng số người dùng trong hệ thống
     */
    long countTotalUsers();

    // 🔥 SỬA: Nhận Connection từ ngoài truyền vào (để bọc lót Transaction cưỡng chế Kick/Ban từ Admin), ném SQLException ra ngoài
    boolean updateStatus(Connection conn, String userId, String name) throws SQLException;

    boolean updateProfile(Connection conn, String userId, String username, String email) throws SQLException;

    boolean updatePassword(Connection conn, String userId, String hashedPassword) throws SQLException;
}