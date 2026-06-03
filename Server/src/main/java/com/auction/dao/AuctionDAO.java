package com.auction.dao;

import com.auction.enums.AuctionStatus;
import com.auction.models.Auction.Auction;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionDAO {
    boolean insertAuction(Connection conn, Auction auction) throws SQLException;

    // 🔥 HÀM CỐT LÕI: Cập nhật giá và người dẫn đầu
    boolean updatePriceAndWinner(Connection conn, String auctionId, double newPrice, String newWinnerId, String winningBidId, LocalDateTime endTime, double liveStepPrice) throws SQLException;

    Optional<Auction> findById(String id);


    // Dành cho hệ thống chạy ngầm kiểm tra phiên hết hạn
    List<Auction> findRunningAuctionsPastEndTime();

    void updateStatus(Connection conn, String auctionId, String status) throws SQLException;

    boolean updateAuctionDetails(Connection conn, String auctionId, double stepPrice, LocalDateTime startTime, LocalDateTime endTime) throws SQLException;


    /**
     * Tìm tất cả các phiên đấu giá do một Seller cụ thể tạo ra
     */
    List<Auction> findBySellerId(String sellerId);

    List<Auction> findByStatuses(Connection conn, List<AuctionStatus> statuses) throws SQLException;

    List<Auction> findActiveAndRecentlyFinished(Connection conn, LocalDateTime finishedSince) throws SQLException;

    /**
     * Ép đồng bộ toàn bộ trạng thái động của phiên đấu giá từ RAM xuống DB.
     * Sử dụng chủ yếu cho tiến trình quét ngầm định kỳ hoặc Graceful Shutdown Hook.
     */
    boolean updateAuctionStatusAndBidding(Auction auction) throws SQLException;
}