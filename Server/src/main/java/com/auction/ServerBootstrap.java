package com.auction;

import com.auction.enums.ItemType;
import com.auction.enums.UserRole;
import com.auction.event.AuctionEventBus;
import com.auction.exception.AuthenticationException;
import com.auction.manage.AuctionManage;
import com.auction.manage.ConnectionManage;
import com.auction.manage.LiveRoomManage;
import com.auction.models.Auction.Auction;
import com.auction.models.Item.ItemFactory;
import com.auction.models.User.AdminFactory;
import com.auction.models.User.BidderFactory;
import com.auction.models.User.SellerFactory;
import com.auction.dao.AuctionDAO;
import com.auction.dao.ItemDAO;
import com.auction.dao.impl.AuctionDAOImpl;
import com.auction.dao.impl.ItemDAOImpl;
import com.auction.service.AuctionService;
import com.auction.service.AuthService;
import com.auction.service.ItemService;
import com.auction.config.DatabaseConnection;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static com.auction.models.User.UserFactory.setRegistry;

/**
 * =========================================================================
 * ServerBootstrap - Tổng chỉ huy vòng đời khởi tạo và dọn dẹp hệ thống
 * =========================================================================
 */
public class ServerBootstrap {

    private final AuctionDAO auctionDAO = new AuctionDAOImpl();
    private final ItemDAO itemDAO = new ItemDAOImpl();

