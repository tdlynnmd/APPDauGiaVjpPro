package com.auction.server;

public class ServerApp {
    static void main() {
        System.out.println("=== HỆ THỐNG SERVER ===");
        System.out.println("[Server] Đang khởi động...");
        System.out.println("[Server] Bếp trưởng đã sẵn sàng. Chưa có Socket nên tạm thời đứng chơi.");

        // Đoạn code này giúp Server chạy liên tục không bị tắt phụt đi
        try {
            Thread.sleep(60000); // Ngủ 60 giây
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
