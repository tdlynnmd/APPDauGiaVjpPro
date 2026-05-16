package com.auction.dao;

import com.auction.enums.AuctionStatus;
import com.auction.models.Auction.Auction;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AuctionDAO {
    boolean insertAuction(Auction auction);
    Optional<Auction> findById(String id);

    // Hàm cốt lõi khi có người đặt giá thành công
    boolean updatePriceAndWinner(String auctionId, double newPrice, String newWinnerId, String winningBidId);

    // Dành cho hệ thống chạy ngầm kiểm tra phiên hết hạn
    List<Auction> findRunningAuctionsPastEndTime();

    boolean updateStatus(String auctionId, String status);

    List<Auction> findByStatuses(List<AuctionStatus> activeStatuses);
}