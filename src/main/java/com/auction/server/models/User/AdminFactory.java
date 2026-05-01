package com.auction.server.models.User;

public class AdminFactory extends UserFactory{
    @Override
    protected Admin createInstance(String username, String email, String password) {
        return new Admin(username,email,password);
    }
}
