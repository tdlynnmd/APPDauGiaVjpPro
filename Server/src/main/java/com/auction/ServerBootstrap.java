package com.auction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auction.enums.ItemType;
import com.auction.enums.UserRole;
import com.auction.event.AuctionEventBus;
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
import com.auction.config.DatabaseConnection;

import java.sql.SQLException;
import java.util.*;

import static com.auction.models.User.UserFactory.setRegistry;

/**
 * =========================================================================
 * ServerBootstrap - Tổng chỉ huy vòng đời khởi tạo và dọn dẹp hệ thống
 * =========================================================================
 */
public class ServerBootstrap {
    private static final Logger log = LoggerFactory.getLogger(ServerBootstrap.class);

    private final AuctionDAO auctionDAO = new AuctionDAOImpl();
    private final ItemDAO itemDAO = new ItemDAOImpl();

    public void start() {
        log.info("[Bootstrap] 🚀 Bắt đầu quy trình khởi tạo hệ thống chuyên nghiệp...");

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

            // Bước 7: KÍCH HOẠT SCHEDULER: Cho phép bộ máy quét thời gian thực trên RAM vào guồng chạy
            log.info("[Bootstrap] 7. Kích hoạt bộ quét vòng đời tự động trên RAM (Every 1 Second)...");
            AuctionManage.getInstance().startLifecycleMonitor();

            // Bước 8: Đăng ký khiên bảo vệ tối cao Graceful Shutdown Hook với cấu trúc giải phóng triệt để
            registerGracefulShutdownHook();

            log.info("[Bootstrap] 🎉 HẠ TẦNG SẴN SÀNG 100%! Server có thể mở cổng đón Socket kết nối.");

        } catch (Exception e) {
            log.error("[Bootstrap] 💥 KHỞI ĐỘNG THẤT BẠI! Hệ thống sẽ cưỡng chế dừng lại.", e);
            System.exit(1);
        }
    }

    /**
     * Bước 0: Đồng bộ múi giờ hệ thống
     */
    private void setupSystemTimezone() {
        log.info("[Bootstrap] 0. Thiết lập cấu hình múi giờ chuẩn hệ thống (Asia/Ho_Chi_Minh)...");
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    /**
     * 1. Gom toàn bộ logic đăng ký Factory vào đây
     */
    private void initializeFactories() {
        log.info("[Bootstrap] 1. Đăng ký các User và Item Factories vào hệ thống...");

        // Đăng ký User Factories
        setRegistry(UserRole.BIDDER, new BidderFactory());
        setRegistry(UserRole.SELLER, new SellerFactory());
        setRegistry(UserRole.ADMIN, new AdminFactory());

        ItemFactory.register(ItemType.ELECTRONICS, new com.auction.models.Item.ElectronicsFactory());
        ItemFactory.register(ItemType.ART, new com.auction.models.Item.ArtFactory());
        ItemFactory.register(ItemType.VEHICLES, new com.auction.models.Item.VehicleFactory());

        log.info("[Bootstrap]    -> Thành công: Hoàn tất cấu hình Polymorphic Factories.");
    }

    private void initializeDatabasePool() throws SQLException {
        log.info("[Bootstrap] 2. Khởi tạo kết nối Database Pool...");
        DatabaseConnection.initialize();
        // Kiểm tra kết nối test và tự động đóng bằng try-with-resources để tránh rò rỉ connection
        try (java.sql.Connection testConn = DatabaseConnection.getConnection()) {
            log.info("[Bootstrap]    -> Pool khởi tạo thành công, kết nối test đã đóng an toàn.");
        }
    }

    private void wireInternalEventSystem() {
        log.info("[Bootstrap] 3. Tiến hành kết nối hệ thống Event-Driven...");
        AuctionEventBus.getInstance().attach(LiveRoomManage.getInstance());
    }

    /**
     * 🔥 HÀM MỚI: Quét dọn và tổng kết các phiên đấu giá bị quá hạn dưới DB do sập nguồn
     * Thực hiện ngay lúc bật Server để tránh nạp dữ liệu rác/quá hạn lên RAM.
     */
    private void cleanupExpiredAuctionsOnStartup() {
        log.info("[Bootstrap] 3.5 Đang kiểm tra cứu hộ các phiên đấu giá dính sự cố sập nguồn cũ...");

        // 1. Mò xuống DB hỏi: "Có ông RUNNING nào đáng lẽ phải kết thúc lúc Server đang sập không?"
        List<Auction> expiredAuctions = auctionDAO.findRunningAuctionsPastEndTime();

        if (expiredAuctions.isEmpty()) {
            log.info("[Bootstrap]    -> Tuyệt vời: Không có phiên đấu giá nào bị treo quá hạn.");
            return;
        }

        log.warn("[Bootstrap]    🚨 Phát hiện {} phiên bị treo trạng thái RUNNING do sập nguồn!", expiredAuctions.size());

        // Gọi Service nghiệp vụ xử lý tổng kết, chuyển khoản tiền cọc và ăn chia tài sản trực tiếp dưới DB
        AuctionService auctionService = AuctionService.getInstance();
        int cleanupCount = 0;

        for (Auction auction : expiredAuctions) {
            try {
                log.info("[Bootstrap]    -> Tiến hành cưỡng chế đóng và chốt số liệu cho phiên: {}", auction.getId());

                // Thực thi hàm kế toán chốt phiên vĩnh viễn dưới DB
                auctionService.finalizeAuction(auction.getId());
                cleanupCount++;

            } catch (Exception e) {
                log.error("[Bootstrap]    ❌ Lỗi khi xử lý cứu hộ phiên {}: {}", auction.getId(), e.getMessage(), e);
            }
        }

        log.info("[Bootstrap]    -> Hoàn tất cứu hộ: Đã đóng thành công {} phiên quá hạn ngầm dưới DB.", cleanupCount);
    }

    private void hydrateMemoryCache() {
       AuctionService.getInstance().loadAuctionsToRAM();
    }

    private void registerGracefulShutdownHook() {
        log.info("[Bootstrap] 6. Đăng ký cơ chế dọn rác hệ thống (Graceful Shutdown)...");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("\n[Shutdown Hook] 🛑 Cảnh báo: JVM đang nhận lệnh tắt Server!");
            try {
                // 🏁 BƯỚC 1: Dừng máy phát điện (Ngắt luồng quét ngầm trước để RAM đứng im, không biến động nữa)
                AuctionManage.getInstance().stopScheduler();

                // 💾 BƯỚC 2: Chốt sổ kế toán (RAM đã đứng im ổn định rồi, giờ ép ghi toàn bộ xuống MySQL an toàn 100%)
                AuctionManage.getInstance().forceSyncRamToDatabase();

                // 🔌 BƯỚC 3: Đuổi khách (Đóng kết nối các cổng Socket vật lý của Client)
                ConnectionManage.getInstance().closeAllConnections();

                // 🗄️ BƯỚC 4: Khóa kho (Đóng toàn bộ Pool kết nối hướng về MySQL)
                DatabaseConnection.closePool();

                log.info("[Shutdown Hook] 🏁 Hệ thống đã đóng cửa an toàn tuyệt đối. Tạm biệt!");
            } catch (Exception e) {
                log.error("[Shutdown Hook] ❌ Lỗi khi dọn dẹp: {}", e.getMessage(), e);
            }
        }));
    }
}