package com.auction.models.Item;

import com.auction.enums.ItemStatus;
import com.auction.enums.ItemType;
import com.auction.models.Entity.Entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public abstract class Item extends Entity implements Serializable {
    private String sellerId;
    private String name;
    private double startingPrice;
    // THAY THẾ CHO category VÀ classType TRONG ẢNH
    private ItemType itemType;
    private int yearCreated;
    private String description;
    // --- CÁC FIELD MỚI ĐƯỢC ĐỀ XUẤT ---
    private String imageUrl;
    private ItemStatus status;
    private LocalDateTime createdAt;

    // Constructor tạo mới
    public Item(String name, double startingPrice, String description,
                int yearCreated, String sellerId, ItemType itemType, String imageUrl) {
        super();
        this.name = name;
        this.startingPrice = startingPrice;
        this.description = description;
        this.yearCreated = yearCreated;
        this.sellerId = sellerId;
        this.itemType = itemType; // Bắt buộc truyền vào khi tạo subclass
        this.imageUrl = imageUrl; // Sử dụng Cloudinary
        this.status = ItemStatus.ACTIVE; // Mặc định khi mới tạo
        this.createdAt = LocalDateTime.now();
    }

    // Constructor load từ DB
    public Item(String id, String name, double startingPrice, String description,
                int yearCreated, String sellerId, ItemType itemType,
                String imageUrl, ItemStatus status, LocalDateTime createdAt) {
        super(id);
        this.name = name;
        this.startingPrice = startingPrice;
        this.description = description;
        this.yearCreated = yearCreated;
        this.sellerId = sellerId;
        this.itemType = itemType;
        this.imageUrl = imageUrl;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getter cho ItemType để tầng DAO biết đường mapping
    public ItemType getItemType() {
        return itemType;
    }

    public abstract String getInfo();

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public double getStartingPrice() {
        return this.startingPrice;
    }

    public int getYearCreated() {
        return this.yearCreated;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setStatus(ItemStatus newStatus) {
        this.status = newStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setName(String name) { this.name = name;
    }

    public void setDescription(String description) { this.description = description;
    }

    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl;
    }

    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice;
    }

    public void setYearCreated(int yearCreated) { this.yearCreated = yearCreated;
    }
}
