package com.auction.dao.impl;

import com.auction.config.DatabaseConnection;
import com.auction.dao.BidTransactionDAO;
import com.auction.enums.BidStatus;
import com.auction.models.Auction.BidTransaction;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp triển khai JDBC truy vấn dữ liệu cho bảng bid_transactions.
 */
public class BidTransactionDAOImpl implements BidTransactionDAO {

    @Override
    public boolean insertBid(Connection conn, BidTransaction bid) throws SQLException {
        String sql = "INSERT INTO bid_transactions (id, bidder_id, auction_id, amount, status, created_at) " +
                "VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, bid.getId());
            stmt.setString(2, bid.getBidderId());
            stmt.setString(3, bid.getAuctionId());
            stmt.setDouble(4, bid.getAmount());
            stmt.setString(5, bid.getStatus().name());
            stmt.setTimestamp(6, Timestamp.valueOf(bid.getTime()));

            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public List<String> findDistinctBidderIdsByAuctionId(String auctionId) {
        List<String> bidderIds = new ArrayList<>();
        if (auctionId == null || auctionId.trim().isEmpty()) {
            return bidderIds;
        }

        String sql = "SELECT DISTINCT BIN_TO_UUID(bidder_id, 1) AS bidder_id " +
                "FROM bid_transactions WHERE auction_id = UUID_TO_BIN(?, 1)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    bidderIds.add(rs.getString("bidder_id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Loi lay danh sach bidder theo phien: " + e.getMessage());
        }
        return bidderIds;
    }

    @Override
    public List<BidTransaction> findTopByAuctionId(String auctionId, int limit) {
        List<BidTransaction> bids = new ArrayList<>();
        String sql = "SELECT BIN_TO_UUID(id, 1) AS id, BIN_TO_UUID(bidder_id, 1) AS bidder_id, BIN_TO_UUID(auction_id, 1) AS auction_id, amount, status, created_at FROM bid_transactions WHERE auction_id = UUID_TO_BIN(?, 1) AND status IN ('ACCEPTED', 'REFUNDED') " +
                "ORDER BY created_at DESC, amount DESC LIMIT ?";

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
            System.err.println("❌ Lỗi lấy lịch sử Bid top: " + e.getMessage());
        }
        return bids;
    }

    @Override
    public List<BidTransaction> findByAuctionIdPaged(String auctionId, int limit, int offset) {
        List<BidTransaction> bids = new ArrayList<>();
        String sql = "SELECT BIN_TO_UUID(id, 1) AS id, BIN_TO_UUID(bidder_id, 1) AS bidder_id, BIN_TO_UUID(auction_id, 1) AS auction_id, amount, status, created_at FROM bid_transactions WHERE auction_id = UUID_TO_BIN(?, 1) " +
                "ORDER BY created_at DESC, amount DESC LIMIT ? OFFSET ?";

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
            System.err.println("❌ Lỗi lấy lịch sử Bid phân trang: " + e.getMessage());
        }
        return bids;
    }

    @Override
    public List<BidTransaction> findByBidderIdPaged(String bidderId, int limit, int offset) {
        List<BidTransaction> bids = new ArrayList<>();
        String sql = "SELECT BIN_TO_UUID(id, 1) AS id, BIN_TO_UUID(bidder_id, 1) AS bidder_id, BIN_TO_UUID(auction_id, 1) AS auction_id, amount, status, created_at FROM bid_transactions WHERE bidder_id = UUID_TO_BIN(?, 1) " +
                "ORDER BY created_at DESC, amount DESC LIMIT ? OFFSET ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, bidderId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    bids.add(mapResultSetToBid(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi lấy lịch sử Bid phân trang của user: " + e.getMessage());
        }
        return bids;
    }

    @Override
    public long getTotalBidCountByAuction(String auctionId) {
        String sql = "SELECT COUNT(*) FROM bid_transactions WHERE auction_id = UUID_TO_BIN(?, 1)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi đếm tổng số bid của phiên: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public long getTotalBidCountByBidder(String bidderId) {
        String sql = "SELECT COUNT(*) FROM bid_transactions WHERE bidder_id = UUID_TO_BIN(?, 1)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, bidderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi đếm tổng số bid của user: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public void updateStatusToRefunded(Connection conn, String auctionId, String bidderId) throws SQLException {
        String sql = "UPDATE bid_transactions SET status = 'REFUNDED' " +
                "WHERE auction_id = UUID_TO_BIN(?, 1) AND bidder_id = UUID_TO_BIN(?, 1) AND status = 'ACCEPTED'";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, auctionId);
            stmt.setString(2, bidderId);
            stmt.executeUpdate();
        }
    }

    @Override
    public void updateStatusByBidId(Connection conn, String bidId, String status) throws SQLException {
        String sql = "UPDATE bid_transactions SET status = ? WHERE id = UUID_TO_BIN(?, 1)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, bidId);
            stmt.executeUpdate();
        }
    }

    private BidTransaction mapResultSetToBid(ResultSet rs) throws SQLException {
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        LocalDateTime createdAt = createdAtTs != null ? createdAtTs.toLocalDateTime() : null;
        return new BidTransaction(
                rs.getString("id"),
                rs.getString("bidder_id"),
                rs.getString("auction_id"),
                rs.getDouble("amount"),
                createdAt,
                BidStatus.valueOf(rs.getString("status"))
        );
    }
}
