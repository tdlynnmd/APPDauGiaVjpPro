package com.auction.models.Item;

import java.util.Map;

/**
 * Lớp biểu diễn thực thể VehicleFactory trong hệ thống.
 */
public class VehicleFactory extends ItemFactory {

    @Override
    protected Item createItem(Map<String, Object> data) {
        String name = getRequiredString(data, "name");
        double startingPrice = getRequiredDouble(data, "startingPrice");
        int yearCreated = getRequiredInt(data, "yearCreated");
        String sellerId = getRequiredString(data, "sellerId");

        String model = getRequiredString(data, "model");
        String engineType = getRequiredString(data, "engineType");
        String licensePlate = getRequiredString(data, "licensePlate");

        double kmage = getRequiredDouble(data, "kmAge");

        String description = getOptionalString(data, "description", "Không có mô tả");
        String imageUrl = getOptionalString(data, "imageUrl", "default_vehicle.png");

        return new Vehicle(name, startingPrice, description, yearCreated,
                sellerId, imageUrl, model, engineType, licensePlate, kmage);
    }
}