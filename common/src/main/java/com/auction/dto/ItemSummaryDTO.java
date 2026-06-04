package com.auction.dto;

/**
 * DTO tóm tắt thông tin cơ bản của một sản phẩm.
 */
public class ItemSummaryDTO {
    private String itemId;
    private String itemName;
    private double startingPrice;
    private String itemType;
    private String status;

    public ItemSummaryDTO(String itemId, String itemName, double startingPrice, String itemType, String status) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.startingPrice = startingPrice;
        this.itemType = itemType;
        this.status = status;
    }

    public String getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public double getStartingPrice() { return startingPrice; }
    public String getItemType() { return itemType; }
    public String getStatus() { return status; }
}