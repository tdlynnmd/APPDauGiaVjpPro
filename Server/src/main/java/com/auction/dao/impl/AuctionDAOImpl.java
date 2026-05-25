package com.auction.dao.impl;

import com.auction.config.DatabaseConnection;
import com.auction.dao.AuctionDAO;
import com.auction.enums.AuctionStatus;
import com.auction.models.Auction.Auction;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AuctionDAOImpl implements AuctionDAO {

    @Override
    public boolean insertAuction(Auction auction) {
        String sql = "INSERT INTO auctions (id, item_id, seller_id, current_price, step_price, start_time, end_time, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, auction.getId());
            stmt.setString(2, auction.getItem().getId());
            stmt.setString(3, auction.getSellerId());
            stmt.setDouble(4, auction.getCurrentPrice());
            stmt.setDouble(5, auction.getStepPrice());

            // Ép kiểu về LocalDateTime (nếu getter của bạn đang trả về ChronoLocalDateTime)
            stmt.setTimestamp(6, Timestamp.valueOf((LocalDateTime) auction.getStartTime()));
            stmt.setTimestamp(7, Timestamp.valueOf((LocalDateTime) auction.getEndTime()));
            stmt.setString(8, auction.getStatus().name());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi Insert Auction: " + e.getMessage());
            return false;
        }
    }

    // 🔥 HÀM CỐT LÕI: Cập nhật giá và người dẫn đầu
    @Override
    public boolean updatePriceAndWinner(String auctionId, double newPrice, String newWinnerId, String winningBidId) {
        String sql = "UPDATE auctions SET current_price = ?, highest_bidder_id = ?, current_winning_bid_id = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, newPrice);
            stmt.setString(2, newWinnerId);
            stmt.setString(3, winningBidId);
            stmt.setString(4, auctionId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi cập nhật giá phiên: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Optional<Auction> findById(String id) {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAuction(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi Find Auction By Id: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * 🔥 HÀM MỚI BỔ SUNG: Tìm tất cả các phiên đấu giá do một Seller cụ thể tạo ra
     * Đảm bảo an toàn dữ liệu đầu vào, sắp xếp phiên mới nhất lên đầu (ORDER BY)
     */
    @Override
    public List<Auction> findBySellerId(String sellerId) {
        List<Auction> auctions = new ArrayList<>();

        // Bảo vệ hệ thống: Kiểm tra dữ liệu đầu vào tránh truy vấn vô nghĩa
        if (sellerId == null || sellerId.trim().isEmpty()) {
            return auctions;
        }

        // Sắp xếp theo start_time giảm dần để Frontend hiển thị các phiên mới tạo lên trước
        String sql = "SELECT * FROM auctions WHERE seller_id = ? ORDER BY start_time DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sellerId);

            // Sử dụng try-with-resources lồng nhau cho ResultSet để giải phóng bộ nhớ ngay lập tức
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Áp dụng SOLID (SRP): Tái sử dụng hàm map dữ liệu có sẵn để tránh lặp code
                    auctions.add(mapResultSetToAuction(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi findBySellerId: " + e.getMessage());
        }
        return auctions;
    }

    @Override
    public List<Auction> findRunningAuctionsPastEndTime() {
        List<Auction> expiredAuctions = new ArrayList<>();
        // Tìm các phiên đang chạy (RUNNING) nhưng thời gian hiện tại đã vượt quá end_time
        String sql = "SELECT * FROM auctions WHERE status = 'RUNNING' AND end_time <= NOW()";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                expiredAuctions.add(mapResultSetToAuction(rs));
            }
        } catch (SQLException e) {
            System.err.println("Lỗi quét phiên hết hạn: " + e.getMessage());
        }
        return expiredAuctions;
    }

    @Override
    public void updateStatus(String auctionId, String status) {
        String sql = "UPDATE auctions SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setString(2, auctionId);

            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Lỗi cập nhật trạng thái Auction: " + e.getMessage());
        }
    }

    /**
     * Helper Method: Chuyển đổi dòng dữ liệu (ResultSet) thành Object Auction
     */
    private Auction mapResultSetToAuction(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String itemId = rs.getString("item_id");
        String sellerId = rs.getString("seller_id");
        String highestBidderId = rs.getString("highest_bidder_id");
        String currentWinningBidId = rs.getString("current_winning_bid_id");

        double currentPrice = rs.getDouble("current_price");
        double stepPrice = rs.getDouble("step_price");

        LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
        LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        LocalDateTime updatedAt = rs.getTimestamp("updated_at").toLocalDateTime();

        AuctionStatus status = AuctionStatus.valueOf(rs.getString("status"));

        // Sử dụng Constructor "Tái tạo từ DB" mà bạn đã thiết kế trong class Auction
        return new Auction(
                id, itemId, sellerId, highestBidderId, currentWinningBidId,
                currentPrice, stepPrice, startTime, endTime,
                createdAt, updatedAt, status
        );
    }

    @Override
    public List<Auction> findByStatuses(List<AuctionStatus> statuses) {
        List<Auction> auctions = new ArrayList<>();

        // Kiểm tra defensive chặt chẽ
        if (statuses == null || statuses.isEmpty()) {
            return auctions;
        }

        // TỐI ƯU: Thay vì dùng Stream.map phức tạp, dùng Collections.nCopies tạo thẳng danh sách "?"
        // giúp tối ưu CPU và RAM hơn rất nhiều khi sinh chuỗi placeholders
        String placeholders = String.join(",", Collections.nCopies(statuses.size(), "?"));

        String sql = "SELECT * FROM auctions WHERE status IN (" + placeholders + ")";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Bind các giá trị enum name vào đúng thứ tự các dấu ?
            for (int i = 0; i < statuses.size(); i++) {
                stmt.setString(i + 1, statuses.get(i).name());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Áp dụng hàm map có sẵn của bạn
                    auctions.add(mapResultSetToAuction(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi findByStatuses: " + e.getMessage());
        }
        return auctions;
    }

    @Override
    public boolean updateAuctionStatusAndBidding(Auction auction) {
        // Gom tất cả các trường có khả năng biến động trên RAM vào 1 câu lệnh UPDATE duy nhất
        String sql = "UPDATE auctions SET current_price = ?, highest_bidder_id = ?, " +
                "current_winning_bid_id = ?, status = ?, updated_at = NOW() WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, auction.getCurrentPrice());
            stmt.setString(2, auction.getHighestBidderId());
            stmt.setString(3, auction.getCurrentWinningBidId());
            stmt.setString(4, auction.getStatus().name());
            stmt.setString(5, auction.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi nghiêm trọng khi ép đồng bộ trạng thái phiên "
                    + auction.getId() + " xuống DB: " + e.getMessage());
            return false;
        }
    }
}