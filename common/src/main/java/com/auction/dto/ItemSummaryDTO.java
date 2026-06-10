package com.auction.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO tóm tắt thông tin cơ bản của một sản phẩm.
 */
public class ItemSummaryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String itemId;
    private String itemName;
    private double startingPrice;
    private String itemType;
    private String status;
    private LocalDateTime createdAt;

    public ItemSummaryDTO(String itemId, String itemName, double startingPrice, String itemType, String status) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.startingPrice = startingPrice;
        this.itemType = itemType;
        this.status = status;
    }

    public ItemSummaryDTO(String itemId, String itemName, double startingPrice, String itemType, String status, LocalDateTime createdAt) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.startingPrice = startingPrice;
        this.itemType = itemType;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public double getStartingPrice() { return startingPrice; }
    public String getItemType() { return itemType; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}