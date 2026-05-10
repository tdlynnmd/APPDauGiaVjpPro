package com.auction.models.Item;

import com.auction.enums.ItemStatus;
import com.auction.enums.ItemType;
import com.auction.models.Entity.Entity;

import java.io.Serializable;
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
        this.imageUrl = imageUrl;
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

    protected String getName() {
        return this.name;
    }

    protected String getDescription() {
        return this.description;
    }

    public double getStartingPrice() {
        return this.startingPrice;
    }

    protected int getYearCreated() {
        return this.yearCreated;
    }

    public ItemStatus getStatus() {
        return status;
    }
}
