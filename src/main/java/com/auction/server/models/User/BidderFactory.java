package com.auction.server.models.User;

public class BidderFactory extends UserFactory {
    @Override
    protected  Bidder createInstance(String username, String email, String password) {
        return new Bidder(username,email,password);
    }
}
