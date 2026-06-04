package com.auction.manage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auction.models.Item.Item;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bộ quản lý danh mục sản phẩm/vật phẩm đấu giá trên bộ đệm RAM.
 */
public class ProductManage {
    private static final Logger log = LoggerFactory.getLogger(ProductManage.class);
    private static volatile ProductManage instance;

    private final Map<String, Item> items = new ConcurrentHashMap<>();

    private final Map<String, LocalDateTime> lastAccessedTime = new ConcurrentHashMap<>();

    private static final long MAX_IDLE_MINUTES = 15;

    private ProductManage(){}

    public static ProductManage getInstance(){
        ProductManage temp = instance;
        if (temp == null){
            synchronized (ProductManage.class){
                temp = instance;
                if(temp == null){
                    temp = instance = new ProductManage();
                }
            }
        }
        return temp;
    }

    public void addProduct(Item item) {
        if (item == null) {
            log.warn("Lỗi: Sản phẩm không được null");
            return;
        }

        Item existing = items.putIfAbsent(item.getId(), item);
        if (existing != null) {
            log.warn("Lỗi: Sản phẩm với ID '{}' đã tồn tại", item.getId());
            return;
        }

        lastAccessedTime.put(item.getId(), LocalDateTime.now());
        log.debug("Thêm sản phẩm thành công vào RAM: {}", item.getId());
    }

    public Item getProduct(String productId) {
        if (productId == null || productId.isEmpty()) {
            return null;
        }

        Item item = items.get(productId);
        if (item != null) {
            lastAccessedTime.put(productId, LocalDateTime.now());
        }
        return item;
    }

    public boolean updateProduct(String productId, Item updatedItem) {
        if (productId == null || updatedItem == null) return false;

        if (!items.containsKey(productId)) {
            log.warn("Lỗi: Sản phẩm với ID '{}' không tồn tại trên RAM", productId);
            return false;
        }

        updatedItem.setId(productId);
        items.put(productId, updatedItem);
        lastAccessedTime.put(productId, LocalDateTime.now());
        return true;
    }

    public boolean deleteProduct(String productId) {
        if (productId == null || productId.isEmpty()) return false;

        Item removed = items.remove(productId);
        if (removed != null) {
            lastAccessedTime.remove(productId);
            log.debug("Xóa sản phẩm thành công khỏi RAM: {}", productId);
            return true;
        }
        return false;
    }

    public List<Item> getAllProducts() {
        return new ArrayList<>(items.values());
    }

    public boolean isProductExists(String productId) {
        return productId != null && items.containsKey(productId);
    }

    /**
     * 🔥 HÀM THÊM MỚI CHÍ MẠNG: Dọn dẹp bộ nhớ đệm sản phẩm định kỳ.
     * Hàm này không cần chạy luồng scheduler riêng, bạn có thể gọi ké nó vào bên trong
     * hàm startLifecycleMonitor() của AuctionManage để tiết kiệm tài nguyên Server!
     */
    public void cleanupIdleProducts() {
        LocalDateTime now = LocalDateTime.now();
        for (String productId : items.keySet()) {
            LocalDateTime lastAccess = lastAccessedTime.get(productId);
            if (lastAccess != null) {
                long idleMinutes = Duration.between(lastAccess, now).toMinutes();

                if (idleMinutes >= MAX_IDLE_MINUTES) {
                    log.debug("[Cache Item] 🧹 Trục xuất sản phẩm idle khỏi RAM để giải phóng bộ nhớ: {}", productId);
                    items.remove(productId);
                    lastAccessedTime.remove(productId);
                }
            }
        }
    }
}