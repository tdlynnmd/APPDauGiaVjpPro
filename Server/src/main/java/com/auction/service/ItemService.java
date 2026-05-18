package com.auction.service;

import com.auction.dao.ItemDAO;
import com.auction.dao.impl.ItemDAOImpl;
import com.auction.dto.ItemSummaryDTO;
import com.auction.enums.ItemStatus;
import com.auction.manage.ProductManage;
import com.auction.models.Item.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ItemService {
    private final ItemDAO itemDAO = new ItemDAOImpl();
    private final ProductManage productManage = ProductManage.getInstance();

    /**
     * 1. [HÀM CHUNG] - THÊM VẬT PHẨM MỚI (Áp dụng Factory Method)
     * Tự động nhận diện loại (ART, ELECTRONICS, VEHICLES) dựa trên tham số 'type'
     */
    public boolean addItem(String type, Map<String, Object> data) {
        if (type == null || data == null) return false;

        try {
            // Nạp thông số hệ thống tự quản lý trực tiếp vào Map dữ liệu
            data.put("id", UUID.randomUUID().toString());
            data.put("status", ItemStatus.ACTIVE);
            data.put("createdAt", LocalDateTime.now());

            // Gọi Factory khởi tạo đối tượng Đa hình tương ứng (Ném lỗi ngay nếu thiếu trường bắt buộc)
            Item newItem = ItemFactory.createItem(type, data);

            // Đồng bộ ghi xuống Database và nạp lên bộ nhớ RAM Cache để quản lý tập trung
            boolean isSavedDB = itemDAO.insertItem(newItem);
            if (isSavedDB) {
                productManage.addProduct(newItem);
                System.out.println("✅ Thêm kho thành công Item thuộc loại: " + type);
                return true;
            }
            return false;
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Lỗi Validation trường dữ liệu từ Factory: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("❌ Lỗi hệ thống khi thêm Item: " + e.getMessage());
            return false;
        }
    }

    /**
     * 2. [HÀM CHUNG] - CHỈNH SỬA THÔNG TIN VẬT PHẨM
     * Cơ chế: Giữ nguyên các trường cũ không thay đổi, chỉ đè các trường mới được gửi lên
     */
    public boolean updateItemInfo(String itemId, String type, Map<String, Object> incomingData) {
        if (itemId == null || type == null || incomingData == null) return false;

        // Lấy thực thể live (Ưu tiên RAM trước, DB sau)
        Item liveItem = getItemById(itemId);
        if (liveItem == null) {
            System.err.println("❌ Lỗi: Không tìm thấy vật phẩm [" + itemId + "] để chỉnh sửa.");
            return false;
        }

        // BẢO VỆ NGHIỆP VỤ: Đang đấu giá (INACTIVE) hoặc đã bán (SOLD) thì cấm sửa đổi!
        if (liveItem.getStatus() != ItemStatus.ACTIVE) {
            System.err.println("❌ Từ chối: Vật phẩm đã lên sàn đấu giá hoặc đã bán, cấm sửa đổi.");
            return false;
        }

        try {
            // Bước 1: Gọi hàm helper trích xuất dữ liệu hiện tại của vật phẩm làm nền móng
            Map<String, Object> mergedData = convertItemToMap(liveItem);

            // Bước 2: Gộp dữ liệu mới (Ghi đè trường thay đổi, giữ nguyên trường cũ không gửi lên)
            mergedData.putAll(incomingData);

            // Bước 3: Khóa cứng các định danh hệ thống cốt lõi bảo vệ tính toàn vẹn dữ liệu
            mergedData.put("id", itemId);
            mergedData.put("status", ItemStatus.ACTIVE);
            mergedData.put("createdAt", liveItem.getCreatedAt());

            // Bước 4: Tái tạo đối tượng qua Factory từ tập dữ liệu đã gộp đầy đủ
            Item updatedItem = ItemFactory.createItem(type, mergedData);

            // Bước 5: Đẩy xuống DAO cập nhật DB và đồng bộ làm mới RAM Cache
            boolean isUpdatedDB = itemDAO.updateItem(updatedItem);

            if (isUpdatedDB) {
                productManage.addProduct(updatedItem); // Đè thực thể mới lên RAM Cache
                System.out.println("✅ Cập nhật thông tin thành công cho Item: " + itemId);
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi thực hiện chỉnh sửa dữ liệu Item: " + e.getMessage());
            return false;
        }
    }

    /**
     * 3. [HÀM CỦA SELLER] - Lấy danh sách vật phẩm dạng DTO siêu nhẹ để đổ lên bảng JavaFX
     * Kết hợp lấy trạng thái Real-time chính xác tuyệt đối từ RAM Cache
     */
    public List<ItemSummaryDTO> getSellerItems(String sellerId) {
        if (sellerId == null || sellerId.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // Kéo danh sách thô từ Database lên
        List<Item> dbItems = itemDAO.findBySellerId(sellerId);
        List<ItemSummaryDTO> result = new ArrayList<>();

        // Duyệt qua để chuyển đổi sang DTO siêu nhẹ giúp tiết kiệm băng thông mạng
        for (Item item : dbItems) {
            // Tìm kiếm đối tượng live đang chạy trên bộ nhớ RAM Cache để lấy status mới nhất
            Item ramItem = productManage.getProduct(item.getId());
            String currentStatus = (ramItem != null) ? ramItem.getStatus().name() : item.getStatus().name();

            result.add(new ItemSummaryDTO(
                    item.getId(),
                    item.getName(),
                    item.getStartingPrice(),
                    item.getItemType().name(),
                    currentStatus
            ));
        }
        return result;
    }

    /**
     * 4. [HÀM CỦA SELLER] - Lấy chi tiết toàn bộ một vật phẩm (Khi click đúp vào dòng trên UI)
     * Trả về Object Model đa hình đầy đủ chứa các trường thông tin đặc thù
     */
    public Item getDetailedItem(String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            return null;
        }

        // Tái sử dụng hàm helper tìm kiếm thông minh: Tìm trên RAM trước, hụt RAM mới lội xuống DB
        Item item = getItemById(itemId);
        if (item == null) {
            System.err.println("⚠️ Cảnh báo: Không tìm thấy chi tiết vật phẩm cho ID [" + itemId + "]");
        }
        return item;
    }

    /**
     * 5. [HÀM HỆ THỐNG] - Cập nhật trạng thái nhanh (Internal Trigger)
     * Được gọi phối hợp bởi AuctionService để quản lý vòng đời (Khóa khi lên sàn / Mở khóa khi hủy / Đóng khi bán)
     */
    public boolean updateItemStatus(String itemId, ItemStatus newStatus) {
        if (itemId == null || newStatus == null) return false;

        boolean isUpdatedDB = itemDAO.updateStatus(itemId, newStatus.name());
        if (isUpdatedDB) {
            Item ramItem = productManage.getProduct(itemId);
            if (ramItem != null) {
                ramItem.setStatus(newStatus);
            }
            return true;
        }
        return false;
    }

    // =========================================================================
    // CÁC HÀM TRỢ GIÚP NỘI BỘ (PRIVATE HELPER METHODS) - PHỤC VỤ TRỰC TIẾP CLEAN CODE
    // =========================================================================

    /**
     * Helper 1: Tìm kiếm nhanh vật phẩm từ RAM Cache, nếu không thấy mới truy vấn DB
     */
    private Item getItemById(String itemId) {
        Item item = productManage.getProduct(itemId);
        return (item != null) ? item : itemDAO.findById(itemId).orElse(null);
    }

    /**
     * Helper 2: Chuyển đổi một đối tượng Model Đa hình ngược thành Map nền phục vụ gộp dữ liệu
     */
    private Map<String, Object> convertItemToMap(Item item) {
        Map<String, Object> map = new HashMap<>();

        // Trích xuất các thuộc tính chung
        map.put("name", item.getName());
        map.put("description", item.getDescription());
        map.put("startingPrice", item.getStartingPrice());
        map.put("yearCreated", item.getYearCreated());
        map.put("sellerId", item.getSellerId());
        map.put("imageUrl", item.getImageUrl());

        // Sử dụng Pattern Matching cho switch để bóc tách dữ liệu đặc thù theo từng dòng đa hình
        switch (item) {
            case Art art -> {
                map.put("painter", art.getPainter());
                map.put("artStyle", art.getArtStyle());
            }
            case Electronics elec -> {
                map.put("brand", elec.getBrand());
                map.put("warrantyMonths", elec.getWarrantyMonths());
            }
            case Vehicle vehicle -> {
                map.put("model", vehicle.getModel());
                map.put("engineType", vehicle.getEngineType());
                map.put("licensePlate", vehicle.getLicensePlate());
                map.put("kmAge", vehicle.getKmAge());
            }
            default -> {
                // Sẵn sàng mở rộng cho các thực thể đặc thù khác mà không ảnh hưởng tới code cũ
            }
        }
        return map;
    }
}