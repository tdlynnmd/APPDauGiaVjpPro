package com.auction.models.Item;

import java.util.HashMap;
import java.util.Map;

public abstract class ItemFactory {
    private static final Map<String, ItemFactory> registry = new HashMap<>();

    //Giao nó cho một lớp Khởi tạo hệ thống (hoặc hàm main khi Server vừa bật lên) để tránh deadlock
    /*static {
        registry.put("ART", new ArtFactory());
        registry.put("VEHICLE", new VehicleFactory());
        registry.put("ELECTRONICS", new ElectronicsFactory());
    }*/

    public static void register(String type, ItemFactory factory) {
        registry.put(type.toUpperCase(), factory);
    }

    // Factory method cốt lõi
    protected abstract Item createItem(Map<String, Object> data);

    public static Item createItem(String type, Map<String, Object> data) {
        ItemFactory factory = registry.get(type.toUpperCase());
        if (factory == null) {
            throw new IllegalArgumentException("Lỗi: Không hỗ trợ loại vật phẩm [" + type + "]");
        }
        return factory.createItem(data);
    }

    // =========================================================
    // CÁC HÀM TIỆN ÍCH VALIDATION (BẮT BUỘC - FAIL FAST)
    // =========================================================

    protected String getRequiredString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        // Kiểm tra null hoặc chuỗi chỉ chứa dấu cách
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
            // Xử lý trường hợp data đến từ thư viện JSON (như Gson/Jackson thường ép thành Number)
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            // Ép kiểu từ String
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

    // =========================================================
    // CÁC HÀM TIỆN ÍCH VALIDATION (TÙY CHỌN - SILENT FALLBACK)
    // =========================================================

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
            // Với optional, nếu họ nhập bậy (ví dụ "abc" vào ô giá) thì ta lấy giá trị mặc định
            // hoặc bạn có thể ném lỗi tùy vào mức độ khắt khe của hệ thống.
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