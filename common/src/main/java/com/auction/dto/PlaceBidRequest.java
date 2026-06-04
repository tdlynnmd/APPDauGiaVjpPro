package com.auction.dto;

/**
 * DTO gửi yêu cầu thực hiện lượt đặt giá mới thủ công.
 */
public class PlaceBidRequest {

    private String auctionId;

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