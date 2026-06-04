package com.auction.dto;

/**
 * DTO gửi yêu cầu tham gia (subscribe) hoặc rời (unsubscribe) phòng đấu giá để nhận cập nhật real-time.
 */
public class AuctionSubscriptionRequest {

    private String auctionId;

    public AuctionSubscriptionRequest() {
    }

    public AuctionSubscriptionRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAuctionId() {
        return auctionId;
    }
}