package com.auction.models.Item;

import java.util.Map;

public class ArtFactory extends ItemFactory {

    @Override
    protected Item createItem(Map<String, Object> data) {
        // 1. Các trường BẮT BUỘC (Sẽ văng IllegalArgumentException nếu sai/thiếu)
        String name = getRequiredString(data, "name");
        double startingPrice = getRequiredDouble(data, "startingPrice");
        int yearCreated = getRequiredInt(data, "yearCreated");
        String sellerId = getRequiredString(data, "sellerId");

        // Các trường bắt buộc riêng của Art
        String painter = getRequiredString(data, "painter");
        String artStyle = getRequiredString(data, "artStyle");

        // 2. Các trường TÙY CHỌN (Sẽ dùng giá trị mặc định nếu API không gửi)
        String description = getOptionalString(data, "description", "Không có mô tả");
        String imageUrl = getOptionalString(data, "imageUrl", "default_art.png");

        // 3. Tạo Object an toàn
        return new Art(name, startingPrice, description, yearCreated,
                sellerId, imageUrl, painter, artStyle);
    }
}