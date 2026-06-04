package com.auction.models.Auction;

import com.auction.enums.BidStatus;
import com.auction.models.Entity.Entity;
import com.auction.models.User.User;

import java.time.LocalDateTime;

/**
 * Lớp biểu diễn thực thể BidTransaction trong hệ thống.
 */
public class BidTransaction extends Entity {
    private String bidderId;
    private String auctionId;
    private double amount;
    private LocalDateTime time;
    private BidStatus status;

    public BidTransaction(String id, String bidderId, String auctionId,
                          double amount, LocalDateTime time, BidStatus status) {
        super(id);
        this.bidderId = bidderId;
        this.auctionId = auctionId;
        this.amount = amount;
        this.time = time;
        this.status = status;
    }

    public BidTransaction( String bidderId, String auctionId,
                          double amount, LocalDateTime time, BidStatus status) {
        this.bidderId = bidderId;
        this.auctionId = auctionId;
        this.amount = amount;
        this.time = time;
        this.status = status;
    }

    public BidStatus getStatus() {
        return this.status;
    }

    public void setStatus(BidStatus status) {
        this.status = status;
    }

    public String getBidderId() {
        return bidderId;
    }

    public double getAmount() {
        return amount;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public LocalDateTime getTime() {
        return time;
    }
}
