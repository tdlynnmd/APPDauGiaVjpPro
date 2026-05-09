package com.auction.models.Item;

import java.util.Map;

public class VehicleFactory extends ItemFactory{
    @Override
    protected Item createItem(Map<String, Object> data) {
        // Lấy dữ liệu
        String name = (String) data.get("name");
        String description = (String) data.get("description");
        double startingPrice = (double) data.get("startingPrice");
        int yearCreated = (int) data.get("yearCreated");
        String model = (String) data.get("model");
        double kmage = (double) data.get("kmage");
        String licensePlate = (String) data.get("licensePlate");
        String engineType = (String) data.get("engineType");

        //Tạo đối tượng Art
        return new Vehicle(name, startingPrice, description,yearCreated,model, engineType,licensePlate,kmage);
    }
}
