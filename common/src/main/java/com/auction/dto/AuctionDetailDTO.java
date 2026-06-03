package com.auction.dto;

import java.time.LocalDateTime;
import java.util.List;


public class AuctionDetailDTO {
    // Thông tin phiên
    private String auctionId;
    private double currentPrice;
    private double stepPrice;
    private double liveStepPrice;
    private LocalDateTime endTime;
    private String status;

    // Thông tin vật phẩm
    private String itemName;
    private String itemDescription;
    private String imageUrl;

    // Thông tin người bán
    private String sellerUsername;

    // Lịch sử đặt giá (Quan trọng để hiển thị bảng lịch sử)
    private List<BidTransactionDTO> bidHistory;

    // Cấu hình AutoBid đang hoạt động của người dùng hiện tại (nếu có)
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
}
