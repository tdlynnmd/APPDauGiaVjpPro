package com.auction.dto;

import java.time.LocalDateTime;

//Dành cho hiển thị danh sách auction bidder tham gia

public class AuctionSummaryDTO {
    private String auctionId;
    private String itemName;
    private double currentPrice;
    private String status;
    private LocalDateTime endTime;
    private LocalDateTime startTime;
    private double stepPrice;

    /**
     * Constructor rỗng cần cho Gson khi parse JSON thành object.
     */
    public AuctionSummaryDTO() {
    }

    public AuctionSummaryDTO(String auctionId, String itemName, double currentPrice, String status, LocalDateTime endTime) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.currentPrice = currentPrice;
        this.status = status;
        this.endTime = endTime;
    }

    public AuctionSummaryDTO(String auctionId, String itemName, double currentPrice, String status, LocalDateTime endTime, LocalDateTime startTime, double stepPrice) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.currentPrice = currentPrice;
        this.status = status;
        this.endTime = endTime;
        this.startTime = startTime;
        this.stepPrice = stepPrice;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getItemName() {
        return itemName;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public double getStepPrice() {
        return stepPrice;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setStepPrice(double stepPrice) {
        this.stepPrice = stepPrice;
    }
}