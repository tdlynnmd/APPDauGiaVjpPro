package com.auction.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO gửi yêu cầu tạo mới sản phẩm/vật phẩm đấu giá với các trường đa hình tương ứng.
 */
public class CreateItemRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String itemType;
    private String name;
    private Double startingPrice;
    private String description;
    private Integer yearCreated;
    private String imageUrl;
    private String painter;
    private String artStyle;
    private String brand;
    private Integer warrantyMonths;
    private String model;
    private String engineType;
    private String licensePlate;
    private Double kmAge;

    public CreateItemRequest() {
    }

    public CreateItemRequest(String itemType, String name, Double startingPrice, String description,
                             Integer yearCreated, String imageUrl, String painter, String artStyle,
                             String brand, Integer warrantyMonths, String model, String engineType,
                             String licensePlate, Double kmAge) {
        this.itemType = itemType;
        this.name = name;
        this.startingPrice = startingPrice;
        this.description = description;
        this.yearCreated = yearCreated;
        this.imageUrl = imageUrl;
        this.painter = painter;
        this.artStyle = artStyle;
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
        this.model = model;
        this.engineType = engineType;
        this.licensePlate = licensePlate;
        this.kmAge = kmAge;
    }

    public Map<String, Object> toItemDataMap(String sellerId) {
        Map<String, Object> data = new HashMap<>();
        putIfPresent(data, "sellerId", sellerId);
        putIfPresent(data, "name", name);
        putIfPresent(data, "startingPrice", startingPrice);
        putIfPresent(data, "description", description);
        putIfPresent(data, "yearCreated", yearCreated);
        putIfPresent(data, "imageUrl", imageUrl);
        putIfPresent(data, "painter", painter);
        putIfPresent(data, "artStyle", artStyle);
        putIfPresent(data, "brand", brand);
        putIfPresent(data, "warrantyMonths", warrantyMonths);
        putIfPresent(data, "model", model);
        putIfPresent(data, "engineType", engineType);
        putIfPresent(data, "licensePlate", licensePlate);
        putIfPresent(data, "kmAge", kmAge);
        return data;
    }

    private void putIfPresent(Map<String, Object> data, String key, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String text && text.trim().isEmpty()) {
            return;
        }
        data.put(key, value);
    }

    public String getNormalizedItemType() {
        if (itemType == null) {
            return null;
        }

        String normalizedType = itemType.trim().toUpperCase();
        return "VEHICLE".equals(normalizedType) ? "VEHICLES" : normalizedType;
    }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(Double startingPrice) { this.startingPrice = startingPrice; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getYearCreated() { return yearCreated; }
    public void setYearCreated(Integer yearCreated) { this.yearCreated = yearCreated; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
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
