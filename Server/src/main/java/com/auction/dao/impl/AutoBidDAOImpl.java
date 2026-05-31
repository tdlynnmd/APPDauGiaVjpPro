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
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE max_bid = VALUES(max_bid), increment_amount = VALUES(increment_amount), is_active = VALUES(is_active)";

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
        String sql = "SELECT * FROM auto_bids WHERE user_id = ? AND auction_id = ? AND is_active = 1";
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
        String sql = "SELECT * FROM auto_bids WHERE auction_id = ? AND is_active = 1";
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
        String sql = "UPDATE auto_bids SET is_active = 0 WHERE id = ?";
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
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();

        return new AutoBid(id, userId, auctionId, maxBid, increment, isActive, createdAt);
    }
}