    public void start() {
        System.out.println("[Bootstrap] 🚀 Bắt đầu quy trình khởi tạo hệ thống chuyên nghiệp...");

        try {
            // 🔥 THÊM MỚI BƯỚC 0: Ép cả Server chạy chuẩn múi giờ Việt Nam, bất kể deploy ở Mỹ hay Singapore
            setupSystemTimezone();
            
            // Bước 1: Khởi tạo tất cả các Factory (User & Item)
            initializeFactories();

            // Bước 2: Khởi tạo kết nối Database Pool
            initializeDatabasePool();

            // Bước 3: Liên kết kiến trúc hướng sự kiện (Event-Driven Observers)
            wireInternalEventSystem();

            // Bước 4: Giải quyết sạch sẽ các phiên lỗi thời dưới MySQL trước khi nhấc dữ liệu lên bộ nhớ Cache
            cleanupExpiredAuctionsOnStartup();

            // Bước 5: Hydrate RAM (Nạp dữ liệu sống từ MySQL lên RAM)
            hydrateMemoryCache();

            // Bước 6: Bơm dữ liệu mẫu phục vụ kiểm thử (Seed Data)
            seedUsersForTesting();
            seedItemsForTesting();
            seedAuctionsForTesting();

            // Bước 7: KÍCH HOẠT SCHEDULER: Cho phép bộ máy quét thời gian thực trên RAM vào guồng chạy
            System.out.println("[Bootstrap] 7. Kích hoạt bộ quét vòng đời tự động trên RAM (Every 1 Second)...");
            AuctionManage.getInstance().startLifecycleMonitor();

            // Bước 8: Đăng ký khiên bảo vệ tối cao Graceful Shutdown Hook với cấu trúc giải phóng triệt để
            registerGracefulShutdownHook();

            System.out.println("[Bootstrap] 🎉 HẠ TẦNG SẴN SÀNG 100%! Server có thể mở cổng đón Socket kết nối.");

        } catch (Exception e) {
            System.err.println("[Bootstrap] 💥 KHỞI ĐỘNG THẤT BẠI! Hệ thống sẽ cưỡng chế dừng lại.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Bước 0: Đồng bộ múi giờ hệ thống
     */
    private void setupSystemTimezone() {
        System.out.println("[Bootstrap] 0. Thiết lập cấu hình múi giờ chuẩn hệ thống (Asia/Ho_Chi_Minh)...");
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    /**
     * 1. Gom toàn bộ logic đăng ký Factory vào đây
     */
    private void initializeFactories() {
        System.out.println("[Bootstrap] 1. Đăng ký các User và Item Factories vào hệ thống...");

        // Đăng ký User Factories
        setRegistry(UserRole.BIDDER, new BidderFactory());
        setRegistry(UserRole.SELLER, new SellerFactory());
        setRegistry(UserRole.ADMIN, new AdminFactory());

        ItemFactory.register(ItemType.ELECTRONICS, new com.auction.models.Item.ElectronicsFactory());
        ItemFactory.register(ItemType.ART, new com.auction.models.Item.ArtFactory());
        ItemFactory.register(ItemType.VEHICLES, new com.auction.models.Item.VehicleFactory());


        System.out.println("[Bootstrap]    -> Thành công: Hoàn tất cấu hình Polymorphic Factories.");
    }

    private void initializeDatabasePool() throws SQLException {
        System.out.println("[Bootstrap] 2. Khởi tạo kết nối Database Pool...");
        DatabaseConnection.getConnection();
    }

    private void wireInternalEventSystem() {
        System.out.println("[Bootstrap] 3. Tiến hành kết nối hệ thống Event-Driven...");
        AuctionEventBus.getInstance().attach(LiveRoomManage.getInstance());
    }

    /**
     * 🔥 HÀM MỚI: Quét dọn và tổng kết các phiên đấu giá bị quá hạn dưới DB do sập nguồn
     * Thực hiện ngay lúc bật Server để tránh nạp dữ liệu rác/quá hạn lên RAM.
     */
    private void cleanupExpiredAuctionsOnStartup() {
        System.out.println("[Bootstrap] 3.5 Đang kiểm tra cứu hộ các phiên đấu giá dính sự cố sập nguồn cũ...");

        // 1. Mò xuống DB hỏi: "Có ông RUNNING nào đáng lẽ phải kết thúc lúc Server đang sập không?"
        List<Auction> expiredAuctions = auctionDAO.findRunningAuctionsPastEndTime();

        if (expiredAuctions.isEmpty()) {
            System.out.println("[Bootstrap]    -> Tuyệt vời: Không có phiên đấu giá nào bị treo quá hạn.");
            return;
        }

        System.out.println("[Bootstrap]    🚨 Phát hiện " + expiredAuctions.size() + " phiên bị treo trạng thái RUNNING do sập nguồn!");

        // Gọi Service nghiệp vụ xử lý tổng kết, chuyển khoản tiền cọc và ăn chia tài sản trực tiếp dưới DB
        AuctionService auctionService = new AuctionService();
        int cleanupCount = 0;

        for (Auction auction : expiredAuctions) {
            try {
                System.out.println("[Bootstrap]    -> Tiến hành cưỡng chế đóng và chốt số liệu cho phiên: " + auction.getId());

                // Thực thi hàm kế toán chốt phiên vĩnh viễn dưới DB
                auctionService.finalizeAuction(auction.getId());
                cleanupCount++;

            } catch (Exception e) {
                System.err.println("[Bootstrap]    ❌ Lỗi khi xử lý cứu hộ phiên " + auction.getId() + ": " + e.getMessage());
            }
        }

        System.out.println("[Bootstrap]    -> Hoàn tất cứu hộ: Đã đóng thành công " + cleanupCount + " phiên quá hạn ngầm dưới DB.");
    }

    private void hydrateMemoryCache() {
       AuctionService auctionService = new AuctionService();
       auctionService.loadAuctionsToRAM();

    }

    /**
     * 5. Di chuyển logic Seeding Data từ Main về đây để giải phóng Main
     */
    private void seedUsersForTesting() {
        System.out.println("[Bootstrap] 5. Tiến hành kiểm tra và bơm dữ liệu mẫu (Seed Data)...");
        AuthService authService = new AuthService();

        registerTestUser(authService, "admin1", "Admin@123", "admin1@auction.com", UserRole.ADMIN);
        registerTestUser(authService, "bidder1", "Bidder@123", "bidder1@auction.com", UserRole.BIDDER);
        registerTestUser(authService, "seller1", "Seller@123", "seller1@auction.com", UserRole.SELLER);

        System.out.println("[Bootstrap]    -> Thành công: Hoàn tất kiểm tra tài khoản Test.");
    }

    private void registerTestUser(AuthService authService, String username, String password, String email, UserRole role) {
        try {
            authService.register(username, password, email, role);
            System.out.println("[Bootstrap Seed] Tạo user test thành công: " + username);
        } catch (AuthenticationException e) {
            System.out.println("[Bootstrap Seed] User " + username + " đã tồn tại hoặc lỗi: " + e.getMessage());
        }
    }

    /**
     * 🔥 BƯỚC MỚI: Bơm dữ liệu mẫu Items (Electronics, Art, Vehicles)
     */
    private void seedItemsForTesting() {
        System.out.println("[Bootstrap] 5.5a Tiến hành bơm dữ liệu mẫu Items (Vật phẩm)...");
        ItemService itemService = new ItemService();

        // Dữ liệu mẫu Electronics
        createElectronicsItem(itemService, "Laptop Dell XPS 13", 12000000.0, 2023, "seller1",
                "Laptop siêu mỏng, hiệu năng cao", "dell_xps.png", "Dell", 24);

        createElectronicsItem(itemService, "iPhone 14 Pro", 18000000.0, 2023, "seller1",
                "iPhone 14 Pro màu bạc, cô hồn lẻ", "iphone14pro.png", "Apple", 12);

        createElectronicsItem(itemService, "Samsung 55\" OLED TV", 15000000.0, 2023, "seller1",
                "Tivi OLED 55 inch, tần số 120Hz", "samsung_tv.png", "Samsung", 36);

        // Dữ liệu mẫu Art
        createArtItem(itemService, "Tranh sơn dầu cổ", 25000000.0, 1950, "seller1",
                "Tranh phong cảnh Châu Âu thế kỷ 20", "painting_1.png", "Picasso", "Cubism");

        createArtItem(itemService, "Tượng gỗ phật", 8000000.0, 1890, "seller1",
                "Tượng phật bằng gỗ nguyên khúc", "statue_1.png", "Unknown", "Buddhism");

        createArtItem(itemService, "Mặt nạ Hy Lạp cổ đại", 35000000.0, 300, "seller1",
                "Mặt nạ từ thời kỳ Hy Lạp cổ đại", "mask_greek.png", "Ancient", "Classical");

        // Dữ liệu mẫu Vehicles
        createVehicleItem(itemService, "Toyota Camry 2.5L", 650000000.0, 2020, "seller1",
                "Xe sedan, động cơ xăng 2.5L, trạng thái mới", "toyota_camry.png",
                "Camry 2.5LE", "Petrol", "29A-123456", 45000.0);

        createVehicleItem(itemService, "Honda Civic 2019", 580000000.0, 2019, "seller1",
                "Xe hatchback 5 cửa, màu đen nguyên zin", "honda_civic.png",
                "Civic EX", "Petrol", "30B-654321", 72000.0);

        createVehicleItem(itemService, "Harley Davidson Street 750", 320000000.0, 2021, "seller1",
                "Mô tô cruiser, máy 750cc, độ chế", "harley.png",
                "Street 750 Custom", "Petrol", "51C-987654", 8500.0);

        System.out.println("[Bootstrap]    -> Thành công: Hoàn tất tạo 9 vật phẩm thử nghiệm.");
    }

    private void createElectronicsItem(ItemService itemService, String name, double price, int year,
                                       String sellerId, String description, String imageUrl,
                                       String brand, int warrantyMonths) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("startingPrice", price);
            data.put("yearCreated", year);
            data.put("sellerId", sellerId);
            data.put("description", description);
            data.put("imageUrl", imageUrl);
            data.put("brand", brand);
            data.put("warrantyMonths", warrantyMonths);

            itemService.addItem(ItemType.ELECTRONICS, data);
            System.out.println("[Bootstrap Seed] Tạo Electronics thành công: " + name);
        } catch (Exception e) {
            System.out.println("[Bootstrap Seed] Lỗi khi tạo Electronics " + name + ": " + e.getMessage());
        }
    }

    private void createArtItem(ItemService itemService, String name, double price, int year,
                               String sellerId, String description, String imageUrl,
                               String painter, String artStyle) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("startingPrice", price);
            data.put("yearCreated", year);
            data.put("sellerId", sellerId);
            data.put("description", description);
            data.put("imageUrl", imageUrl);
            data.put("painter", painter);
            data.put("artStyle", artStyle);

            itemService.addItem(ItemType.ART, data);
            System.out.println("[Bootstrap Seed] Tạo Art thành công: " + name);
        } catch (Exception e) {
            System.out.println("[Bootstrap Seed] Lỗi khi tạo Art " + name + ": " + e.getMessage());
        }
    }

