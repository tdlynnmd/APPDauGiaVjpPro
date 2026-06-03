package com.auction.manage;

import com.auction.dao.AuctionDAO;
import com.auction.dao.impl.AuctionDAOImpl;
import com.auction.enums.AuctionStatus;
import com.auction.models.Auction.Auction;
import com.auction.event.AuctionEvent;
import com.auction.event.AuctionEventBus;
import com.auction.event.AuctionEventType;
import com.auction.service.AuctionService;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import static com.auction.enums.AuctionStatus .*;

public class AuctionManage {
    public static volatile AuctionManage instance;
    // 🔥 THÊM MỚI: Quản lý thời gian tương tác cuối cùng của các phiên trên RAM (Để dọn dẹp phiên rác)
    private final Map<String, LocalDateTime> lastAccessedTime = new ConcurrentHashMap<>();
    // Cấu hình thời gian tối đa một phiên được phép "nằm im" trên RAM nếu không phải trạng thái RUNNING
    private static final long MAX_IDLE_MINUTES = 10;
    private final Map<String, Auction> activeAuctions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private AuctionService auctionService;

    private AuctionManage(){}
    public static AuctionManage getInstance(){
        AuctionManage temp = instance;
        if (temp == null){
            synchronized (AuctionManage.class){
                temp = instance;
                if(temp == null){
                    temp = instance  = new AuctionManage();
                }
            }
        }
        return temp;
    }

    // Hàm gọi lười AuctionService
    private AuctionService getAuctionService() {
        if (auctionService == null) {
            auctionService = new AuctionService();
        }
        return auctionService;
    }

    public void addAuction(Auction auction){
        activeAuctions.put(auction.getId(),auction);
        // 🔥 THÊM MỚI: Đánh dấu thời gian nạp vào RAM ban đầu
        lastAccessedTime.put(auction.getId(), LocalDateTime.now());
    }

    public void removeAuctionById(String id){
        activeAuctions.remove(id);
        lastAccessedTime.remove(id); // 🔥 Tháo dỡ vết cache
    }

    public Auction getAuctionById(String id){
        Auction auction = activeAuctions.get(id);
        if (auction != null) {
            // 🔥 THÊM MỚI: Mỗi khi có người truy cập (xem chi tiết, đặt giá), cập nhật mốc thời gian "sống"
            lastAccessedTime.put(id, LocalDateTime.now());
        }
        return auction;
    }

    public List<Auction> getAllActive(){
        return new ArrayList<>(activeAuctions.values());
    }

