package com.auction.dao;

import com.auction.models.Auction.BidTransaction;

import java.util.List;

public interface BidTransactionDAO {
    public boolean insertBid(BidTransaction bid);

    List<BidTransaction> findTopByAuctionId(String auctionId, int limit);

    List<BidTransaction> findByAuctionIdPaged(String auctionId, int limit, int offset);

    List<BidTransaction> findByBidderId(String bidderId);
}
