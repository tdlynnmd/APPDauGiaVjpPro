package com.auction.models.Item;

import java.util.Map;

/**
 * Lớp biểu diễn thực thể ElectronicsFactory trong hệ thống.
 */
public class ElectronicsFactory extends ItemFactory {

    @Override
    protected Item createItem(Map<String, Object> data) {
        String name = getRequiredString(data, "name");
        double startingPrice = getRequiredDouble(data, "startingPrice");
        int yearCreated = getRequiredInt(data, "yearCreated");
        String sellerId = getRequiredString(data, "sellerId");

        String brand = getRequiredString(data, "brand");
        int warrantyMonths = getRequiredInt(data, "warrantyMonths");

        String description = getOptionalString(data, "description", "Không có mô tả");
        String imageUrl = getOptionalString(data, "imageUrl", "default_electronics.png");

        return new Electronics(name, startingPrice, description, yearCreated,
                sellerId, imageUrl, brand, warrantyMonths);
    }
}