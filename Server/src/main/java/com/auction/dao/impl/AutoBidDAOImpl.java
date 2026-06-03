package com.auction.dao.impl;

import com.auction.dao.AutoBidDAO;
import com.auction.models.Auction.AutoBid;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AutoBidDAOImpl implements AutoBidDAO {

    @Override
    public boolean insertOrUpdate(Connection conn, AutoBid autoBid) throws SQLException {
        String sql = "INSERT INTO auto_bids (id, user_id, auction_id, max_bid, increment_amount, is_active, created_at) " +
                "VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE id = VALUES(id), max_bid = VALUES(max_bid), increment_amount = VALUES(increment_amount), " +
                "is_active = VALUES(is_active), created_at = VALUES(created_at)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, autoBid.getId());
            stmt.setString(2, autoBid.getUserId());
            stmt.setString(3, autoBid.getAuctionId());
            stmt.setDouble(4, autoBid.getMaxBid());
            stmt.setDouble(5, autoBid.getIncrement());
            stmt.setBoolean(6, autoBid.isActive());
            stmt.setTimestamp(7, Timestamp.valueOf(autoBid.getCreatedAt()));

            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public Optional<AutoBid> findActiveByUserAndAuction(Connection conn, String userId, String auctionId) throws SQLException {
        // Tìm BẤT KỲ bản ghi AutoBid nào (kể cả inactive) để tái sử dụng ID cũ khi kích hoạt lại
        // Tránh lỗi duplicate entry trên ràng buộc unique_user_auction
        String sql = "SELECT BIN_TO_UUID(id, 1) AS id, BIN_TO_UUID(user_id, 1) AS user_id, BIN_TO_UUID(auction_id, 1) AS auction_id, max_bid, increment_amount, is_active, created_at FROM auto_bids WHERE user_id = UUID_TO_BIN(?, 1) AND auction_id = UUID_TO_BIN(?, 1) ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAutoBid(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<AutoBid> findActiveByAuctionId(Connection conn, String auctionId) throws SQLException {
        List<AutoBid> list = new ArrayList<>();
        String sql = "SELECT BIN_TO_UUID(id, 1) AS id, BIN_TO_UUID(user_id, 1) AS user_id, BIN_TO_UUID(auction_id, 1) AS auction_id, max_bid, increment_amount, is_active, created_at FROM auto_bids WHERE auction_id = UUID_TO_BIN(?, 1) AND is_active = 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAutoBid(rs));
                }
            }
        }
        return list;
    }

    @Override
    public boolean disableAutoBid(Connection conn, String id) throws SQLException {
        String sql = "UPDATE auto_bids SET is_active = 0 WHERE id = UUID_TO_BIN(?, 1)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    private AutoBid mapResultSetToAutoBid(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String userId = rs.getString("user_id");
        String auctionId = rs.getString("auction_id");
        double maxBid = rs.getDouble("max_bid");
        double increment = rs.getDouble("increment_amount");
        boolean isActive = rs.getBoolean("is_active");
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        LocalDateTime createdAt = createdAtTs != null ? createdAtTs.toLocalDateTime() : null;

        return new AutoBid(id, userId, auctionId, maxBid, increment, isActive, createdAt);
    }
}
