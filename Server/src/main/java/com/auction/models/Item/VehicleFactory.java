package com.auction.models.Item;

import java.util.Map;

public class VehicleFactory extends ItemFactory {

    @Override
    protected Item createItem(Map<String, Object> data) {
        // 1. Các trường BẮT BUỘC chung
        String name = getRequiredString(data, "name");
        double startingPrice = getRequiredDouble(data, "startingPrice");
        int yearCreated = getRequiredInt(data, "yearCreated");
        String sellerId = getRequiredString(data, "sellerId");

        // Các trường bắt buộc riêng của Vehicle
        String model = getRequiredString(data, "model");
        String engineType = getRequiredString(data, "engineType");
        String licensePlate = getRequiredString(data, "licensePlate");

        // kmAge thường là số thập phân, ví dụ: 15000.5 km
        double kmage = getRequiredDouble(data, "kmAge");

        // 2. Các trường TÙY CHỌN
        String description = getOptionalString(data, "description", "Không có mô tả");
        String imageUrl = getOptionalString(data, "imageUrl", "default_vehicle.png");

        // 3. Tạo Object an toàn
        // (Lưu ý truyền đúng thứ tự Constructor của lớp Vehicle)
        return new Vehicle(name, startingPrice, description, yearCreated,
                sellerId, imageUrl, model, engineType, licensePlate, kmage);
    }
}