package com.auction.dao.impl;

import com.auction.config.DatabaseConnection;
import com.auction.dao.AuctionDAO;
import com.auction.dao.ItemDAO;
import com.auction.enums.AuctionStatus;
import com.auction.models.Auction.Auction;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AuctionDAOImpl implements AuctionDAO {

    // 🔥 SỬA: Thêm throws SQLException, dọn bỏ hoàn toàn khối try-catch nuốt lỗi
    @Override
    public boolean insertAuction(Connection conn, Auction auction) throws SQLException {
        String sql = "INSERT INTO auctions (id, item_id, seller_id, current_price, step_price, start_time, end_time, status) " +
                "VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, auction.getId());
            stmt.setString(2, auction.getItem().getId());
            stmt.setString(3, auction.getSellerId());
            stmt.setDouble(4, auction.getCurrentPrice());
            stmt.setDouble(5, auction.getStepPrice());

            stmt.setTimestamp(6, Timestamp.valueOf((LocalDateTime) auction.getStartTime()));
            stmt.setTimestamp(7, Timestamp.valueOf((LocalDateTime) auction.getEndTime()));
            stmt.setString(8, auction.getStatus().name());

            return stmt.executeUpdate() > 0;
        }
    }

    // 🔥 SỬA: Thêm throws SQLException, bóc gỡ try-catch để lộ lỗi phục vụ rollback ở Service
    @Override
    public boolean updatePriceAndWinner(Connection conn, String auctionId, double newPrice, String newWinnerId, String winningBidId, LocalDateTime endTime, double liveStepPrice) throws SQLException {
        String sql = "UPDATE auctions SET current_price = ?, highest_bidder_id = UUID_TO_BIN(?, 1), " +
                "current_winning_bid_id = UUID_TO_BIN(?, 1), end_time = ?, step_price = ?, updated_at = NOW() WHERE id = UUID_TO_BIN(?, 1)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, newPrice);
            stmt.setString(2, newWinnerId);
            stmt.setString(3, winningBidId);
            stmt.setTimestamp(4, Timestamp.valueOf(endTime));
            stmt.setDouble(5, liveStepPrice);
            stmt.setString(6, auctionId);

            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public Optional<Auction> findById(String id) {
        String sql = "SELECT BIN_TO_UUID(id, 1) AS id, BIN_TO_UUID(item_id, 1) AS item_id, BIN_TO_UUID(seller_id, 1) AS seller_id, BIN_TO_UUID(highest_bidder_id, 1) AS highest_bidder_id, BIN_TO_UUID(current_winning_bid_id, 1) AS current_winning_bid_id, current_price, step_price, start_time, end_time, status, created_at, updated_at FROM auctions WHERE id = UUID_TO_BIN(?, 1)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAuction(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi Find Auction By Id: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Auction> findBySellerId(String sellerId) {
        List<Auction> auctions = new ArrayList<>();

        if (sellerId == null || sellerId.trim().isEmpty()) {
            return auctions;
        }

        String sql = "SELECT BIN_TO_UUID(id, 1) AS id, BIN_TO_UUID(item_id, 1) AS item_id, BIN_TO_UUID(seller_id, 1) AS seller_id, BIN_TO_UUID(highest_bidder_id, 1) AS highest_bidder_id, BIN_TO_UUID(current_winning_bid_id, 1) AS current_winning_bid_id, current_price, step_price, start_time, end_time, status, created_at, updated_at FROM auctions WHERE seller_id = UUID_TO_BIN(?, 1) ORDER BY start_time DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sellerId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    auctions.add(mapResultSetToAuction(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi findBySellerId: " + e.getMessage());
        }
        return auctions;
    }

    @Override
    public List<Auction> findRunningAuctionsPastEndTime() {
        List<Auction> expiredAuctions = new ArrayList<>();
        String sql = "SELECT BIN_TO_UUID(id, 1) AS id, BIN_TO_UUID(item_id, 1) AS item_id, BIN_TO_UUID(seller_id, 1) AS seller_id, BIN_TO_UUID(highest_bidder_id, 1) AS highest_bidder_id, BIN_TO_UUID(current_winning_bid_id, 1) AS current_winning_bid_id, current_price, step_price, start_time, end_time, status, created_at, updated_at FROM auctions WHERE status = 'RUNNING' AND end_time <= NOW()";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                expiredAuctions.add(mapResultSetToAuction(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi quét phiên hết hạn: " + e.getMessage());
        }
        return expiredAuctions;
    }

    // 🔥 SỬA: Hàm ghi dữ liệu tham gia chốt phiên bắt buộc phải ném SQLException ra ngoài
    @Override
    public void updateStatus(Connection conn, String auctionId, String status) throws SQLException {
        String sql = "UPDATE auctions SET status = ? WHERE id = UUID_TO_BIN(?, 1)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, auctionId);

            stmt.executeUpdate();
        }
    }

    /**
     * Helper Method: Chuyển đổi dòng dữ liệu (ResultSet) thành Object Auction
     * Hàm này vốn dĩ đã throws SQLException sẵn nên cấu trúc giữ nguyên rất sạch sẽ
     */
    private Auction mapResultSetToAuction(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String itemId = rs.getString("item_id");
        String sellerId = rs.getString("seller_id");
        String highestBidderId = rs.getString("highest_bidder_id");
        String currentWinningBidId = rs.getString("current_winning_bid_id");

        double currentPrice = rs.getDouble("current_price");
        double stepPrice = rs.getDouble("step_price");

        Timestamp startTimeTs = rs.getTimestamp("start_time");
        LocalDateTime startTime = startTimeTs != null ? startTimeTs.toLocalDateTime() : null;

        Timestamp endTimeTs = rs.getTimestamp("end_time");
        LocalDateTime endTime = endTimeTs != null ? endTimeTs.toLocalDateTime() : null;

        Timestamp createdAtTs = rs.getTimestamp("created_at");
        LocalDateTime createdAt = createdAtTs != null ? createdAtTs.toLocalDateTime() : null;

        Timestamp updatedAtTs = rs.getTimestamp("updated_at");
        LocalDateTime updatedAt = updatedAtTs != null ? updatedAtTs.toLocalDateTime() : null;

        AuctionStatus status = AuctionStatus.valueOf(rs.getString("status"));

        return new Auction(
                id, itemId, sellerId, highestBidderId, currentWinningBidId,
                currentPrice, stepPrice, startTime, endTime,
                createdAt, updatedAt, status
        );
    }

    // 🔥 Hàm phục vụ nạp RAM và hiển thị trang chủ, giữ nguyên ném ngoại lệ lên Service
    @Override
    public List<Auction> findByStatuses(Connection conn, List<AuctionStatus> statuses) throws SQLException {
        List<Auction> auctions = new ArrayList<>();
        if (statuses == null || statuses.isEmpty()) {
            return auctions;
        }

        String placeholders = String.join(",", Collections.nCopies(statuses.size(), "?"));
        String sql = "SELECT BIN_TO_UUID(id, 1) AS id, BIN_TO_UUID(item_id, 1) AS item_id, BIN_TO_UUID(seller_id, 1) AS seller_id, BIN_TO_UUID(highest_bidder_id, 1) AS highest_bidder_id, BIN_TO_UUID(current_winning_bid_id, 1) AS current_winning_bid_id, current_price, step_price, start_time, end_time, status, created_at, updated_at FROM auctions WHERE status IN (" + placeholders + ")";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < statuses.size(); i++) {
                stmt.setString(i + 1, statuses.get(i).name());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Auction auction = mapResultSetToAuction(rs);

                    ItemDAO itemDAO = new ItemDAOImpl();
                    itemDAO.findById(auction.getItemId()).ifPresent(auction::setItem);

                    auctions.add(auction);
                }
            }
        }
        return auctions;
    }

    // 🔥 SỬA: Hàm ép đồng bộ định kỳ lúc tắt Server, ném thẳng lỗi SQLException lên luồng chính xử lý
    @Override
    public boolean updateAuctionStatusAndBidding(Auction auction) throws SQLException {
        String sql = "UPDATE auctions SET current_price = ?, highest_bidder_id = UUID_TO_BIN(?, 1), " +
                "current_winning_bid_id = UUID_TO_BIN(?, 1), status = ?, updated_at = NOW() WHERE id = UUID_TO_BIN(?, 1)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, auction.getCurrentPrice());
            stmt.setString(2, auction.getHighestBidderId());
            stmt.setString(3, auction.getCurrentWinningBidId());
            stmt.setString(4, auction.getStatus().name());
            stmt.setString(5, auction.getId());

            return stmt.executeUpdate() > 0;
        }
    }
}