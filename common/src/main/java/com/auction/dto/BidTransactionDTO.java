package com.auction.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO chứa thông tin về một giao dịch đặt giá (bid) của người dùng.
 */
public class BidTransactionDTO implements Serializable {
    private String bidId;
    private String auctionId;
    private String bidderName;
    private double amount;
    private LocalDateTime time;
    private String status;
    private LocalDateTime newEndTime;
    private double liveStepPrice;

    public BidTransactionDTO(String bidderName, double amount, LocalDateTime time, String status) {
        this.bidderName = bidderName;
        this.amount = amount;
        this.time = time;
        this.status = status;
    }

    public BidTransactionDTO(String bidderName, double amount, LocalDateTime time, String status, LocalDateTime newEndTime, double liveStepPrice) {
        this.bidderName = bidderName;
        this.amount = amount;
        this.time = time;
        this.status = status;
        this.newEndTime = newEndTime;
        this.liveStepPrice = liveStepPrice;
    }

    public String getBidId() {
        return bidId;
    }

    public void setBidId(String bidId) {
        this.bidId = bidId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public double getAmount() {
        return amount;
    }

    public String getBidderName() {
        return bidderName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public LocalDateTime getNewEndTime() {
        return newEndTime;
    }

    public double getLiveStepPrice() {
        return liveStepPrice;
    }
}
