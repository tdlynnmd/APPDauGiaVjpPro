package com.auction;

import com.auction.exception.AuthenticationException;
import com.auction.enums.UserRole;
import com.auction.network.SocketServer;
import com.auction.service.AuthService;

public class Main {

    public static void main(String[] args) {
        System.out.println("=== HỆ THỐNG SERVER ===");
        System.out.println("[Server] Đang khởi động...");

        seedUsersForTesting();

        SocketServer socketServer = new SocketServer();
        socketServer.start();
    }

    private static void seedUsersForTesting() {
        AuthService authService = new AuthService();

        registerTestUser(
                authService,
                "admin1",
                "Admin@123",
                "admin1@auction.com",
                UserRole.ADMIN
        );

        registerTestUser(
                authService,
                "bidder1",
                "Bidder@123",
                "bidder1@auction.com",
                UserRole.BIDDER
        );

        registerTestUser(
                authService,
                "seller1",
                "Seller@123",
                "seller1@auction.com",
                UserRole.SELLER
        );

        System.out.println("[Server] Hoàn tất tạo tài khoản test.");
        System.out.println("[Server] Tài khoản test:");
        System.out.println("[Server] admin1 / Admin@123");
        System.out.println("[Server] bidder1 / Bidder@123");
        System.out.println("[Server] seller1 / Seller@123");
    }

    private static void registerTestUser(
            AuthService authService,
            String username,
            String password,
            String email,
            UserRole role
    ) {
        try {
            authService.register(username, password, email, role);
            System.out.println("[Server] Tạo user test thành công: " + username);
        } catch (AuthenticationException e) {
            System.out.println("[Server] Không tạo được user " + username + ": " + e.getMessage());
        }
    }
}
