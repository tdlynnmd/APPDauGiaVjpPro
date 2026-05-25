package com.auction;

import com.auction.exception.AuthenticationException;
import com.auction.enums.UserRole;
import com.auction.manage.LiveRoomManage;
import com.auction.models.Item.ArtFactory;
import com.auction.models.Item.ElectronicsFactory;
import com.auction.models.Item.ItemFactory;
import com.auction.models.Item.VehicleFactory;
import com.auction.models.User.AdminFactory;
import com.auction.models.User.BidderFactory;
import com.auction.models.User.SellerFactory;
import com.auction.network.SocketServer;
import com.auction.event.AuctionEventBus;
import com.auction.service.AuthService;

import static com.auction.models.User.UserFactory.setRegistry;

public class Main {

    public static void main(String[] args) {

        setRegistry(com.auction.enums.UserRole.BIDDER, new BidderFactory());
        setRegistry(com.auction.enums.UserRole.SELLER, new SellerFactory());
        setRegistry(com.auction.enums.UserRole.ADMIN, new AdminFactory());
        registerItemFactories();

        System.out.println("=== HỆ THỐNG SERVER ===");
        System.out.println("[Server] Đang khởi động...");

        // ✨ THÊM CODE NÀY ✨
        // 1. Khởi tạo EventBus
        AuctionEventBus eventBus = AuctionEventBus.getInstance();
        System.out.println("[Main] ✅ AuctionEventBus được khởi tạo");

        // 2. Khởi tạo LiveRoomManage
        LiveRoomManage liveRoomManager = LiveRoomManage.getInstance();
        System.out.println("[Main] ✅ LiveRoomManage được khởi tạo");

        // 3. QUAN TRỌNG: Đăng ký LiveRoomManage vào EventBus
        eventBus.attach(liveRoomManager);
        System.out.println("[Main] ✅ LiveRoomManage đã attach vào EventBus");

        seedUsersForTesting();

        SocketServer socketServer = new SocketServer();
        socketServer.start();
    }

    /**
     * Registers concrete item factories once when the server starts.
     */
    private static void registerItemFactories() {
        ItemFactory.register("ART", new ArtFactory());
        ItemFactory.register("ELECTRONICS", new ElectronicsFactory());
        ItemFactory.register("VEHICLES", new VehicleFactory());
        ItemFactory.register("VEHICLE", new VehicleFactory());
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
