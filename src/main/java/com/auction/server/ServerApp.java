package com.auction.server;

import com.auction.dto.UserDTO;
import com.auction.server.exception.AuthenticationException;
import com.auction.server.models.User.UserRole;
import com.auction.server.service.AuthService;

public class ServerApp {
    static void main() {
        System.out.println("=== HỆ THỐNG SERVER ===");
        System.out.println("[Server] Đang khởi động...");
        System.out.println("[Server] Bếp trưởng đã sẵn sàng. Chưa có Socket nên tạm thời đứng chơi.");

        /*// Đoạn code này giúp Server chạy liên tục không bị tắt phụt đi
        try {
            Thread.sleep(60000); // Ngủ 60 giây
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

        //Test logic login
        AuthService authService = new AuthService();

        try {
            // Test Register
            UserDTO bidderDTO1 = authService.register(
                    "john_doe",
                    "123123fA@",
                    "john@auction.com",
                    UserRole.BIDDER
            );
            System.out.println("✅ Register success: " + bidderDTO1.getUsername());

            // Test Login (cùng username/password)
            UserDTO bidderDTO2 = authService.login(
                    "john_doe",
                    "123123fA@"
            );
            System.out.println("✅ Login success: " + bidderDTO2.getUsername());

            // Test Login (sai password)
            try {
                authService.login("john_doe", "SecurePass@123");
                System.out.println("✅ Login success");
                authService.logout(bidderDTO2.getId());
                System.out.println("✅ Logout success");
            } catch (AuthenticationException e) {
                System.out.println("❌ Login failed: " + e.getMessage());
            }

        } catch (AuthenticationException e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
    }
}
