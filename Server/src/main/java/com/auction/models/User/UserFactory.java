package com.auction.models.User;

import java.util.HashMap;
import java.util.Map;

public abstract class UserFactory {
    private static final Map<com.auction.enums.UserRole, UserFactory> registry = new HashMap<>();

    //Giao nó cho một lớp Khởi tạo hệ thống (hoặc hàm main khi Server vừa bật lên) để tránh deadlock
    /*static {
        registry.put(com.auction.enums.UserRole.BIDDER, new BidderFactory());
        registry.put(com.auction.enums.UserRole.SELLER, new SellerFactory());
        registry.put(com.auction.enums.UserRole.ADMIN, new AdminFactory());
    }*/

    // Factory Method: Các lớp con sẽ triển khai logic khởi tạo riêng [cite: 29]
    abstract <T extends User> T createInstance(String username, String email, String password);

    /**
     * Hàm tạo User tổng quát sử dụng Generics [cite: 62]
     * @param role: Đối tượng Enum UserRole
     * @return Trả về đúng kiểu subclass (Bidder, Seller...) mà không cần ép kiểu thủ công
     */
    @SuppressWarnings("unchecked")
    public static <T extends User> T createUser(com.auction.enums.UserRole role, String username, String email, String password ) {
        UserFactory factory = registry.get(role);

        if (factory == null) {
            throw new IllegalArgumentException("Role chưa được đăng ký trong hệ thống: " + role);
        }

        // Trả về đúng kiểu dữ liệu cụ thể nhờ Generics [cite: 61]
        return (T) factory.createInstance(username, email, password);
    }
}
