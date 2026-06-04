package com.auction.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lớp trung tâm quản lý và phát hành các sự kiện đấu giá theo mô hình Observer Pattern.
 */
public class AuctionEventBus {
    private static final Logger log = LoggerFactory.getLogger(AuctionEventBus.class);
    private static volatile AuctionEventBus instance;

    private final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    private AuctionEventBus() {
        log.info("[EventBus] 🚀 AuctionEventBus khởi động");
    }

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

    public void attach(AuctionObserver observer) {
        if (observer == null) {
            log.warn("[EventBus] ⚠️ Không thể attach observer null");
            return;
        }

        observers.add(observer);
        log.info("[EventBus] ✅ Observer đã đăng ký: {}",
            observer.getClass().getSimpleName());
    }

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

    public void publish(AuctionEvent event) {
        if (event == null) {
            log.warn("[EventBus] ⚠️ Không thể publish event null");
            return;
        }

        log.debug("[EventBus] 📢 Publishing: {}", event);

        for (AuctionObserver observer : observers) {
            try {
                observer.update(event);
            } catch (Exception e) {
                log.error("[EventBus] ⚠️ Observer {} xử lý event lỗi: {}",
                    observer.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }

    public int observerCount() {
        return observers.size();
    }

    public void clearAllObservers() {
        observers.clear();
        log.info("[EventBus] 🧹 Tất cả Observers đã bị xóa");
    }
}

