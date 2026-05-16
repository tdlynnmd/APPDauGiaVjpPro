package com.auction.dao.impl;

import com.auction.config.DatabaseConnection;
import com.auction.dao.BidTransactionDAO;
import com.auction.enums.BidStatus;
import com.auction.models.Auction.BidTransaction;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BidTransactionDAOImpl implements BidTransactionDAO {

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
            stmt.setString(5, bid.getStatus().name());
            stmt.setTimestamp(6, Timestamp.valueOf(bid.getTime()));

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi lưu biên lai Bid: " + e.getMessage());
            return false;
        }
    }

    /**
     * 1. Lấy N lượt đặt giá mới nhất của một phiên (Dùng cho Live Room)
     */
    @Override
    public List<BidTransaction> findTopByAuctionId(String auctionId, int limit) {
        List<BidTransaction> bids = new ArrayList<>();
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? AND status = 'ACCEPTED' " +
                "ORDER BY created_at DESC LIMIT ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, auctionId);
            stmt.setInt(2, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    bids.add(mapResultSetToBid(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy lịch sử Bid top: " + e.getMessage());
        }
        return bids;
    }

    /**
     * 2. Lấy toàn bộ lịch sử đặt giá có phân trang (Dùng cho màn hình xem lại lịch sử)
     */
    @Override
    public List<BidTransaction> findByAuctionIdPaged(String auctionId, int limit, int offset) {
        List<BidTransaction> bids = new ArrayList<>();
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? " +
                "ORDER BY created_at DESC LIMIT ? OFFSET ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, auctionId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    bids.add(mapResultSetToBid(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy lịch sử Bid phân trang: " + e.getMessage());
        }
        return bids;
    }

    /**
     * 3. Lấy các lượt đặt giá của một người dùng (Dùng để xem "Lịch sử đi đấu giá" của tôi)
     */
    @Override
    public List<BidTransaction> findByBidderId(String bidderId) {
        List<BidTransaction> bids = new ArrayList<>();
        String sql = "SELECT * FROM bid_transactions WHERE bidder_id = ? ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, bidderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    bids.add(mapResultSetToBid(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy lịch sử Bid của người dùng: " + e.getMessage());
        }
        return bids;
    }

    /**
     * Helper Method: Ánh xạ dữ liệu từ SQL sang Object Java
     */
    private BidTransaction mapResultSetToBid(ResultSet rs) throws SQLException {
        return new BidTransaction(
                rs.getString("id"),
                rs.getString("bidder_id"),
                rs.getString("auction_id"),
                rs.getDouble("amount"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                BidStatus.valueOf(rs.getString("status"))
        );
    }
}