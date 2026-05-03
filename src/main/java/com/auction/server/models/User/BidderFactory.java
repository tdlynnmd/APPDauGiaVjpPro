package com.auction.server.models.User;

public class BidderFactory extends UserFactory {
    @Override
    <T extends User> T createInstance(String username, String email, String password) {
        return (T) new Bidder(username, email, password);
    }
}
