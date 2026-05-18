package com.auction.dto;

/**
 * DTO dùng cho SUBSCRIBE_AUCTION và UNSUBSCRIBE_AUCTION.

 * Ý nghĩa:
  - SUBSCRIBE_AUCTION: Client đang xem phiên này, muốn nhận realtime update.
  - UNSUBSCRIBE_AUCTION: Client rời màn hình chi tiết, không cần nhận update nữa.
 */
public class AuctionSubscriptionRequest {

    /**
      ID phiên đấu giá cần theo dõi hoặc rời theo dõi.
     */
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