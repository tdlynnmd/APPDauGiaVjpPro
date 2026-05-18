package com.auction.dao;

import com.auction.models.Auction.BidTransaction;

import java.util.List;

public interface BidTransactionDAO {
    public boolean insertBid(BidTransaction bid);

    List<BidTransaction> findTopByAuctionId(String auctionId, int limit);

    List<BidTransaction> findByAuctionIdPaged(String auctionId, int limit, int offset);

    List<BidTransaction> findByBidderIdPaged(String bidderId, int limit, int offset);

    long getTotalBidCountByAuction(String auctionId);

    long getTotalBidCountByBidder(String bidderId);

    // Thêm hàm này vào BidTransactionDAOImpl để chuyển trạng thái bid cũ
    boolean updateStatusToRefunded(String auctionId, String bidderId);
}
