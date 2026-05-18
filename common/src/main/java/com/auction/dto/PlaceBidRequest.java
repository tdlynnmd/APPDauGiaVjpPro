package com.auction.dto;

/**
 * DTO chứa dữ liệu Client gửi lên khi Bidder đặt giá.

 * Luồng:
  Bidder nhập số tiền muốn đặt
  -> Client gửi action PLACE_BID
  -> body chứa auctionId và amount
  -> Server kiểm tra quyền BIDDER
  -> AuctionService xử lý đặt giá
 */
public class PlaceBidRequest {

    /**
     ID phiên đấu giá mà Bidder muốn đặt giá.
     */
    private String auctionId;

    /**
     * Số tiền Bidder muốn đặt.
     */
    private double amount;

    public PlaceBidRequest() {
    }

    public PlaceBidRequest(String auctionId, double amount) {
        this.auctionId = auctionId;
        this.amount = amount;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public double getAmount() {
        return amount;
    }
}