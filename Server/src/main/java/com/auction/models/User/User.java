package com.auction.models.User;
import at.favre.lib.crypto.bcrypt.BCrypt;
import com.auction.enums.UserRole;
import com.auction.models.Entity.Entity;


public abstract class User extends Entity {
    private String username;
    private String password;
    private String email;
    private com.auction.enums.UserRole role;

    User(String username, String email, String password, UserRole role){
        this.email=email;
        this.password=password;
        this.username=username;
        this.role=role;
    }

    // Thêm constructor cho load từ DB
    User(String id, String username, String email, String password, UserRole role){
        super(id);
        this.email = email;
        this.password = password;
        this.username = username;
        this.role = role;
    }

    public boolean checkPassword(String plainPasswordInput) {
        BCrypt.Result result = BCrypt.verifyer().verify(plainPasswordInput.toCharArray(), password);
        return result.verified;
    }

    public String getRole(){
        return this.role.toString();
    }

    public com.auction.enums.UserRole getUserRole(){return this.role;}

    public String getUsername() {
        return this.username;
    }

    public String getEmail() {
        return this.email;
    }
}
