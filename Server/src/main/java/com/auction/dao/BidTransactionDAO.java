package com.auction.dao;

import com.auction.models.Auction.BidTransaction;

public interface BidTransactionDAO {
    public boolean insertBid(BidTransaction bid);
}
