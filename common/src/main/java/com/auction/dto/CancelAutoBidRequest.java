package com.auction.dto;

import java.io.Serializable;

/**
 * DTO gửi yêu cầu hủy cấu hình tự động đặt giá (AutoBid) cho một phiên.
 */
public class CancelAutoBidRequest implements Serializable {
    private String auctionId;

    public CancelAutoBidRequest() {}

    public CancelAutoBidRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }
}
