package com.auction.dto;

/**
  DTO chứa dữ liệu khi Seller/Admin muốn hủy một phiên đấu giá.

 * Lưu ý:
  - Seller chỉ nên hủy phiên của chính mình.
  - Admin có thể hủy mọi phiên.
  - Việc kiểm tra chi tiết này nên làm ở AuctionController hoặc AuctionService.
 */
public class CancelAuctionRequest {

    /**
     * ID phiên đấu giá cần hủy.
     */
    private String auctionId;

    /**
     * Lý do hủy phiên, dùng để thông báo cho các client đang theo dõi.
     */
    private String reason;

    public CancelAuctionRequest() {
    }

    public CancelAuctionRequest(String auctionId, String reason) {
        this.auctionId = auctionId;
        this.reason = reason;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getReason() {
        return reason;
    }
}