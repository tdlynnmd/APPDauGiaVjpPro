package com.auction.dao;

import com.auction.enums.AuctionStatus;
import com.auction.models.Auction.Auction;
import java.util.List;
import java.util.Optional;

public interface AuctionDAO {
    boolean insertAuction(Auction auction);
    Optional<Auction> findById(String id);

    // Hàm cốt lõi khi có người đặt giá thành công
    boolean updatePriceAndWinner(String auctionId, double newPrice, String newWinnerId, String winningBidId);

    // Dành cho hệ thống chạy ngầm kiểm tra phiên hết hạn
    List<Auction> findRunningAuctionsPastEndTime();

    void updateStatus(String auctionId, String status);

    List<Auction> findByStatuses(List<AuctionStatus> activeStatuses);

    /**
     * Tìm tất cả các phiên đấu giá do một Seller cụ thể tạo ra
     */
    List<Auction> findBySellerId(String sellerId);

    /**
     * Ép đồng bộ toàn bộ trạng thái động của phiên đấu giá từ RAM xuống DB.
     * Sử dụng chủ yếu cho tiến trình quét ngầm định kỳ hoặc Graceful Shutdown Hook.
     */
    boolean updateAuctionStatusAndBidding(Auction auction);
}