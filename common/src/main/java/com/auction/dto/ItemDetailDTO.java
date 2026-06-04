package com.auction.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO chứa thông tin chi tiết đầy đủ của một sản phẩm bao gồm đặc tính đa hình.
 */
public class ItemDetailDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String itemId;
    private String itemName;
    private Double startingPrice;
    private String itemType;
    private String status;
    private String description;
    private Integer yearCreated;
    private String imageUrl;
    private String sellerId;
    private LocalDateTime createdAt;
    private String painter;
    private String artStyle;
    private String brand;
    private Integer warrantyMonths;
    private String model;
    private String engineType;
    private String licensePlate;
    private Double kmAge;

    public ItemDetailDTO() {
    }

    public ItemDetailDTO(String itemId, String itemName, Double startingPrice, String itemType, String status,
                         String description, Integer yearCreated, String imageUrl, String sellerId,
                         LocalDateTime createdAt, String painter, String artStyle, String brand,
                         Integer warrantyMonths, String model, String engineType, String licensePlate,
                         Double kmAge) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.startingPrice = startingPrice;
        this.itemType = itemType;
        this.status = status;
        this.description = description;
        this.yearCreated = yearCreated;
        this.imageUrl = imageUrl;
        this.sellerId = sellerId;
        this.createdAt = createdAt;
        this.painter = painter;
        this.artStyle = artStyle;
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
        this.model = model;
        this.engineType = engineType;
        this.licensePlate = licensePlate;
        this.kmAge = kmAge;
    }

    public ItemSummaryDTO toSummaryDTO() {
        return new ItemSummaryDTO(itemId, itemName, startingPrice == null ? 0.0 : startingPrice, itemType, status);
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public Double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(Double startingPrice) { this.startingPrice = startingPrice; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getYearCreated() { return yearCreated; }
    public void setYearCreated(Integer yearCreated) { this.yearCreated = yearCreated; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
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
