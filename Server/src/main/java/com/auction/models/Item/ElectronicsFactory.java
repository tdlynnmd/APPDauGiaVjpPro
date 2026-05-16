package com.auction.models.Item;

import java.util.Map;

public class ElectronicsFactory extends ItemFactory {

    @Override
    protected Item createItem(Map<String, Object> data) {
        // 1. Các trường BẮT BUỘC chung
        String name = getRequiredString(data, "name");
        double startingPrice = getRequiredDouble(data, "startingPrice");
        int yearCreated = getRequiredInt(data, "yearCreated");
        String sellerId = getRequiredString(data, "sellerId");

        // Các trường bắt buộc riêng của Electronics
        String brand = getRequiredString(data, "brand");
        int warrantyMonths = getRequiredInt(data, "warrantyMonths"); // Bảo hành tính bằng tháng

        // 2. Các trường TÙY CHỌN
        String description = getOptionalString(data, "description", "Không có mô tả");
        String imageUrl = getOptionalString(data, "imageUrl", "default_electronics.png");

        // 3. Tạo Object an toàn
        return new Electronics(name, startingPrice, description, yearCreated,
                sellerId, imageUrl, brand, warrantyMonths);
    }
}