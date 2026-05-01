package com.auction.client.service;

import com.auction.server.models.User.User;

public class SessionManager {
    // Sử dụng Singleton Pattern để truy cập phiên làm việc ở mọi nơi trong Client
    private static SessionManager instance;

    // Lưu trữ duy nhất một User đang đăng nhập trên máy khách này [cite: 1494, 2938]
    private User currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // Logic nghiệp vụ: Thiết lập phiên làm việc sau khi login thành công
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    // Kiểm tra trạng thái đã đăng nhập hay chưa để điều hướng UI
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    // Logic nghiệp vụ đăng xuất: Xóa sạch thông tin phiên
    public void clearSession() {
        this.currentUser = null;
        System.out.println("Đã xóa phiên làm việc cục bộ.");
    }
}
