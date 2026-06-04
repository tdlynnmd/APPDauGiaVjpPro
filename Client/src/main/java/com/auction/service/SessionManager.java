package com.auction.service;

import com.auction.dto.*;

/**
 * Lớp quản lý thông tin phiên làm việc hiện tại của người dùng phía Client.
 */
public class SessionManager {
    private static SessionManager instance;

    private UserDTO currentUserDTO;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentUser(UserDTO userDTO) {
        this.currentUserDTO = userDTO;
    }

    public UserDTO getCurrentUser() {
        return currentUserDTO;
    }

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

    public boolean isLoggedIn() {
        return currentUserDTO != null;
    }

    public String getCurrentUserId() {
        return currentUserDTO != null ? currentUserDTO.getId() : null;
    }

    public void clearSession() {
        this.currentUserDTO = null;
        System.out.println("Đã xóa phiên làm việc cục bộ.");
    }
}
