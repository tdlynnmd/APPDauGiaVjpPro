package com.auction.dto;

import java.time.LocalDateTime;
import java.util.List;


public class AuctionDetailDTO {
    // Thông tin phiên
    private String auctionId;
    private double currentPrice;
    private double stepPrice;
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
}
