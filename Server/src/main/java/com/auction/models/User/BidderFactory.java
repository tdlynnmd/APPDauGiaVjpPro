package com.auction.models.User;

/**
 * Lớp biểu diễn thực thể BidderFactory trong hệ thống.
 */
public class BidderFactory extends UserFactory {
    @Override
    <T extends User> T createInstance(String username, String email, String password) {
        return (T) new Bidder(username, email, password);
    }
}
