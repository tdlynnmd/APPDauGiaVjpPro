package com.auction.dao;

import com.auction.models.Auction.BidTransaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface BidTransactionDAO {

    boolean insertBid(Connection conn, BidTransaction bid) throws SQLException;

    List<BidTransaction> findTopByAuctionId(String auctionId, int limit);

    List<BidTransaction> findByAuctionIdPaged(String auctionId, int limit, int offset);

    List<BidTransaction> findByBidderIdPaged(String bidderId, int limit, int offset);

    List<String> findDistinctBidderIdsByAuctionId(String auctionId);

    long getTotalBidCountByAuction(String auctionId);

    long getTotalBidCountByBidder(String bidderId);

    // Thêm hàm này vào BidTransactionDAOImpl để chuyển trạng thái bid cũ
    void updateStatusToRefunded(Connection conn, String auctionId, String bidderId) throws SQLException;

    void updateStatusByBidId(Connection conn, String bidId, String status) throws SQLException;
}

