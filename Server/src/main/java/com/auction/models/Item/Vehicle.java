package com.auction.models.Item;

import com.auction.enums.ItemStatus;
import com.auction.enums.ItemType;
import java.time.LocalDateTime;

/**
 * Lớp biểu diễn thực thể Vehicle trong hệ thống.
 */
public class Vehicle extends Item {
    private String model;
    private double kmage;
    private String licensePlate;
    private String engineType;

    public Vehicle(String name, double startingPrice, String description, int yearCreated,
                   String sellerId, String imageUrl, String model, String engineType,
                   String licensePlate, double kmage) {
        super(name, startingPrice, description, yearCreated, sellerId, ItemType.VEHICLES, imageUrl);
        this.model = model;
        this.engineType = engineType;
        this.licensePlate = licensePlate;
        this.kmage = kmage;
    }

    public Vehicle(String id, String name, double startingPrice, String description,
                   int yearCreated, String sellerId, String imageUrl, ItemStatus status,
                   LocalDateTime createdAt, String model, String engineType,
                   String licensePlate, double kmage) {
        super(id, name, startingPrice, description, yearCreated, sellerId, ItemType.VEHICLES, imageUrl, status, createdAt);
        this.model = model;
        this.engineType = engineType;
        this.licensePlate = licensePlate;
        this.kmage = kmage;
    }

    @Override
    public String getInfo() {
        return String.format("[Phương tiện]\n" +
                        "Tên: %s\n" +
                        "Dòng xe: %s\n" +
                        "Số km đã đi: %,.1f km\n" +
                        "Động cơ: %s\n" +
                        "Biển số: %s\n" +
                        "Năm sản xuất: %d\n" +
                        "Giá khởi điểm: %,.0f VNĐ\n" +
                        "Trạng thái: %s\n",
                this.getName(), model, kmage, engineType, licensePlate, this.getYearCreated(), this.getStartingPrice(), this.getStatus());
    }

    public String getModel() { return model; }
    public double getKmage() { return kmage; }
    public String getLicensePlate() { return licensePlate; }
    public String getEngineType() { return engineType; }

    public double getKmAge() {
        return kmage;
    }

    public void setModel(String model) { this.model = model; }
    public void setKmage(double kmage) { this.kmage = kmage; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
    public void setEngineType(String engineType) { this.engineType = engineType;
    }

    public void setKmAge(double kmAge) { this.kmage = kmAge;
    }
}