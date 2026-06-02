package com.auction.dto;

import java.time.LocalDateTime;

public class UpdateAuctionRequest {
    private String auctionId;
    private double stepPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public UpdateAuctionRequest() {
    }

    public UpdateAuctionRequest(String auctionId, double stepPrice, LocalDateTime startTime, LocalDateTime endTime) {
        this.auctionId = auctionId;
        this.stepPrice = stepPrice;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public double getStepPrice() {
        return stepPrice;
    }

    public void setStepPrice(double stepPrice) {
        this.stepPrice = stepPrice;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
