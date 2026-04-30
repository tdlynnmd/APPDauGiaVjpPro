package com.auction.server.models.User;
import com.auction.server.models.Entity.Entity;

public abstract class User extends Entity {
    private String username;
    private String password;
    private String email;
    private UserRole role;

    User(String username, String email, String password, UserRole role){
        this.email=email;
        this.password=password;
        this.username=username;
        this.role=role;
    }

    //30/4/26 cần xem xét lại tạo UserFactory ko
    public abstract <Type> Type createUser(String username, String password, String email, UserRole role);

    public boolean checkpassword(String input){
        return this.password.equals(input);
    }

    public String getRole(){
        return this.role.toString();
    };

    public String getUsername() {
        return this.username;
    }

    public String getEmail() {
        return this.email;
    }
}
