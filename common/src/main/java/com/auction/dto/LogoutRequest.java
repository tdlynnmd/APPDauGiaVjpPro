package com.auction.dto;

/**
 Chứa thông tin đăng xuất Client gửi lên Server
 Mang userId của người đang đăng nhập
 Giúp Server biết user nào cần Logout
 Hỗ trợ trường hợp ClientSession phía Server bị thiếu userId.
 */

public class LogoutRequest {
    private String userID;      // ID của user hiện tại đang đăng nhập
    public LogoutRequest(){
    }
    public LogoutRequest(String userID){
        this.userID = userID;
    }
    public String getUserID(){
        return userID;
    }
}
