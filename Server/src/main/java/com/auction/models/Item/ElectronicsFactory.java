package com.auction.models.Item;

import java.util.Map;

public class ElectronicsFactory extends ItemFactory {
    @Override
    protected Item createItem(Map<String, Object> data) {
        // Lấy dữ liệu
        String name = (String) data.get("name");
        double startingPrice = (double) data.get("startingPrice");
        String brand = (String) data.get("brand");
        int yearCreated = (int) data.get("yearCreated");
        int warrantyMonths = (int) data.get("warrantyMonths");
        String sellerId = (String) data.get("sellerId");

        String description = "Không có mô tả";
        //Tạo đối tượng Electronics
        if(data.containsKey("description")) {
            description = (String) data.get("description");
        }
        return new Electronics(name,startingPrice, description,yearCreated,brand, warrantyMonths,sellerId);
    }
}
