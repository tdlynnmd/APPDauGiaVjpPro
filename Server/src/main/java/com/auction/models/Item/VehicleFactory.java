package com.auction.models.Item;

import java.util.Map;

public class VehicleFactory extends ItemFactory{
    @Override
    protected Item createItem(Map<String, Object> data) {
        // Lấy dữ liệu
        String name = (String) data.get("name");
        double startingPrice = (double) data.get("startingPrice");
        int yearCreated = (int) data.get("yearCreated");
        String model = (String) data.get("model");
        double kmAge = (double) data.get("kmAge");
        String licensePlate = (String) data.get("licensePlate");
        String engineType = (String) data.get("engineType");
        String sellerId = (String) data.get("sellerId");
        String description = "Không có mô tả";

        //Tạo đối tượng Electronics
        if(data.containsKey("description")) {
            description = (String) data.get("description");
        }
        return new Vehicle(name, startingPrice, description,yearCreated,model, engineType,licensePlate,kmAge,sellerId);
    }
}
