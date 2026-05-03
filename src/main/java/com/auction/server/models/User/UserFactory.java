package com.auction.server.models.User;

import com.auction.server.manage.AuctionManage;

import java.util.HashMap;
import java.util.Map;

public abstract class UserFactory {
    private static final Map<UserRole, UserFactory> registry = new HashMap<>();

    static {
        registry.put(UserRole.BIDDER, new BidderFactory());
        registry.put(UserRole.SELLER, new SellerFactory());
        registry.put(UserRole.ADMIN, new AdminFactory());
    }

    // Factory Method: Các lớp con sẽ triển khai logic khởi tạo riêng [cite: 29]
    abstract <T extends User> T createInstance(String username, String email, String password);

    /**
     * Hàm tạo User tổng quát sử dụng Generics [cite: 62]
     * @param role: Đối tượng Enum UserRole
     * @return Trả về đúng kiểu subclass (Bidder, Seller...) mà không cần ép kiểu thủ công
     */
    @SuppressWarnings("unchecked")
    public static <T extends User> T createUser(UserRole role, String username, String email, String password ) {
        UserFactory factory = registry.get(role);

        if (factory == null) {
            throw new IllegalArgumentException("Role chưa được đăng ký trong hệ thống: " + role);
        }

        // Trả về đúng kiểu dữ liệu cụ thể nhờ Generics [cite: 61]
        return (T) factory.createInstance(username, email, password);
    }
}
