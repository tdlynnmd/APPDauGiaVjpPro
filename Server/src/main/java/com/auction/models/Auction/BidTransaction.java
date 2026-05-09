package com.auction.models.Auction;

import com.auction.models.User.User;

import java.time.LocalDateTime;

public class BidTransaction {
    private User bidder;
    private double amount;
    private LocalDateTime time;

    public BidTransaction(User bidder, double amount, LocalDateTime time) {
        this.bidder = bidder;
        this.amount = amount;
        this.time = time;
    }

}
