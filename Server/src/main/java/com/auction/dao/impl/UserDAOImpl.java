package com.auction.dao.impl;

import com.auction.config.DatabaseConnection;
import com.auction.dao.UserDAO;
import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;
import com.auction.models.User.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserDAOImpl implements UserDAO {

    // 🔥 SỬA: Nhận Connection từ ngoài truyền vào và ném SQLException lên Service điều phối
    @Override
    public boolean insertUser(Connection conn, User user) throws SQLException {
        String sql = "INSERT INTO users (id, user_type, username, email, password_hash, available_balance, frozen_balance, status) VALUES (UUID_TO_BIN(?, 1), ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getId());
            stmt.setString(2, user.getUserRole().name());
            stmt.setString(3, user.getUsername());
            stmt.setString(4, user.getEmail());
            stmt.setString(5, user.getPassword());
            stmt.setDouble(6, user.getAvailableBalance());
            stmt.setDouble(7, user.getFrozenBalance());
            stmt.setString(8, user.getStatus().name());

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Hàm ĐỌC (SELECT) độc lập, tự mở connection nên giữ lại try-catch cục bộ an toàn
     */
    @Override
    public Optional<User> findById(String id) {
        String sql = "SELECT BIN_TO_UUID(id, 1) AS id, user_type, username, email, password_hash, available_balance, frozen_balance, status, rating, created_at, updated_at FROM users WHERE id = UUID_TO_BIN(?, 1) AND deleted_at IS NULL";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi Find By ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Hàm ĐỌC (SELECT) độc lập, tự mở connection nên giữ lại try-catch cục bộ an toàn
     */
    @Override
    public java.util.Map<String, String> findUsernamesByIds(List<String> ids) {
        java.util.Map<String, String> map = new java.util.HashMap<>();

        if (ids == null || ids.isEmpty()) {
            return map;
        }

        String placeholders = ids.stream()
                .map(id -> "UUID_TO_BIN(?, 1)")
                .collect(Collectors.joining(", "));

        String sql = "SELECT BIN_TO_UUID(id, 1) AS id, username FROM users WHERE id IN (" + placeholders + ") AND deleted_at IS NULL";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < ids.size(); i++) {
                stmt.setString(i + 1, ids.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String userId = rs.getString("id");
                    String username = rs.getString("username");
                    map.put(userId, username);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi Batch Fetching Usernames tại UserDAOImpl: " + e.getMessage());
        }
        return map;
    }

    /**
     * Hàm ĐỌC (SELECT) độc lập, tự mở connection nên giữ lại try-catch cục bộ an toàn
     */
    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT BIN_TO_UUID(id, 1) AS id, user_type, username, email, password_hash, available_balance, frozen_balance, status, rating, created_at, updated_at FROM users WHERE username = ? AND deleted_at IS NULL";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi Find By Username: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Hàm ĐỌC (SELECT) độc lập, tự mở connection nên giữ lại try-catch cục bộ an toàn
     */
    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT BIN_TO_UUID(id, 1) AS id, user_type, username, email, password_hash, available_balance, frozen_balance, status, rating, created_at, updated_at FROM users WHERE email = ? AND deleted_at IS NULL";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi Find By Email: " + e.getMessage());
        }
        return Optional.empty();
    }

    // --- CÁC HÀM QUẢN LÝ TIỀN CHỐNG RACE CONDITION (ĐÃ THROWS SẴN) ---

    @Override
    public boolean freezeMoney(Connection conn, String userId, double amount) throws SQLException {
        String sql = "UPDATE users SET available_balance = available_balance - ?, " +
                "frozen_balance = frozen_balance + ? " +
                "WHERE id = UUID_TO_BIN(?, 1) AND available_balance >= ? AND deleted_at IS NULL";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setDouble(2, amount);
            stmt.setString(3, userId);
            stmt.setDouble(4, amount);

            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public void unfreezeMoney(Connection conn, String userId, double amount) throws SQLException {
        String sql = "UPDATE users SET available_balance = available_balance + ?, " +
                "frozen_balance = frozen_balance - ? " +
                "WHERE id = UUID_TO_BIN(?, 1) AND frozen_balance >= ? AND deleted_at IS NULL";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setDouble(2, amount);
            stmt.setString(3, userId);
            stmt.setDouble(4, amount);

            stmt.executeUpdate();
        }
    }

    @Override
    public boolean deductFrozenMoney(Connection conn, String userId, double amount) throws SQLException {
        String sql = "UPDATE users SET frozen_balance = frozen_balance - ? " +
                "WHERE id = UUID_TO_BIN(?, 1) AND frozen_balance >= ? AND deleted_at IS NULL";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setString(2, userId);
            stmt.setDouble(3, amount);

            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean addAvailableBalance(Connection conn, String userId, double amount) throws SQLException {
        String sql = "UPDATE users SET available_balance = available_balance + ? " +
                "WHERE id = UUID_TO_BIN(?, 1) AND deleted_at IS NULL";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setString(2, userId);

            return stmt.executeUpdate() > 0;
        }
    }

    // 🔥 SỬA: Nhận Connection từ ngoài truyền vào để bọc vào Transaction quản lý tài chính chung, ném SQLException lên Service
    @Override
    public boolean withdrawAvailableBalance(Connection conn, String userId, double amount) throws SQLException {
        String sql = "UPDATE users SET available_balance = available_balance - ? " +
                "WHERE id = UUID_TO_BIN(?, 1) AND available_balance >= ? AND deleted_at IS NULL";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setString(2, userId);
            stmt.setDouble(3, amount);

            return stmt.executeUpdate() > 0;
        }
    }

    // 🔥 SỬA: Loại bỏ khối try-catch, ném SQLException ra ngoài Service bọc lót Transaction
    @Override
    public boolean addJoinedAuction(Connection conn, String userId, String auctionId) throws SQLException {
        String sql = "INSERT IGNORE INTO bidder_joined_auctions (user_id, auction_id) VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1))";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, auctionId);

            stmt.executeUpdate();
            return true;
        }
    }

    // 🔥 SỬA: Nhận Connection từ ngoài truyền vào, loại bỏ khối try-catch, ném SQLException lên trên
    @Override
    public void removeJoinedAuction(Connection conn, String userId, String auctionId) throws SQLException {
        String sql = "DELETE FROM bidder_joined_auctions WHERE user_id = UUID_TO_BIN(?, 1) AND auction_id = UUID_TO_BIN(?, 1)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, auctionId);
            stmt.executeUpdate();
        }
    }

    /**
     * Hàm ĐỌC (SELECT) độc lập, tự mở connection nên giữ lại try-catch cục bộ an toàn
     */
    @Override
    public List<User> findPaginated(int limit, int offset) {
        String sql = "SELECT BIN_TO_UUID(id, 1) AS id, user_type, username, email, password_hash, available_balance, frozen_balance, status, rating, created_at, updated_at FROM users WHERE deleted_at IS NULL LIMIT ? OFFSET ?";
        List<User> users = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            stmt.setInt(2, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi tải danh sách phân trang: " + e.getMessage());
        }
        return users;
    }

    /**
     * Đếm tổng số người dùng trong hệ thống
     */
    @Override
    public long countTotalUsers() {
        String sql = "SELECT COUNT(*) FROM users WHERE deleted_at IS NULL";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi đếm tổng số người dùng: " + e.getMessage());
        }
        return 0;
    }

    // 🔥 SỬA: Nhận Connection từ ngoài truyền vào (để bọc lót Transaction cưỡng chế Kick/Ban từ Admin), ném SQLException ra ngoài
    @Override
    public boolean updateStatus(Connection conn, String userId, String name) throws SQLException {
        String sql = "UPDATE users SET status = ? WHERE id = UUID_TO_BIN(?, 1) AND deleted_at IS NULL";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, userId);

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Helper Method: Ánh xạ dữ liệu từ SQL sang Object Java
     * Hàm này giữ nguyên throws外 lý sẵn rất sạch sẽ
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String username = rs.getString("username");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");

        double available = rs.getDouble("available_balance");
        double frozen = rs.getDouble("frozen_balance");

        UserRole role = UserRole.valueOf(rs.getString("user_type"));
        UserStatus status = UserStatus.valueOf(rs.getString("status"));

        Timestamp createdAtTs = rs.getTimestamp("created_at");
        java.time.LocalDateTime createdAt = createdAtTs != null ? createdAtTs.toLocalDateTime() : null;
        Timestamp updatedAtTs = rs.getTimestamp("updated_at");
        java.time.LocalDateTime updatedAt = updatedAtTs != null ? updatedAtTs.toLocalDateTime() : null;

        switch (role) {
            case BIDDER:
                Bidder bidder = new Bidder(id, username, email, passwordHash, role, available, frozen, status, createdAt, updatedAt);
                loadJoinedAuctionsForBidder(rs.getStatement().getConnection(), bidder);
                return bidder;

            case SELLER:
                double rating = rs.getDouble("rating");
                if (rs.wasNull()) rating = -1.0;
                return new Seller(id, username, email, passwordHash, role, available, frozen, status, createdAt, updatedAt, rating);

            case ADMIN:
                return new Admin(id, username, email, passwordHash, role, available, frozen, status, createdAt, updatedAt);

            default:
                throw new SQLException("Unsupported user role: " + role);
        }
    }

    private void loadJoinedAuctionsForBidder(Connection conn, Bidder bidder) {
        String sql = "SELECT BIN_TO_UUID(auction_id, 1) AS auction_id FROM bidder_joined_auctions WHERE user_id = UUID_TO_BIN(?, 1)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, bidder.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    bidder.addJoinedAuction(rs.getString("auction_id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi loadJoinedAuctionsForBidder: " + e.getMessage());
        }
    }
}