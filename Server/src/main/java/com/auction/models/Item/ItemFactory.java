package com.auction.models.Item;

import com.auction.enums.ItemType;

import java.util.HashMap;
import java.util.Map;

public abstract class ItemFactory {
    private static final Map<ItemType, ItemFactory> registry = new HashMap<>();

    public static void register(ItemType type, ItemFactory factory) {
        registry.put(type, factory);
    }

    protected abstract Item createItem(Map<String, Object> data);

    public static Item createItem(ItemType type, Map<String, Object> data) {
        ItemFactory factory = registry.get(type);
        if (factory == null) {
            throw new IllegalArgumentException("Lỗi: Không hỗ trợ loại vật phẩm [" + type + "]");
        }
        return factory.createItem(data);
    }

    /**
     * Parses socket/JSON item type strings (including legacy "VEHICLE") into {@link ItemType}.
     */
    public static ItemType parseItemType(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Item type must not be empty.");
        }

        String normalizedType = type.trim().toUpperCase();
        if ("VEHICLE".equals(normalizedType)) {
            normalizedType = "VEHICLES";
        }
        return ItemType.valueOf(normalizedType);
    }

    public static Item createItem(String type, Map<String, Object> data) {
        return createItem(parseItemType(type), data);
    }

    protected String getRequiredString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            throw new IllegalArgumentException("Lỗi: Thiếu thông tin bắt buộc hoặc để trống trường [" + key + "]");
        }
        return String.valueOf(value).trim();
    }

    protected double getRequiredDouble(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            throw new IllegalArgumentException("Lỗi: Thiếu thông tin bắt buộc trường [" + key + "]");
        }
        try {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Lỗi: Trường [" + key + "] sai định dạng số");
        }
    }

    protected int getRequiredInt(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            throw new IllegalArgumentException("Lỗi: Thiếu thông tin bắt buộc trường [" + key + "]");
        }
        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Lỗi: Trường [" + key + "] sai định dạng số nguyên");
        }
    }

    protected String getOptionalString(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return defaultValue;
        }
        return String.valueOf(value).trim();
    }

    protected double getOptionalDouble(Map<String, Object> data, String key, double defaultValue) {
        Object value = data.get(key);
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return defaultValue;
        }
        try {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    protected int getOptionalInt(Map<String, Object> data, String key, int defaultValue) {
        Object value = data.get(key);
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return defaultValue;
        }
        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
