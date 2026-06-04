package com.auction.dto;

import java.io.Serializable;

/**
 * DTO gửi yêu cầu thiết lập hoặc thay đổi cấu hình tự động đặt giá (AutoBid).
 */
public class SetupAutoBidRequest implements Serializable {
    private String auctionId;
    private double maxBid;
    private double increment;

    public SetupAutoBidRequest() {}

    public SetupAutoBidRequest(String auctionId, double maxBid, double increment) {
        this.auctionId = auctionId;
        this.maxBid = maxBid;
        this.increment = increment;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public double getMaxBid() {
        return maxBid;
    }

    public void setMaxBid(double maxBid) {
        this.maxBid = maxBid;
    }

    public double getIncrement() {
        return increment;
    }

    public void setIncrement(double increment) {
        this.increment = increment;
    }
}
