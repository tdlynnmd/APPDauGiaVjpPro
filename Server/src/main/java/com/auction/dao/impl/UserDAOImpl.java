package com.auction.dao.impl;

import com.auction.config.DatabaseConnection;
import com.auction.dao.UserDAO;
import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;
import com.auction.models.User.*;


import java.sql.*;
import java.util.Optional;

public class UserDAOImpl implements UserDAO {

    @Override
    public boolean insertUser(User user) {
        // Dùng INSERT IGNORE hoặc kiểm tra trùng ở Service
        String sql = "INSERT INTO users (id, user_type, username, email, password_hash, available_balance, frozen_balance, status) VALUES (?, ?, ?, ?, ?, ?, ?,?)";

        // try-with-resources: Tự động đóng connection sau khi chạy xong
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getId());
            stmt.setString(2, user.getUserRole().name()); // Lấy ENUM thành String
            stmt.setString(3, user.getUsername());
            stmt.setString(4, user.getEmail());
            // Cần tạo hàm getHashedPassword() trong lớp User
            stmt.setString(5, user.getPassword());
            stmt.setDouble(6, user.getAvailableBalance());
            stmt.setDouble(7, user.getFrozenBalance());
            stmt.setString(8, user.getStatus().name());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi Insert User: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Optional<User> findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ? AND deleted_at IS NULL";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi Find By ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ? AND deleted_at IS NULL";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi Find By Username: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ? AND deleted_at IS NULL";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi Find By Email: " + e.getMessage());
        }
        return Optional.empty();
    }

    // --- CÁC HÀM QUẢN LÝ TIỀN CHỐNG RACE CONDITION ---

    /**
     * Đóng băng tiền: Trừ khả dụng, cộng vào đóng băng.
     * Chỉ thực hiện nếu số dư khả dụng ĐỦ (WHERE available_balance >= amount)
     */
    public boolean freezeMoney(String userId, double amount) {
        String sql = "UPDATE users SET available_balance = available_balance - ?, " +
                "frozen_balance = frozen_balance + ? " +
                "WHERE id = ? AND available_balance >= ? AND deleted_at IS NULL";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setDouble(2, amount);
            stmt.setString(3, userId);
            stmt.setDouble(4, amount);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Giải phóng tiền: Trả tiền từ đóng băng về lại khả dụng (khi bị outbid)
     */
    public boolean unfreezeMoney(String userId, double amount) {
        String sql = "UPDATE users SET available_balance = available_balance + ?, " +
                "frozen_balance = frozen_balance - ? " +
                "WHERE id = ? AND frozen_balance >= ? AND deleted_at IS NULL";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setDouble(2, amount);
            stmt.setString(3, userId);
            stmt.setDouble(4, amount);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Khấu trừ tiền: Xóa hẳn tiền trong cột đóng băng (Khi thắng đấu giá)
     */
    public boolean deductFrozenMoney(String userId, double amount) {
        String sql = "UPDATE users SET frozen_balance = frozen_balance - ? " +
                "WHERE id = ? AND frozen_balance >= ? AND deleted_at IS NULL";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setString(2, userId);
            stmt.setDouble(3, amount);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Nạp tiền: Cộng thẳng vào khả dụng
     */
    public boolean addAvailableBalance(String userId, double amount) {
        String sql = "UPDATE users SET available_balance = available_balance + ? " +
                "WHERE id = ? AND deleted_at IS NULL";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setString(2, userId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    // --- CÁC HÀM TRUY VẤN ---

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String username = rs.getString("username");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");

        // Đọc 2 cột mới
        double available = rs.getDouble("available_balance");
        double frozen = rs.getDouble("frozen_balance");

        UserRole role = UserRole.valueOf(rs.getString("user_type"));
        UserStatus status = UserStatus.valueOf(rs.getString("status"));

        java.time.LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        java.time.LocalDateTime updatedAt = rs.getTimestamp("updated_at").toLocalDateTime();

        return switch (role) {
            case BIDDER -> new Bidder(id, username, email, passwordHash, role, available, frozen, status, createdAt, updatedAt);
            case SELLER -> {
                double rating = rs.getDouble("rating");
                if (rs.wasNull()) rating = -1.0;
                yield new Seller(id, username, email, passwordHash, role, available, frozen, status, createdAt, updatedAt, rating);
            }
            case ADMIN -> new Admin(id, username, email, passwordHash, role, available, frozen, status, createdAt, updatedAt);
        };
    }
}