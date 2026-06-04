package com.auction.models.User;

/**
 * Lớp biểu diễn thực thể SellerFactory trong hệ thống.
 */
public class SellerFactory extends UserFactory {
    @Override
    <T extends User> T createInstance(String username, String email, String password) {
        return (T) new Seller(username, email, password);
    }
}
