package com.auction.service;

import com.auction.dto.*;

public class SessionManager {
    // Sử dụng Singleton Pattern để truy cập phiên làm việc ở mọi nơi trong Client
    private static SessionManager instance;

    // Lưu trữ duy nhất một UserDTO đang đăng nhập trên máy khách này
    // Sử dụng DTO thay vì User entity để bảo vệ thông tin nhạy cảm (password, etc.)
    private UserDTO currentUserDTO;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // Logic nghiệp vụ: Thiết lập phiên làm việc sau khi login thành công (sử dụng UserDTO)
    public void setCurrentUser(UserDTO userDTO) {
        this.currentUserDTO = userDTO;
    }

    public UserDTO getCurrentUser() {
        return currentUserDTO;
    }

    // Convenience methods để lấy DTO cụ thể theo role
    public BidderDTO getCurrentBidder() {
        if (currentUserDTO instanceof BidderDTO) {
            return (BidderDTO) currentUserDTO;
        }
        return null;
    }

    public SellerDTO getCurrentSeller() {
        if (currentUserDTO instanceof SellerDTO) {
            return (SellerDTO) currentUserDTO;
        }
        return null;
    }

    public AdminDTO getCurrentAdmin() {
        if (currentUserDTO instanceof AdminDTO) {
            return (AdminDTO) currentUserDTO;
        }
        return null;
    }

    // Kiểm tra trạng thái đã đăng nhập hay chưa để điều hướng UI
    public boolean isLoggedIn() {
        return currentUserDTO != null;
    }

    // Lấy ID người dùng hiện tại
    public String getCurrentUserId() {
        return currentUserDTO != null ? currentUserDTO.getId() : null;
    }

    // Logic nghiệp vụ đăng xuất: Xóa sạch thông tin phiên
    public void clearSession() {
        this.currentUserDTO = null;
        System.out.println("Đã xóa phiên làm việc cục bộ.");
    }
}
