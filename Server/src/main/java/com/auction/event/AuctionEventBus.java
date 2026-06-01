package com.auction.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ============================================================
 * AuctionEventBus - Publisher/Subject chính của Observer Pattern
 * ============================================================
 * Mục đích:
 * - Quản lý danh sách các Observers (người theo dõi sự kiện)
 * - Cho phép Observers "đăng ký" (attach) hay "hủy đăng ký" (detach)
 * - Phát hành (publish) sự kiện đến tất cả Observers đã đăng ký
 * - Hoạt động như "trung tâm phối hợp" - Nơi Publisher gửi event, và các Observer nhận
 * Singleton Pattern:
 * - Chỉ có 1 instance duy nhất trong toàn bộ ứng dụng
 * - Tất cả publishers (AuctionService, AuctionManage) đều dùng instance này
 * Thread Safety:
 * - Dùng CopyOnWriteArrayList để an toàn khi mọi thread truy cập cùng lúc
 * - Không cần synchronized block
 * Ví dụ sử dụng:
 *   AuctionEvent event = new AuctionEvent("auction-123", AuctionEventType.NEW_BID, bidDTO);
 *   AuctionEventBus.getInstance().publish(event);
 *   // → Tất cả Observers đã đăng ký sẽ nhận được sự kiện này
 */
public class AuctionEventBus {
    private static final Logger log = LoggerFactory.getLogger(AuctionEventBus.class);
    private static volatile AuctionEventBus instance;

    // Danh sách các Observers (thread-safe)
    private final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    private AuctionEventBus() {
        // Constructor private - chông ngừa khởi tạo từ bên ngoài
        log.info("[EventBus] 🚀 AuctionEventBus khởi động");
    }

    /**
     * Lấy instance duy nhất của AuctionEventBus (Singleton)
     *
     * Double-checked locking pattern:
     * - Lần đầu check: nhanh (không cần lock)
     * - Nếu null: lock + check lần 2 trước khi tạo
     * - Secure và efficient
     *
     * @return AuctionEventBus instance
     */
    public static AuctionEventBus getInstance() {
        AuctionEventBus temp = instance;
        if (temp == null) {
            synchronized (AuctionEventBus.class) {
                temp = instance;
                if (temp == null) {
                    temp = instance = new AuctionEventBus();
                }
            }
        }
        return temp;
    }

    /**
     * Đăng ký một Observer (Subscribe)
     * Mục đích:
     * - Thêm một Observer vào danh sách nhận sự kiện
     * - Sau đó, whenever publish() gọi, Observer này sẽ nhận sự kiện
     *
     * @param observer Observer muốn đăng ký (VD: LiveRoomManage)
     */
    public void attach(AuctionObserver observer) {
        if (observer == null) {
            log.warn("[EventBus] ⚠️ Không thể attach observer null");
            return;
        }

        // CopyOnWriteArrayList tự động handle thread-safety
        observers.add(observer);
        log.info("[EventBus] ✅ Observer đã đăng ký: {}",
            observer.getClass().getSimpleName());
    }

    /**
     * Hủy đăng ký một Observer (Unsubscribe)
     * Mục đích:
     * - Xóa một Observer khỏi danh sách
     * - Sau đó, Observer này sẽ không nhận sự kiện nữa
     *
     * @param observer Observer muốn hủy đăng ký
     */
    public void detach(AuctionObserver observer) {
        if (observer == null) {
            log.warn("[EventBus] ⚠️ Không thể detach observer null");
            return;
        }

        if (observers.remove(observer)) {
            log.info("[EventBus] ❌ Observer đã hủy đăng ký: {}",
                observer.getClass().getSimpleName());
        }
    }

    /**
     * Phát hành một sự kiện đến tất cả Observers
     * Mục đích:
     * - Gửi một AuctionEvent đến tất cả Observers đã đăng ký
     * - Mỗi Observer sẽ nhận được sự kiện và xử lý trong method update()
     * Khi nào gọi:
     * - AuctionService.processBid(): publish(NEW_BID event)
     * - AuctionService.finalizeAuction(): publish(STATUS_CHANGED event)
     * - AuctionManage.tick(): publish(TIMER_TICK event)
     *
     * @param event Sự kiện muốn gửi đi
     */
    public void publish(AuctionEvent event) {
        if (event == null) {
            log.warn("[EventBus] ⚠️ Không thể publish event null");
            return;
        }

        log.debug("[EventBus] 📢 Publishing: {}", event);

        // Gửi event đến tất cả Observers
        // CopyOnWriteArrayList cho phép iterate an toàn khi mọi thread truy cập
        for (AuctionObserver observer : observers) {
            try {
                // Gọi update() của mỗi Observer
                observer.update(event);
            } catch (Exception e) {
                // Nếu 1 Observer lỗi, không ảnh hưởng đến Observer khác
                log.error("[EventBus] ⚠️ Observer {} xử lý event lỗi: {}",
                    observer.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }

    /**
     * Lấy số lượng Observers đang đăng ký (for monitoring)
     * @return số lượng observers
     */
    public int observerCount() {
        return observers.size();
    }

    /**
     * Xóa tất cả Observers (for cleanup/testing)
     */
    public void clearAllObservers() {
        observers.clear();
        log.info("[EventBus] 🧹 Tất cả Observers đã bị xóa");
    }
}

