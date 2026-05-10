package com.auction.dto;

//Dành cho hiển thị list Item mà Seller bán
public class ItemSummaryDTO {
    private String itemId;
    private String itemName;
    private double startingPrice;
    private String itemType; // "ART", "ELECTRONICS", "VEHICLE"
    private String status;   // "ACTIVE", "SOLD", "REMOVED"

    public ItemSummaryDTO(String itemId, String itemName, double startingPrice, String itemType, String status) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.startingPrice = startingPrice;
        this.itemType = itemType;
        this.status = status;
    }

    // Các Getters...
    public String getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public double getStartingPrice() { return startingPrice; }
    public String getItemType() { return itemType; }
    public String getStatus() { return status; }
}