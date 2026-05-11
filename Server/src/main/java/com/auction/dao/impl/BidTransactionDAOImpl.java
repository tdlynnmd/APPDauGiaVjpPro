package com.auction.dao.impl;

import com.auction.config.DatabaseConnection;
import com.auction.dao.BidTransactionDAO;
import com.auction.models.Auction.BidTransaction;
import java.sql.*;

public class BidTransactionDAOImpl implements BidTransactionDAO { // Triển khai interface tương ứng

    @Override
    public boolean insertBid(BidTransaction bid) {
        String sql = "INSERT INTO bid_transactions (id, bidder_id, auction_id, amount, status, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, bid.getId());
            stmt.setString(2, bid.getBidderId());
            stmt.setString(3, bid.getAuctionId());
            stmt.setDouble(4, bid.getAmount());
            stmt.setString(5, bid.getStatus().name()); // ACCEPTED, REJECTED...
            stmt.setTimestamp(6, Timestamp.valueOf(bid.getTime())); // Ánh xạ LocalDateTime

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi lưu biên lai Bid: " + e.getMessage());
            return false;
        }
    }

    // Hàm lấy lịch sử bid cho UI JavaFX (Có phân trang)
    // SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?
}