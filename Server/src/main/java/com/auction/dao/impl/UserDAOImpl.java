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
        String sql = "INSERT INTO users (id, user_type, username, email, password_hash, balance, status) VALUES (?, ?, ?, ?, ?, ?, ?)";

        // try-with-resources: Tự động đóng connection sau khi chạy xong
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getId());
            stmt.setString(2, user.getUserRole().name()); // Lấy ENUM thành String
            stmt.setString(3, user.getUsername());
            stmt.setString(4, user.getEmail());
            // Cần tạo hàm getHashedPassword() trong lớp User
            stmt.setString(5, user.getPassword());
            stmt.setDouble(6, user.getBalance());
            stmt.setString(7, user.getStatus().name());

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

    @Override
    public boolean updateBalance(String userId, double newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE id = ? AND deleted_at IS NULL";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, newBalance);
            stmt.setString(2, userId);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi Cập nhật tiền: " + e.getMessage());
            return false;
        }
    }

    /**
     * Hàm tiện ích (Helper method) để tái sử dụng logic Map DB -> Object
     * Đây là nơi thể hiện sức mạnh của Single Table Inheritance
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String username = rs.getString("username");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");

        double balance = rs.getDouble("balance");
        // Balance có DEFAULT 0 nên cũng không lo bị NULL

        UserRole role = UserRole.valueOf(rs.getString("user_type"));
        UserStatus status = UserStatus.valueOf(rs.getString("status"));

        java.time.LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        java.time.LocalDateTime updatedAt = rs.getTimestamp("updated_at").toLocalDateTime();

        // Tái tạo Object dựa trên ROLE
        return switch (role) {
            case BIDDER -> new Bidder(id, username, email, passwordHash, role, balance, status, createdAt, updatedAt);

            case SELLER -> {
                double rating = rs.getDouble("rating");
                // 🔥 SỬA LỖI Ở ĐÂY: Kiểm tra NULL
                if (rs.wasNull()) {
                    rating = -1.0; // Quy ước -1.0 nghĩa là "Chưa có đánh giá"
                }
                yield new Seller(id, username, email, passwordHash, role, balance, status, createdAt, updatedAt, rating);
            }

            case ADMIN -> new Admin(id, username, email, passwordHash, role, balance, status, createdAt, updatedAt);
        };
    }
}