    private void createVehicleItem(ItemService itemService, String name, double price, int year,
                                   String sellerId, String description, String imageUrl,
                                   String model, String engineType, String licensePlate, double kmAge) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("startingPrice", price);
            data.put("yearCreated", year);
            data.put("sellerId", sellerId);
            data.put("description", description);
            data.put("imageUrl", imageUrl);
            data.put("model", model);
            data.put("engineType", engineType);
            data.put("licensePlate", licensePlate);
            data.put("kmAge", kmAge);

            itemService.addItem(ItemType.VEHICLES, data);
            System.out.println("[Bootstrap Seed] Tạo Vehicle thành công: " + name);
        } catch (Exception e) {
            System.out.println("[Bootstrap Seed] Lỗi khi tạo Vehicle " + name + ": " + e.getMessage());
        }
    }

    /**
     * 🔥 BƯỚC MỚI: Bơm dữ liệu mẫu Auctions trên các Items vừa tạo
     */
    private void seedAuctionsForTesting() {
        System.out.println("[Bootstrap] 5.5b Tiến hành bơm dữ liệu mẫu Auctions (Phiên đấu giá)...");
        AuctionService auctionService = new AuctionService();

        try {
            // Lấy danh sách items từ seller1 trong DB
            List<com.auction.models.Item.Item> items = itemDAO.findBySellerId("seller1");

            if (items.isEmpty()) {
                System.out.println("[Bootstrap]    ⚠️ Cảnh báo: Không tìm thấy items nào từ seller1, bỏ qua tạo auctions.");
                return;
            }

            System.out.println("[Bootstrap]    📦 Tìm thấy " + items.size() + " items, tiến hành tạo auctions...");

            // Tạo auctions cho 6 items đầu tiên (hoặc tất cả nếu ít hơn)
            int auctionCount = Math.min(items.size(), 6);
            for (int i = 0; i < auctionCount; i++) {
                try {
                    com.auction.models.Item.Item item = items.get(i);
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime startTime = now.plusHours(1);
                    LocalDateTime endTime = startTime.plusHours(24);

                    // Bước giá tối thiểu: 10% giá khởi động
                    double stepPrice = item.getStartingPrice() * 0.1;

                    auctionService.createAuction(
                            item.getId(),
                            "seller1",
                            stepPrice,
                            startTime,
                            endTime
                    );
                    System.out.println("[Bootstrap Seed] Tạo Auction thành công cho: " + item.getName() + " (ID: " + item.getId() + ")");
                } catch (Exception e) {
                    System.out.println("[Bootstrap Seed] Lỗi khi tạo Auction " + i + ": " + e.getMessage());
                }
            }

            System.out.println("[Bootstrap]    -> Thành công: Hoàn tất tạo " + auctionCount + " phiên đấu giá thử nghiệm.");
        } catch (Exception e) {
            System.out.println("[Bootstrap]    ❌ Lỗi khi lấy items từ DB: " + e.getMessage());
        }
    }

    private void registerGracefulShutdownHook() {
        System.out.println("[Bootstrap] 6. Đăng ký cơ chế dọn rác hệ thống (Graceful Shutdown)...");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Shutdown Hook] 🛑 Cảnh báo: JVM đang nhận lệnh tắt Server!");
            try {
                // 🏁 BƯỚC 1: Dừng máy phát điện (Ngắt luồng quét ngầm trước để RAM đứng im, không biến động nữa)
                AuctionManage.getInstance().stopScheduler();

                // 💾 BƯỚC 2: Chốt sổ kế toán (RAM đã đứng im ổn định rồi, giờ ép ghi toàn bộ xuống MySQL an toàn 100%)
                AuctionManage.getInstance().forceSyncRamToDatabase();

                // 🔌 BƯỚC 3: Đuổi khách (Đóng kết nối các cổng Socket vật lý của Client)
                ConnectionManage.getInstance().closeAllConnections();

                // 🗄️ BƯỚC 4: Khóa kho (Đóng toàn bộ Pool kết nối hướng về MySQL)
                DatabaseConnection.closePool();

                System.out.println("[Shutdown Hook] 🏁 Hệ thống đã đóng cửa an toàn tuyệt đối. Tạm biệt!");
            } catch (Exception e) {
                System.err.println("[Shutdown Hook] ❌ Lỗi khi dọn dẹp: " + e.getMessage());
            }
        }));
    }
}