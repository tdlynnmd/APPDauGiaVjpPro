package com.auction.dto;

/**
 * DTO gửi yêu cầu tạo mới một phiên đấu giá.
 */
public class CreateAuctionRequest {
    
    private String itemId;
    private double stepPrice;
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
