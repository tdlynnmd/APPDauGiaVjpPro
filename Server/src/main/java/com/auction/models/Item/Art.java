package com.auction.models.Item;

public class Art extends Item {
    private String artist;
    private String material;

    Art(String name,double startingPrice,String description, int yearCreated,String artist,String material){
        super(name, startingPrice, description,yearCreated);
        this.artist = artist;
        this.material = material;
    }

    @Override
    public String getInfo() {
        return String.format("[Nghệ thuật]\n" +
                "Tên: %s\n" +
                "Tác giả: %s\n" +
                "Chất liệu: %s\n" +
                "Năm tạo ra: %d\n" +
                "Giá gốc: %f VNĐ\n", this.getName(), artist, material, this.getYearCreated(), this.getStartingPrice());
    }
}
