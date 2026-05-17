package com.auction.network;

/**
 ClientSession là "Hồ sơ tạm thời" của Client đang kết nối

 Nhiệm vụ: quản lý
 - Client này đã đăng nhập chưa?
 - userId là gì?
 - username là gì?
 - role là BIDDER, SELLER hay ADMIN?
 - socket nào đang thuộc về client này?
 - client đang xem phiên đấu giá nào?
 - Giúp Server kiem tra phân quyền trước khi xử lí action
 */

import com.auction.enums.UserRole;

import java.io.PrintWriter;
import java.net.Socket;

public class ClientSession {
    private String userId; // Ban đầu null, sau khi login mới có giá trị
    private UserRole role;      // Server luu role để kiểm tra quyền

    private final Socket socket;
    private final PrintWriter out;

    public ClientSession(Socket socket, PrintWriter out) {
        this.socket = socket;
        this.out = out;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public UserRole getRole() {return role;}
    public void setRole(UserRole role) {this.role = role;}


    // Hàm này để ConnectionManage có thể gọi để bắn tin nhắn real-time về UI
    public void sendMessage(String jsonMessage) {
        if (out != null) {
            out.println(jsonMessage);
            out.flush();
        }
    }

    // 1 session được coi là đã login khi có cả userID và role
    public boolean isLoggedIn() {
        return userId != null && role != null;
    }

    // Xóa thông tin khi logout thành công
    public void clearLoginInfo() {
        this.userId = null;
        this.role = null;
    }

    // Đóng luồng
    public void close() {
        // logic try-catch close socket...
    }
}