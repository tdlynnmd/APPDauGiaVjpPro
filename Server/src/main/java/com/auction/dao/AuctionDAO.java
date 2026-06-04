package com.auction.dao;

import com.auction.enums.AuctionStatus;
import com.auction.models.Auction.Auction;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Interface định nghĩa các thao tác truy vấn cơ sở dữ liệu đối với bảng auctions.
 */
public interface AuctionDAO {
    boolean insertAuction(Connection conn, Auction auction) throws SQLException;

    boolean updatePriceAndWinner(Connection conn, String auctionId, double newPrice, String newWinnerId, String winningBidId, LocalDateTime endTime, double liveStepPrice) throws SQLException;

    Optional<Auction> findById(String id);

    List<Auction> findRunningAuctionsPastEndTime();

    void updateStatus(Connection conn, String auctionId, String status) throws SQLException;

    boolean updateAuctionDetails(Connection conn, String auctionId, double stepPrice, LocalDateTime startTime, LocalDateTime endTime) throws SQLException;

    List<Auction> findBySellerId(String sellerId);

    List<Auction> findByStatuses(Connection conn, List<AuctionStatus> statuses) throws SQLException;

    List<Auction> findActiveAndRecentlyFinished(Connection conn, LocalDateTime finishedSince) throws SQLException;

    boolean updateAuctionStatusAndBidding(Auction auction) throws SQLException;

    List<Auction> findAllPaginated(int limit, int offset);

    long countAllAuctions();
}