package com.auction.dto;

import java.time.LocalDateTime;

//Dành cho hiển thị danh sách auction bidder tham gia

public class AuctionSummaryDTO {
    private String auctionId;
    private String itemName;
    private double currentPrice;
    private String status;
    private LocalDateTime endTime;

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
}