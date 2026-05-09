package com.auction.models.Item;

import com.auction.models.User.Seller;

import java.util.Map;

public class ArtFactory extends ItemFactory{
    @Override
    protected Item createItem(Map<String, Object> data) {
        // Lấy dữ liệu
        String name = (String) data.get("name");
        double startingPrice = (double) data.get("startingPrice");
        String artist = (String) data.get("artist");
        int yearCreated = (int) data.get("yearCreated");
        String material = (String) data.get("material");
        String sellerId = (String) data.get("sellerId");

        String description = "Không có mô tả";
        //Tạo đối tượng Art
        if(data.containsKey("description")){
            description = (String) data.get("description");
        }
        return new Art(name, startingPrice, description,yearCreated,artist,material,sellerId);

    }
}
