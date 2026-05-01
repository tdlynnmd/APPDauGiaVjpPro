package com.auction.server.models.User;

public class SellerFactory extends UserFactory {
    @Override
    protected Seller createInstance(String username, String email, String password) {
        return new Seller(username,email,password);
    }
}
