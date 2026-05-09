package com.auction.models.Item;

import java.util.Map;

public class ArtFactory extends ItemFactory{
    @Override
    protected Item createItem(Map<String, Object> data) {
        // Lấy dữ liệu
        String name = (String) data.get("name");
        String description = (String) data.get("description");
        double startingPrice = (double) data.get("startingPrice");
        String artist = (String) data.get("artist");
        int yearCreated = (int) data.get("yearCreated");
        String material = (String) data.get("material");
        //Tạo đối tượng Art
        return new Art(name, startingPrice, description,yearCreated,material, artist);
    }
}
