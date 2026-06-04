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

/**
 * Bộ quản lý vòng đời và bộ đệm RAM (Cache) của các phiên đấu giá trực tuyến.
 */
public class AuctionManage {
    public static volatile AuctionManage instance;
    private final Map<String, LocalDateTime> lastAccessedTime = new ConcurrentHashMap<>();
    private static final long MAX_IDLE_MINUTES = 10;
    private final Map<String, Auction> activeAuctions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

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

    public void addAuction(Auction auction){
        activeAuctions.put(auction.getId(),auction);
        lastAccessedTime.put(auction.getId(), LocalDateTime.now());
    }

    public void removeAuctionById(String id){
        activeAuctions.remove(id);
        lastAccessedTime.remove(id);
    }

    public Auction getAuctionById(String id){
        Auction auction = activeAuctions.get(id);
        if (auction != null) {
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
            synchronized (auctionId.trim().intern()) {
                auction.refreshStatus(LocalDateTime.now());
                if (auction.getStatus() != FINISHED) {
                    return;
                }

                try {
                    AuctionService.getInstance().finalizeAuction(auctionId);
                } catch (Exception e) {
                    System.err.println("[AuctionManage] ❌ Lỗi nghiêm trọng khi chốt phiên " + auctionId + ": " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    activeAuctions.remove(auctionId);
                    lastAccessedTime.remove(auctionId);
                }
            }
        }
    }

    public void startLifecycleMonitor() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                LocalDateTime now = LocalDateTime.now();

                ProductManage.getInstance().cleanupIdleProducts();

                for (Auction auction : activeAuctions.values()) {
                    AuctionStatus oldStatus = auction.getStatus();

                    auction.refreshStatus(now);
                    AuctionStatus newStatus = auction.getStatus();
                    String auctionId = auction.getId();

                    if (newStatus == RUNNING) {
                        long secondsLeft = Duration.between(now, auction.getEndTime()).toSeconds();
                        if (secondsLeft < 0) secondsLeft = 0;

                        AuctionEvent timerEvent = new AuctionEvent(
                                auctionId,
                                AuctionEventType.TIMER_TICK,
                                secondsLeft
                        );
                        AuctionEventBus.getInstance().publish(timerEvent);
                        lastAccessedTime.put(auctionId, now);
                    }

                    if (oldStatus == OPEN && newStatus == RUNNING) {
                        AuctionEvent startEvent = getAuctionEvent(auctionId);
                        AuctionEventBus.getInstance().publish(startEvent);

                        try {
                            synchronized (auction.getId().trim().intern()) {
                                AuctionService.getInstance().triggerAutoBids(auction);
                            }
                        } catch (Exception ex) {
                            System.err.println("[AuctionManage] ⚠️ Lỗi trigger AutoBid khi phiên khai hỏa: " + ex.getMessage());
                        }
                    }

                    if (oldStatus == RUNNING && newStatus == FINISHED) {
                        finishAuction(auction.getId());
                        continue;
                    }

                    if (newStatus != RUNNING) {
                        long minutesUntilStart = Duration.between(now, auction.getStartTime()).toMinutes();

                        if (minutesUntilStart > 15) {
                            LocalDateTime lastAccess = lastAccessedTime.get(auctionId);
                            if (lastAccess != null) {
                                long idleMinutes = Duration.between(lastAccess, now).toMinutes();
                                if (idleMinutes >= MAX_IDLE_MINUTES) {
                                    activeAuctions.remove(auctionId);
                                    lastAccessedTime.remove(auctionId);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[AuctionManage] ❌ Lỗi trong vòng quét lifecycle, bỏ qua: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);

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
                String deleteAuctionsSql = "DELETE FROM auctions WHERE status IN ('FINISHED', 'CANCELED', 'PAID') AND end_time <= ?";
                try (java.sql.PreparedStatement stmt1 = conn.prepareStatement(deleteAuctionsSql)) {
                    stmt1.setTimestamp(1, java.sql.Timestamp.valueOf(threshold));
                    int deletedAuctions = stmt1.executeUpdate();
                    if (deletedAuctions > 0) {
                        System.out.println("[AuctionManage] 🧹 Đã xóa " + deletedAuctions + " phiên đấu giá đã kết thúc/hủy > 24 giờ.");
                    }
                }

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

        if (activeAuctions.isEmpty()) {
            System.out.println("[AuctionManage] ℹ️ Không có phiên đấu giá nào trên RAM cần đồng bộ.");
            return;
        }

        int successCount = 0;
        int totalCount = activeAuctions.size();

        for (Auction auction : activeAuctions.values()) {
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

        scheduler.shutdown();

        try {
            if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
                System.out.println("[AuctionManage] ⚠️ Luồng ngầm không chịu dừng, đã cưỡng chế hủy (ShutdownNow).");
            } else {
                System.out.println("[AuctionManage] ✅ Bộ quét luồng ngầm đã hạ cánh an toàn.");
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
            System.err.println("[AuctionManage] ❌ Quá trình đóng luồng bị ngắt quãng, cưỡng chế dừng.");
        }
    }
}
