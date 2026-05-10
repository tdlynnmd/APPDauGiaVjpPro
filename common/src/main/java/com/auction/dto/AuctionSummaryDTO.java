package com.auction.dto;

import java.time.LocalDateTime;

//Dành cho hiển thị danh sách auction bidder tham gia

public class AuctionSummaryDTO {
    private String auctionId;
    private String itemName;
    private double currentPrice;
    private String status;
    private LocalDateTime endTime;

    public AuctionSummaryDTO(String auctionId, String itemName, double currentPrice, String status, LocalDateTime endTime) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.currentPrice = currentPrice;
        this.status = status;
        this.endTime = endTime;
    }
    // Các Getters...
}