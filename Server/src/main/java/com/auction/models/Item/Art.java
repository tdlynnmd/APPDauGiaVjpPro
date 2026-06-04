package com.auction.models.Item;

import com.auction.enums.ItemStatus;
import com.auction.enums.ItemType;
import java.time.LocalDateTime;

/**
 * Lớp biểu diễn thực thể Art trong hệ thống.
 */
public class Art extends Item {
    private String painter;
    private String artStyle;

    public Art(String name, double startingPrice, String description, int yearCreated,
               String sellerId, String imageUrl, String painter, String artStyle) {
        super(name, startingPrice, description, yearCreated, sellerId, ItemType.ART, imageUrl);
        this.painter = painter;
        this.artStyle = artStyle;
    }

    public Art(String id, String name, double startingPrice, String description,
               int yearCreated, String sellerId, String imageUrl, ItemStatus status,
               LocalDateTime createdAt, String painter, String artStyle) {
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

    public String getPainter() { return painter; }
    public String getArtStyle() { return artStyle; }

    public void setPainter(String painter) { this.painter = painter;
    }

    public void setArtStyle(String artStyle) { this.artStyle = artStyle;
    }
}