package com.auction.server.models.User;

public class SellerFactory extends UserFactory {
    @Override
    <T extends User> T createInstance(String username, String email, String password) {
        return (T) new Seller(username, email, password);
    }
}
