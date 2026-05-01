package com.auction.server.models.User;

import java.util.HashMap;
import java.util.Map;

public abstract class UserFactory<T extends User> {
    // Registry sử dụng UserRole làm Key thay vì String [cite: 52]
    private static final Map<UserRole, UserFactory<?>> registry = new HashMap<>();

    public static void register(UserRole role, UserFactory<?> factory) {
        registry.put(role, factory);
    }

    // Factory Method: Các lớp con sẽ triển khai logic khởi tạo riêng [cite: 29]
    protected abstract T createInstance(String username, String email, String password);

    /**
     * Hàm tạo User tổng quát sử dụng Generics [cite: 62]
     * @param role: Đối tượng Enum UserRole
     * @return Trả về đúng kiểu subclass (Bidder, Seller...) mà không cần ép kiểu thủ công
     */
    @SuppressWarnings("unchecked")
    public static <U extends User> U createUser(UserRole role, String username, String email, String password ) {
        UserFactory<?> factory = registry.get(role);

        if (factory == null) {
            throw new IllegalArgumentException("Role chưa được đăng ký trong hệ thống: " + role);
        }

        // Trả về đúng kiểu dữ liệu cụ thể nhờ Generics [cite: 61]
        return (U) factory.createInstance(username, email, password);
    }
}
