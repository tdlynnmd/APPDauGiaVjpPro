package com.auction.models.User;

/**
 * Lớp biểu diễn thực thể AdminFactory trong hệ thống.
 */
public class AdminFactory extends UserFactory{
    @Override
    protected Admin createInstance(String username, String email, String password) {
        return new Admin(username,email,password);
    }
}
