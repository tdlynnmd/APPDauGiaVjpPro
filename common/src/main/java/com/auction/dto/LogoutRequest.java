package com.auction.dto;

/**
 * DTO gửi yêu cầu đăng xuất khỏi hệ thống.
 */
public class LogoutRequest {
    private String userID;
    public LogoutRequest(){
    }
    public LogoutRequest(String userID){
        this.userID = userID;
    }
    public String getUserID(){
        return userID;
    }
}
