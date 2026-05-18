package com.auction.dto;

/**
 * Chua du lieu Client gui len khi Seller/Admin muon tao phien dau gia
 */
public class CreateAuctionRequest {
    /**
     Ko nen de Client gui sellerId
     Server se lay sellerId tu ClientSession sau khi login
     Nhu vay nguoi dung ko the gia mao Id cua nguoi khac
     */
    private String itemId;
    private double stepPrice;       // Buoc gia toi thieu moi lan Bid
    private String startTime;
    private String endTime;
    public CreateAuctionRequest(){};

    public CreateAuctionRequest(String endTime, String itemId, String startTime, double stepPrice) {
        this.endTime = endTime;
        this.itemId = itemId;
        this.startTime = startTime;
        this.stepPrice = stepPrice;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getItemId() {
        return itemId;
    }

    public String getStartTime() {
        return startTime;
    }

    public double getStepPrice() {
        return stepPrice;
    }
}
