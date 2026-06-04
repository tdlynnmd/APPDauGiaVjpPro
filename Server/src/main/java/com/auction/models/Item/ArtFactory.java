package com.auction.models.Item;

import java.util.Map;

/**
 * Lớp biểu diễn thực thể ArtFactory trong hệ thống.
 */
public class ArtFactory extends ItemFactory {

    @Override
    protected Item createItem(Map<String, Object> data) {
        String name = getRequiredString(data, "name");
        double startingPrice = getRequiredDouble(data, "startingPrice");
        int yearCreated = getRequiredInt(data, "yearCreated");
        String sellerId = getRequiredString(data, "sellerId");

        String painter = getRequiredString(data, "painter");
        String artStyle = getRequiredString(data, "artStyle");

        String description = getOptionalString(data, "description", "Không có mô tả");
        String imageUrl = getOptionalString(data, "imageUrl", "default_art.png");

        return new Art(name, startingPrice, description, yearCreated,
                sellerId, imageUrl, painter, artStyle);
    }
}