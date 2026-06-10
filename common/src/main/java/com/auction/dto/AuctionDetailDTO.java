package com.auction.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO chứa thông tin chi tiết đầy đủ của một phiên đấu giá bao gồm lịch sử thầu và cấu hình AutoBid.
 */
public class AuctionDetailDTO {
    private String auctionId;
    private double currentPrice;
    private double stepPrice;
    private double liveStepPrice;
    private LocalDateTime endTime;
    private String status;

    private String itemName;
    private String itemDescription;
    private String imageUrl;

    private String sellerUsername;

    private List<BidTransactionDTO> bidHistory;

    private double activeAutoBidMaxBid;
    private double activeAutoBidIncrement;

    public AuctionDetailDTO(String auctionId, double currentPrice, double stepPrice,
                            LocalDateTime endTime, String status, String itemName,
                            String itemDescription, String imageUrl,
                            String sellerUsername, List<BidTransactionDTO> bidHistory) {
        this.auctionId = auctionId;
        this.currentPrice = currentPrice;
        this.stepPrice = stepPrice;
        this.endTime = endTime;
        this.status = status;
        this.itemName = itemName;
        this.itemDescription = itemDescription;
        this.imageUrl = imageUrl;
        this.sellerUsername = sellerUsername;
        this.bidHistory = bidHistory;
    }

    public double getLiveStepPrice() {
        return liveStepPrice;
    }

    public void setLiveStepPrice(double liveStepPrice) {
        this.liveStepPrice = liveStepPrice;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getStepPrice() {
        return stepPrice;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public String getItemName() {
        return itemName;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public List<BidTransactionDTO> getBidHistory() {
        return bidHistory;
    }

    public double getActiveAutoBidMaxBid() {
        return activeAutoBidMaxBid;
    }

    public void setActiveAutoBidMaxBid(double activeAutoBidMaxBid) {
        this.activeAutoBidMaxBid = activeAutoBidMaxBid;
    }

    public double getActiveAutoBidIncrement() {
        return activeAutoBidIncrement;
    }

    public void setActiveAutoBidIncrement(double activeAutoBidIncrement) {
        this.activeAutoBidIncrement = activeAutoBidIncrement;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    private int viewerCount;

    public int getViewerCount() {
        return viewerCount;
    }

    public void setViewerCount(int viewerCount) {
        this.viewerCount = viewerCount;
    }

    private String itemType;
    private Integer yearCreated;
    private String painter;
    private String artStyle;
    private String brand;
    private Integer warrantyMonths;
    private String model;
    private String engineType;
    private String licensePlate;
    private Double kmAge;

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public Integer getYearCreated() { return yearCreated; }
    public void setYearCreated(Integer yearCreated) { this.yearCreated = yearCreated; }

    public String getPainter() { return painter; }
    public void setPainter(String painter) { this.painter = painter; }

    public String getArtStyle() { return artStyle; }
    public void setArtStyle(String artStyle) { this.artStyle = artStyle; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public Integer getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(Integer warrantyMonths) { this.warrantyMonths = warrantyMonths; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getEngineType() { return engineType; }
    public void setEngineType(String engineType) { this.engineType = engineType; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public Double getKmAge() { return kmAge; }
    public void setKmAge(Double kmAge) { this.kmAge = kmAge; }
}
