package com.auction.dto;

import java.io.Serializable;

/**
 * DTO gửi yêu cầu hủy phiên đấu giá từ người bán hoặc quản trị viên.
 */
public class CancelAuctionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String auctionId;

    private String reason;

    private String userId;

    public CancelAuctionRequest() {
    }

    public CancelAuctionRequest(String auctionId, String reason, String userId) {
        this.auctionId = auctionId;
        this.reason = reason;
        this.userId = userId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}