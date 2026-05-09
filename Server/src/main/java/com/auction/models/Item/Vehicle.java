package com.auction.models.Item;

public class Vehicle extends Item {
    private String model;
    private double kmage;
    private String licensePlate;
    private String engineType;
    private String sellerId;

    Vehicle(String name, double startingPrice, String description, int yearCreated, String model, String engineType, String licensePlate, double kmage,String sellerId){
        super(name, startingPrice, description,yearCreated,sellerId);
        this.engineType=engineType;
        this.model=model;
        this.kmage=kmage;
        this.licensePlate=licensePlate;
    }

    // Constructor load từ DB
    public Vehicle(String id, String name, double startingPrice, String description,
                   int yearCreated, String sellerId, String model,
                   String engineType, String licensePlate, double kmage) {
        super(id, name, startingPrice, description, yearCreated, sellerId);
        this.engineType = engineType;
        this.model = model;
        this.kmage = kmage;
        this.licensePlate = licensePlate;
    }

    @Override
    public String getInfo() {
        return String.format("[Phương tiện]\n" +
                "Tên: %s\n"+
                "Dòng xe: %s\n" +
                "Số km đã đi: %s\n" +
                "Động cơ: %s\n"+
                "Biển số: %s\n" +
                "Năm sản xuất: %d\n" +
                "Giá gốc: %f VNĐ\n", this.getName(), model, kmage,engineType,licensePlate,this.getYearCreated(), this.getStartingPrice());
    }
}
