package com.auction.server.models.User;
import com.auction.server.models.Entity.Entity;

public abstract class User extends Entity {
    private String username;
    private String password;
    private String email;

    User(String username, String password, String email){
        this.email=email;
        this.password=password;
        this.username=username;
    }

    public boolean checkpassword(String input){
        return this.password.equals(input);
    }

    public abstract String getRole();
}
