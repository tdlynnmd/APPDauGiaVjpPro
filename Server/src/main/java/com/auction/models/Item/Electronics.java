package com.auction.models.Item;

import com.auction.enums.ItemStatus;
import com.auction.enums.ItemType;
import java.time.LocalDateTime;

public class Electronics extends Item {
    private String brand;
    private int warrantyMonths;

    // Constructor 1: Tạo mới
    public Electronics(String name, double startingPrice, String description, int yearCreated,
                       String sellerId, String imageUrl, String brand, int warrantyMonths) {
        // Tự động truyền ItemType.ELECTRONICS
        super(name, startingPrice, description, yearCreated, sellerId, ItemType.ELECTRONICS, imageUrl);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    // Constructor 2: Load từ Database
    public Electronics(String id, String name, double startingPrice, String description,
                       int yearCreated, String sellerId, String imageUrl, ItemStatus status,
                       LocalDateTime createdAt, String brand, int warrantyMonths) {
        // Tự động truyền ItemType.ELECTRONICS
        super(id, name, startingPrice, description, yearCreated, sellerId, ItemType.ELECTRONICS, imageUrl, status, createdAt);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public String getInfo() {
        return String.format("[Điện tử]\n" +
                        "Tên: %s\n" +
                        "Thương hiệu: %s\n" +
                        "Năm sản xuất: %d\n" +
                        "Thời gian BH: %d tháng\n" +
                        "Giá khởi điểm: %,.0f VNĐ\n" +
                        "Trạng thái: %s\n",
                this.getName(), brand, this.getYearCreated(), warrantyMonths, this.getStartingPrice(), this.getStatus());
    }

    public String getBrand() { return brand; }
    public int getWarrantyMonths() { return warrantyMonths; }
}