package com.auction.dto;

public class LoginRequest {
    private String usernameOrEmail;
    private String password;

    public LoginRequest(){
    }
    public LoginRequest(String usernameOrEmail, String password){
        this.usernameOrEmail = usernameOrEmail;
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public String getUsernameOrEmail() {
        return usernameOrEmail;
    }
}
