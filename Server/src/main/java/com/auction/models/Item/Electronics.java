package com.auction.models.Item;

public class Electronics extends Item {
    private String brand;
    private int warrantyMonths;

    public Electronics(String name,double startingPrice, String description,int yearCreated, String brand, int warrantyMonths,String sellerId){
        super(name,startingPrice,description,yearCreated,sellerId);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    // Constructor load từ DB
    public Electronics(String id, String name, double startingPrice, String description,
                       int yearCreated, String sellerId, String brand, int warrantyMonths) {
        super(id, name, startingPrice, description, yearCreated, sellerId);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }


    @Override
    public String getInfo() {
        return String.format("[Điện tử]\n" +
                "Tên: %s\n" +
                "Thương hiệu: %s\n" +
                "Năm sản xuất: %d\n"+
                "Thời gian BH: %d tháng\n"+
                "Giá gốc: %f VNĐ\n",this.getName(),brand,this.getYearCreated(), warrantyMonths,this.getStartingPrice());
    }
}
