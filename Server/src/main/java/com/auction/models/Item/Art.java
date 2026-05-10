package com.auction.models.Item;

import com.auction.enums.ItemStatus;
import com.auction.enums.ItemType;
import java.time.LocalDateTime;

public class Art extends Item {
    private String painter;
    private String artStyle;

    // Constructor 1: Tạo mới
    public Art(String name, double startingPrice, String description, int yearCreated,
               String sellerId, String imageUrl, String painter, String artStyle) {
        // Tự động truyền ItemType.ART lên lớp cha
        super(name, startingPrice, description, yearCreated, sellerId, ItemType.ART, imageUrl);
        this.painter = painter;
        this.artStyle = artStyle;
    }

    // Constructor 2: Load từ Database
    public Art(String id, String name, double startingPrice, String description,
               int yearCreated, String sellerId, String imageUrl, ItemStatus status,
               LocalDateTime createdAt, String painter, String artStyle) {
        // Tự động truyền ItemType.ART lên lớp cha
        super(id, name, startingPrice, description, yearCreated, sellerId, ItemType.ART, imageUrl, status, createdAt);
        this.painter = painter;
        this.artStyle = artStyle;
    }

    @Override
    public String getInfo() {
        return String.format("[Nghệ thuật]\n" +
                        "Tên: %s\n" +
                        "Họa sĩ/Tác giả: %s\n" +
                        "Phong cách: %s\n" +
                        "Năm sáng tác: %d\n" +
                        "Giá khởi điểm: %,.0f VNĐ\n" +
                        "Trạng thái: %s\n",
                this.getName(), painter, artStyle, this.getYearCreated(), this.getStartingPrice(), this.getStatus());
    }

    // Getters & Setters cho các field riêng (nếu cần)
    public String getPainter() { return painter; }
    public String getArtStyle() { return artStyle; }
}