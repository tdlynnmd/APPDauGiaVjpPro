package com.auction.dto;

import java.time.LocalDateTime;

/**
 * DTO tóm tắt thông tin cơ bản của phiên đấu giá để hiển thị trên danh sách.
 */
public class AuctionSummaryDTO {
    private String auctionId;
    private String itemId;
    private String itemName;
    private double currentPrice;
    private String status;
    private LocalDateTime endTime;
    private LocalDateTime startTime;
    private double stepPrice;
    private String itemType;

    public AuctionSummaryDTO() {
    }

    public AuctionSummaryDTO(String auctionId, String itemName, double currentPrice, String status, LocalDateTime endTime) {
        this.auctionId = auctionId;
        this.itemId = null;
        this.itemName = itemName;
        this.currentPrice = currentPrice;
        this.status = status;
        this.endTime = endTime;
    }

    public AuctionSummaryDTO(String auctionId, String itemId, String itemName, double currentPrice, String status, LocalDateTime endTime, LocalDateTime startTime, double stepPrice) {
        this.auctionId = auctionId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.currentPrice = currentPrice;
        this.status = status;
        this.endTime = endTime;
        this.startTime = startTime;
        this.stepPrice = stepPrice;
    }

    public AuctionSummaryDTO(String auctionId, String itemName, double currentPrice, String status, LocalDateTime endTime, LocalDateTime startTime, double stepPrice) {
        this.auctionId = auctionId;
        this.itemId = null;
        this.itemName = itemName;
        this.currentPrice = currentPrice;
        this.status = status;
        this.endTime = endTime;
        this.startTime = startTime;
        this.stepPrice = stepPrice;
    }

    public AuctionSummaryDTO(String auctionId, String itemId, String itemName, double currentPrice, String status, LocalDateTime endTime, LocalDateTime startTime, double stepPrice, String itemType) {
        this.auctionId = auctionId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.currentPrice = currentPrice;
        this.status = status;
        this.endTime = endTime;
        this.startTime = startTime;
        this.stepPrice = stepPrice;
        this.itemType = itemType;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
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

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }
}