    private void finishAuction(String auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction != null) {
            synchronized (auction) {
                // DOUBLE-CHECK: Lỡ có ai vừa vặn đặt giá và gia hạn thêm 60s khi ta đang đứng đợi khóa thì sao?
                auction.refreshStatus(LocalDateTime.now());
                if (auction.getStatus() != FINISHED) {
                    return; // Quay xe, chưa hết giờ!
                }

                // 1. Gọi Service Kế toán để trừ/cộng tiền trong Database
                getAuctionService().finalizeAuction(auctionId);

                // Xóa khỏi danh sách "đang hoạt động" để giải phóng bộ nhớ RAM
                activeAuctions.remove(auctionId);
                lastAccessedTime.remove(auctionId);
            }
        }
    }


    //Quản lý vòng đời của auction tự động bằng realtime
    public void startLifecycleMonitor() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                LocalDateTime now = LocalDateTime.now();

                // Luồng dọn dẹp sản phẩm rác ké luồng đếm giờ hệ thống
                ProductManage.getInstance().cleanupIdleProducts();

                for (Auction auction : activeAuctions.values()) {
                    //  Lưu lại trạng thái cũ
                    AuctionStatus oldStatus = auction.getStatus();

                    //  Refresh trạng thái theo thời gian thực
                    auction.refreshStatus(now);
                    AuctionStatus newStatus = auction.getStatus();
                    String auctionId = auction.getId();

                    // 1. Tự động phát sự kiện TIMER_TICK mỗi giây cho phòng đang chạy
                    if (newStatus == RUNNING) {
                        long secondsLeft = Duration.between(now, auction.getEndTime()).toSeconds();
                        if (secondsLeft < 0) secondsLeft = 0;

                        AuctionEvent timerEvent = new AuctionEvent(
                                auctionId,
                                AuctionEventType.TIMER_TICK,
                                secondsLeft
                        );
                        AuctionEventBus.getInstance().publish(timerEvent);
                        // Vì phòng đang RUNNING và bắn tick liên tục, ta luôn cập nhật để giữ nó trên RAM
                        lastAccessedTime.put(auctionId, now);
                    }

                    // 2. Thông báo khi VỪA MỚI chuyển trạng thái từ OPEN sang RUNNING
                    if (oldStatus == OPEN && newStatus == RUNNING) {
                        // 🔥 THAY ĐỔI TẠI ĐÂY: Đồng bộ các Khóa dữ liệu khi phiên chính thức khai hỏa
                        AuctionEvent startEvent = getAuctionEvent(auctionId);
                        AuctionEventBus.getInstance().publish(startEvent);

                        // Kích hoạt AutoBid ngay khi phiên chuyển sang RUNNING
                        // đảm bảo các lệnh autobid đặt trước không bị bỏ qua giây đầu tiên
                        try {
                            synchronized (auction) {
                                getAuctionService().triggerAutoBids(auction);
                            }
                        } catch (Exception ex) {
                            System.err.println("[AuctionManage] ⚠️ Lỗi trigger AutoBid khi phiên khai hỏa: " + ex.getMessage());
                        }
                    }

                    // 3. Nếu VỪA MỚI kết thúc thì gọi hàm xử lý
                    if (oldStatus == RUNNING && newStatus == FINISHED) {
                        finishAuction(auction.getId());
                        continue;
                    }

                    // 🔥 LUỒNG 4 (THÊM MỚI): KIỂM TRA ĐỂ TRỤC XUẤT CÁC PHIÊN KHÔNG HOẠT ĐỘNG (CACHE EVICTION)
                    // Điều kiện trục xuất: Phiên KHÔNG PHẢI đang chạy (có thể là OPEN hoặc đã đóng)
                    // VÀ không có ai tương tác (đặt giá, xem chi tiết) quá MAX_IDLE_MINUTES
                    if (newStatus != RUNNING) {
                        // Tính toán xem còn bao nhiêu phút nữa thì phiên này bắt đầu chạy (startTime)
                        long minutesUntilStart = Duration.between(now, auction.getStartTime()).toMinutes();

                        // LUẬT BẢO VỆ: Nếu còn dưới 15 phút nữa là mở cửa, KHÔNG ĐƯỢC trục xuất khỏi RAM
                        if (minutesUntilStart > 15) {
                            LocalDateTime lastAccess = lastAccessedTime.get(auctionId);
                            if (lastAccess != null) {
                                long idleMinutes = Duration.between(lastAccess, now).toMinutes();
                                if (idleMinutes >= MAX_IDLE_MINUTES) {
                                    // Đủ điều kiện nằm im và còn lâu mới chạy -> Tiến hành dọn dẹp giải phóng RAM
                                    activeAuctions.remove(auctionId);
                                    lastAccessedTime.remove(auctionId);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Bọc toàn bộ thân Task trong try-catch để đảm bảo luồng đếm giờ không bao giờ chết
                // dù có lỗi phát sinh từ bất kỳ phiên đấu giá nào
                System.err.println("[AuctionManage] ❌ Lỗi trong vòng quét lifecycle, bỏ qua: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS); // Quét mỗi giây 1 lần

        // Task dọn dẹp chạy mỗi 10 phút
        scheduler.scheduleAtFixedRate(() -> {
            try {
                performDatabaseCleanup();
            } catch (Exception e) {
                System.err.println("[AuctionManage] ❌ Lỗi chạy task dọn dẹp DB: " + e.getMessage());
            }
        }, 0, 10, TimeUnit.MINUTES);
    }

    private void performDatabaseCleanup() {
        System.out.println("[AuctionManage] 🧹 Đang chạy tiến trình dọn dẹp cơ sở dữ liệu định kỳ...");
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        
        try (java.sql.Connection conn = com.auction.config.DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Query 1: Xóa cứng các phiên đấu giá đã kết thúc/hủy lâu hơn 24 giờ
                String deleteAuctionsSql = "DELETE FROM auctions WHERE status IN ('FINISHED', 'CANCELED', 'PAID') AND end_time <= ?";
                try (java.sql.PreparedStatement stmt1 = conn.prepareStatement(deleteAuctionsSql)) {
                    stmt1.setTimestamp(1, java.sql.Timestamp.valueOf(threshold));
                    int deletedAuctions = stmt1.executeUpdate();
                    if (deletedAuctions > 0) {
                        System.out.println("[AuctionManage] 🧹 Đã xóa " + deletedAuctions + " phiên đấu giá đã kết thúc/hủy > 24 giờ.");
                    }
                }

                // Query 2: Xóa cứng các vật phẩm đã bị xóa mềm và không còn phiên đấu giá nào tham chiếu
                String deleteItemsSql = "DELETE i FROM items i LEFT JOIN auctions a ON i.id = a.item_id WHERE i.deleted_at IS NOT NULL AND a.id IS NULL";
                try (java.sql.PreparedStatement stmt2 = conn.prepareStatement(deleteItemsSql)) {
                    int deletedItems = stmt2.executeUpdate();
                    if (deletedItems > 0) {
                        System.out.println("[AuctionManage] 🧹 Đã xóa " + deletedItems + " vật phẩm đã xóa mềm không còn phiên đấu giá tham chiếu.");
                    }
                }
                
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            System.err.println("[AuctionManage] ❌ Lỗi dọn dẹp cơ sở dữ liệu định kỳ: " + e.getMessage());
        }
    }

    @NotNull
    private static AuctionEvent getAuctionEvent(String auctionId) {
        Map<String, Object> statusPayload = new HashMap<>();
        statusPayload.put("newStatus", AuctionStatus.RUNNING.name());
        statusPayload.put("message", "Phiên đấu giá ĐÃ BẮT ĐẦU! Hãy nhanh tay đặt giá!");

        return new AuctionEvent(
                auctionId,
                AuctionEventType.STATUS_CHANGED,
                statusPayload
        );
    }

    /**
     * 🔥 THỰC THI FORCE SYNC TOÀN DIỆN: Đẩy toàn bộ dữ liệu từ RAM xuống MySQL
     * Chốt chặn tối cao bảo vệ an toàn tài sản và số dư của khách hàng khi tắt Server.
     */
    public void forceSyncRamToDatabase() {
        System.out.println("[AuctionManage] 💾 Đang kích hoạt tiến trình đồng bộ khẩn cấp RAM -> Database...");

        // Defensive check: Nếu RAM trống thì không cần tốn tài nguyên mở kết nối DB
        if (activeAuctions.isEmpty()) {
            System.out.println("[AuctionManage] ℹ️ Không có phiên đấu giá nào trên RAM cần đồng bộ.");
            return;
        }

        int successCount = 0;
        int totalCount = activeAuctions.size();

        // Sử dụng values() chạy trên ConcurrentHashMap an toàn đa luồng.
        // Kể cả khi có luồng khác đang đọc ghi, tiến trình quét này vẫn không bị dính ConcurrentModificationException
        for (Auction auction : activeAuctions.values()) {
            // Tối ưu hóa: Sử dụng kỹ thuật cô lập lỗi (Error Isolation).
            // Nếu 1 phiên bị lỗi dữ liệu, hệ thống vẫn phải tiếp tục ghi nhận các phiên tiếp theo, không được phép sập luồng ngắt quãng.
            try {
                AuctionDAO auctionDAO = new AuctionDAOImpl();
                boolean isSynced = auctionDAO.updateAuctionStatusAndBidding(auction);
                if (isSynced) {
                    successCount++;
                } else {
                    System.err.println("[AuctionManage] ⚠️ Phiên đấu giá " + auction.getId()
                            + " không thể cập nhật (Có thể do Id không tồn tại dưới DB).");
                }
            } catch (Exception e) {
                System.err.println("[AuctionManage] ❌ Lỗi đột biến khi đồng bộ phiên "
                        + auction.getId() + ": " + e.getMessage());
            }
        }

        System.out.println("[AuctionManage] 🎉 HOÀN TẤT ĐỒNG BỘ NỀN KHẨN CẤP!");
        System.out.println("[AuctionManage] 👉 Kết quả: Đã ép bảo vệ thành công "
                + successCount + "/" + totalCount + " phiên đấu giá an toàn xuống MySQL.");
    }

    /**
     * 🔥 CƠ CHẾ ĐÓNG AN TOÀN LUỒNG NGẦM: Tắt bộ quét vòng đời hệ thống
     * Được gọi duy nhất khi Server nhận tín hiệu đóng cửa (Shutdown Hook).
     */
    public void stopScheduler() {
        System.out.println("[AuctionManage] ⏳ Đang tiến hành đóng băng luồng đếm giây ngầm...");

        // 1. Ra lệnh không tiếp nhận chu kỳ quét mới nữa
        scheduler.shutdown();

        try {
            // 2. Chờ tối đa 3 giây cho lượt quét hiện tại (nếu đang chạy dở) kịp kết thúc an toàn
            if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                // Nếu quá 3 giây mà luồng vẫn cố tình treo, cưỡng chế hủy diệt lập tức
                scheduler.shutdownNow();
                System.out.println("[AuctionManage] ⚠️ Luồng ngầm không chịu dừng, đã cưỡng chế hủy (ShutdownNow).");
            } else {
                System.out.println("[AuctionManage] ✅ Bộ quét luồng ngầm đã hạ cánh an toàn.");
            }
        } catch (InterruptedException e) {
            // Bị ngắt quãng trong quá trình đợi, cưỡng chế dừng luôn
            scheduler.shutdownNow();
            Thread.currentThread().interrupt(); // Khôi phục lại trạng thái ngắt quãng của Thread
            System.err.println("[AuctionManage] ❌ Quá trình đóng luồng bị ngắt quãng, cưỡng chế dừng.");
        }
    }
}
