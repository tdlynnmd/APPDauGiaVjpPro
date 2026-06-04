package com.auction.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auction.dao.ItemDAO;
import com.auction.dao.LogDAO;
import com.auction.dao.impl.ItemDAOImpl;
import com.auction.dao.impl.LogDAOImpl;
import com.auction.dto.ItemDetailDTO;
import com.auction.dto.ItemSummaryDTO;
import com.auction.enums.ItemStatus;
import com.auction.enums.ItemType;
import com.auction.exception.AuctionErrorCode;
import com.auction.exception.AuctionException;
import com.auction.exception.ValidationErrorCode;
import com.auction.exception.ValidationException;
import com.auction.manage.ProductManage;
import com.auction.models.Item.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dịch vụ quản lý vòng đời và thông tin vật phẩm/sản phẩm đấu giá.
 */
public class ItemService {
    private static final Logger log = LoggerFactory.getLogger(ItemService.class);

    private final ItemDAO itemDAO = new ItemDAOImpl();
    private final ProductManage productManage = ProductManage.getInstance();
    private final LogDAO logDAO = new LogDAOImpl();

    public ItemDetailDTO addItem(String type, Map<String, Object> data) {
        return addItem(parseItemType(type), data);
    }

    public ItemDetailDTO addItem(ItemType type, Map<String, Object> data) {
        if (type == null || data == null) {
            throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Missing type or parameter payload data.");
        }

        validateBasicItemData(data);

        try (Connection conn = com.auction.config.DatabaseConnection.getConnection()) {
            data.put("id", UUID.randomUUID().toString());
            data.put("status", ItemStatus.ACTIVE);
            data.put("createdAt", LocalDateTime.now());

            Item newItem = ItemFactory.createItem(type, data);

            boolean isSavedDB = itemDAO.insertItem(conn, newItem);
            if (!isSavedDB) {
                throw new AuctionException(AuctionErrorCode.ITEM_SAVE_FAILED, "Persisting new item failed.");
            }
            productManage.addProduct(newItem);
            return toItemDetailDTO(newItem);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Factory payload evaluation error: " + e.getMessage());
        } catch (SQLException e) {
            throw new AuctionException(AuctionErrorCode.ITEM_SAVE_FAILED, "Database transaction failed at addItem: " + e.getMessage());
        }
    }

    public ItemDetailDTO updateItemInfo(String itemId, String type, Map<String, Object> incomingData) {
        return updateItemInfo(itemId, parseItemType(type), incomingData);
    }

    public ItemDetailDTO updateItemInfo(String itemId, ItemType type, Map<String, Object> incomingData) {
        if (itemId == null || itemId.trim().isEmpty() || type == null || incomingData == null) {
            throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Invalid update request mapping criteria.");
        }

        validateBasicItemData(incomingData);

        synchronized (itemId.trim().intern()) {
            Item liveItem = getItemById(itemId);
            if (liveItem == null) {
                throw new AuctionException(AuctionErrorCode.ITEM_NOT_FOUND);
            }

            if (liveItem.getStatus() != ItemStatus.ACTIVE) {
                throw new AuctionException(AuctionErrorCode.ITEM_IS_LOCKED,
                        "Vật phẩm này hiện không hoạt động (INACTIVE) hoặc đã bán, không thể chỉnh sửa.");
            }

            if (liveItem.getItemType() != type) {
                throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Mâu thuẫn loại vật phẩm. Không thể thay đổi loại của vật phẩm đang tồn tại.");
            }

            validateMergedItemData(liveItem, type, incomingData);
            updateLiveItemFields(liveItem, incomingData);

            try (Connection conn = com.auction.config.DatabaseConnection.getConnection()) {
                boolean isUpdatedDB = itemDAO.updateItem(conn, liveItem);
                if (!isUpdatedDB) {
                    throw new AuctionException(AuctionErrorCode.UPDATE_FAILED, "Synchronizing modified item properties to store failed.");
                }
            } catch (SQLException e) {
                throw new AuctionException(AuctionErrorCode.UPDATE_FAILED, "Database transaction failed at updateItemInfo: " + e.getMessage());
            }

            productManage.updateProduct(itemId, liveItem);
            return toItemDetailDTO(liveItem);
        }
    }

    public List<ItemSummaryDTO> getSellerItems(String sellerId) {
        if (sellerId == null || sellerId.trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.MISSING_REQUIRED_FIELD, "Seller identification constraint is empty.");
        }

        List<Item> dbItems = itemDAO.findBySellerId(sellerId);
        List<ItemSummaryDTO> result = new ArrayList<>();

        for (Item item : dbItems) {
            Item ramItem = productManage.getProduct(item.getId());

            if (ramItem == null) {
                if (item.getStatus() == ItemStatus.ACTIVE) {
                    productManage.addProduct(item);
                }
                ramItem = item;
            }

            result.add(new ItemSummaryDTO(
                    item.getId(),
                    item.getName(),
                    item.getStartingPrice(),
                    item.getItemType().name(),
                    ramItem.getStatus().name()
            ));
        }
        return result;
    }

    public ItemDetailDTO getDetailedItem(String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.MISSING_REQUIRED_FIELD, "Item criteria constraint target is empty.");
        }
        Item item = getItemById(itemId);
        if (item == null) {
            throw new AuctionException(AuctionErrorCode.ITEM_NOT_FOUND);
        }
        return toItemDetailDTO(item);
    }

    public void updateItemStatus(String itemId, ItemStatus newStatus) {
        if (itemId == null || newStatus == null) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Trường cập nhật trạng thái không hợp lệ.");
        }

        synchronized (itemId.trim().intern()) {
            try (Connection conn = com.auction.config.DatabaseConnection.getConnection()) {

                boolean isUpdatedDB = itemDAO.updateStatus(conn, itemId, newStatus.name());
                if (!isUpdatedDB) {
                    throw new AuctionException(AuctionErrorCode.ITEM_STATUS_UPDATE_FAILED, "Failed to update item status.");
                }

            } catch (SQLException e) {
                throw new AuctionException(AuctionErrorCode.ITEM_STATUS_UPDATE_FAILED, "Database link failed at updateItemStatus: " + e.getMessage());
            }

            Item ramItem = productManage.getProduct(itemId);
            if (ramItem == null) {
                if (newStatus == ItemStatus.ACTIVE) {
                    itemDAO.findById(itemId).ifPresent(productManage::addProduct);
                }
            } else {
                if (newStatus == ItemStatus.SOLD) {
                    productManage.deleteProduct(itemId);
                    ramItem.setStatus(newStatus);
                } else {
                    ramItem.setStatus(newStatus);
                }
            }
        }
    }

    private Item getItemById(String itemId) {
        Item item = productManage.getProduct(itemId);
        if (item == null) {
            item = itemDAO.findById(itemId).orElse(null);
            if (item != null && item.getStatus() == ItemStatus.ACTIVE) {
                productManage.addProduct(item);
            }
        }
        return item;
    }

    private void validateBasicItemData(Map<String, Object> data) {
        if (data.containsKey("startingPrice")) {
            try {
                double price = Double.parseDouble(data.get("startingPrice").toString());
                if (price < 0) {
                    throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Starting price cannot be negative.");
                }
            } catch (NumberFormatException e) {
                throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Starting price must be a valid number.");
            }
        }
        if (data.containsKey("yearCreated")) {
            try {
                int year = Integer.parseInt(data.get("yearCreated").toString());
                int currentYear = LocalDateTime.now().getYear();
                if (year > currentYear) {
                    throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Year created cannot be in the future.");
                }
            } catch (NumberFormatException e) {
                throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Year created must be a valid integer.");
            }
        }
    }

    public void deleteItem(String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.MISSING_REQUIRED_FIELD, "Item criteria target is empty.");
        }

        synchronized (itemId.trim().intern()) {
            Item item = getItemById(itemId);
            if (item == null) {
                throw new AuctionException(AuctionErrorCode.ITEM_NOT_FOUND);
            }

            if (item.getStatus() == ItemStatus.INACTIVE) {
                throw new AuctionException(AuctionErrorCode.ITEM_IS_LOCKED, "Vật phẩm đang trong phiên đấu giá hoạt động, không thể xóa.");
            }

            try (Connection conn = com.auction.config.DatabaseConnection.getConnection()) {
                boolean success = itemDAO.softDelete(conn, itemId);
                if (!success) {
                    throw new AuctionException(AuctionErrorCode.ITEM_DELETE_FAILED, "Failed to delete item.");
                }
            } catch (SQLException e) {
                throw new AuctionException(AuctionErrorCode.ITEM_DELETE_FAILED, "Database error during deleting item: " + e.getMessage());
            }

            productManage.deleteProduct(itemId);
            log.info("[ItemService] 🧹 Đã gỡ/xóa vật phẩm khỏi hệ thống và RAM: {}", itemId);
        }
    }

    public void deleteItemByAdmin(String itemId, String adminId, String reason) {
        if (itemId == null || itemId.trim().isEmpty() || adminId == null || adminId.trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Item ID and Admin ID constraints cannot be empty.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Censorship action requires a valid reason detail.");
        }

        synchronized (itemId.trim().intern()) {

            Item item = getItemById(itemId);
            if (item == null) {
                throw new AuctionException(AuctionErrorCode.ITEM_NOT_FOUND, "The target item for deletion does not exist.");
            }

            if (item.getStatus() == ItemStatus.INACTIVE) {
                log.warn("[Admin Censor] ⚠️ Vật phẩm đang nằm trong phiên đấu giá. Thực hiện cưỡng chế gỡ bỏ...");
            }

            Connection conn = null;
            try {
                conn = com.auction.config.DatabaseConnection.getConnection();
                conn.setAutoCommit(false);

                boolean isDeletedDB = itemDAO.updateStatus(conn, itemId, "BANNED");
                if (!isDeletedDB) {
                    throw new AuctionException(AuctionErrorCode.ITEM_DELETE_FAILED, "Database persistent rejection for deleting item.");
                }

                String logId = UUID.randomUUID().toString();
                String actionDetail = "Admin cưỡng chế gỡ bỏ sản phẩm [" + item.getName() + "] do vi phạm. Lý do: " + reason;
                logDAO.insertLog(conn, logId, adminId, actionDetail, "ITEM", itemId);

                conn.commit();
                log.info("[DB Transaction] ✅ Cưỡng chế xóa vật phẩm và lưu Audit Log thành công.");

            } catch (SQLException e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                        log.error("[DB Transaction] ❌ Gãy mạch xóa vật phẩm. Đã rollback DB nguyên trạng!");
                    } catch (SQLException ex) {
                        log.error("[DB Transaction] 🚨 Lỗi khẩn cấp không thể rollback: {}", ex.getMessage(), ex);
                    }
                }
                throw new AuctionException(AuctionErrorCode.ITEM_DELETE_FAILED, "Censorship transaction failed: " + e.getMessage());
            } finally {
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                        conn.close();
                    } catch (SQLException ex) {
                        log.error("[DB Transaction] Đóng kết nối thất bại: {}", ex.getMessage(), ex);
                    }
                }
            }

            productManage.deleteProduct(itemId);
            log.info("[Cache Item] 🧹 Đã trục xuất hoàn toàn vật phẩm vi phạm khỏi RAM: {}", itemId);
        }
    }

    private ItemDetailDTO toItemDetailDTO(Item item) {
        if (item == null) {
            throw new AuctionException(AuctionErrorCode.ITEM_NOT_FOUND);
        }

        String painter = null;
        String artStyle = null;
        String brand = null;
        Integer warrantyMonths = null;
        String model = null;
        String engineType = null;
        String licensePlate = null;
        Double kmAge = null;

        switch (item) {
            case Art art -> {
                painter = art.getPainter();
                artStyle = art.getArtStyle();
            }
            case Electronics electronics -> {
                brand = electronics.getBrand();
                warrantyMonths = electronics.getWarrantyMonths();
            }
            case Vehicle vehicle -> {
                model = vehicle.getModel();
                engineType = vehicle.getEngineType();
                licensePlate = vehicle.getLicensePlate();
                kmAge = vehicle.getKmAge();
            }
            default -> {
            }
        }

        return new ItemDetailDTO(
                item.getId(),
                item.getName(),
                item.getStartingPrice(),
                item.getItemType().name(),
                item.getStatus() == null ? null : item.getStatus().name(),
                item.getDescription(),
                item.getYearCreated(),
                item.getImageUrl(),
                item.getSellerId(),
                item.getCreatedAt(),
                painter,
                artStyle,
                brand,
                warrantyMonths,
                model,
                engineType,
                licensePlate,
                kmAge
        );
    }

    private ItemType parseItemType(String type) {
        try {
            return ItemFactory.parseItemType(type);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, e.getMessage());
        }
    }

    private void validateMergedItemData(Item liveItem, ItemType type, Map<String, Object> incomingData) {
        Map<String, Object> mergedData = toItemDataMap(liveItem);
        mergedData.putAll(incomingData);

        try {
            ItemFactory.createItem(type, mergedData);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Factory payload evaluation error: " + e.getMessage());
        }
    }

    private Map<String, Object> toItemDataMap(Item item) {
        Map<String, Object> data = new HashMap<>();

        putIfPresent(data, "sellerId", item.getSellerId());
        putIfPresent(data, "name", item.getName());
        data.put("startingPrice", item.getStartingPrice());
        putIfPresent(data, "description", item.getDescription());
        data.put("yearCreated", item.getYearCreated());
        putIfPresent(data, "imageUrl", item.getImageUrl());

        switch (item) {
            case Art art -> {
                putIfPresent(data, "painter", art.getPainter());
                putIfPresent(data, "artStyle", art.getArtStyle());
            }
            case Electronics electronics -> {
                putIfPresent(data, "brand", electronics.getBrand());
                data.put("warrantyMonths", electronics.getWarrantyMonths());
            }
            case Vehicle vehicle -> {
                putIfPresent(data, "model", vehicle.getModel());
                putIfPresent(data, "engineType", vehicle.getEngineType());
                putIfPresent(data, "licensePlate", vehicle.getLicensePlate());
                data.put("kmAge", vehicle.getKmAge());
            }
            default -> {
            }
        }

        return data;
    }

    private void putIfPresent(Map<String, Object> data, String key, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String text && text.trim().isEmpty()) {
            return;
        }
        data.put(key, value);
    }

    private void updateLiveItemFields(Item liveItem, Map<String, Object> incomingData) {
        if (incomingData.containsKey("name")) liveItem.setName((String) incomingData.get("name"));
        if (incomingData.containsKey("description")) liveItem.setDescription((String) incomingData.get("description"));
        if (incomingData.containsKey("imageUrl")) liveItem.setImageUrl((String) incomingData.get("imageUrl"));
        if (incomingData.containsKey("startingPrice")) {
            liveItem.setStartingPrice(Double.parseDouble(incomingData.get("startingPrice").toString()));
        }
        if (incomingData.containsKey("yearCreated")) {
            liveItem.setYearCreated(Integer.parseInt(incomingData.get("yearCreated").toString()));
        }

        switch (liveItem) {
            case Art art -> {
                if (incomingData.containsKey("painter")) art.setPainter((String) incomingData.get("painter"));
                if (incomingData.containsKey("artStyle")) art.setArtStyle((String) incomingData.get("artStyle"));
            }
            case Electronics elec -> {
                if (incomingData.containsKey("brand")) elec.setBrand((String) incomingData.get("brand"));
                if (incomingData.containsKey("warrantyMonths")) {
                    elec.setWarrantyMonths(Integer.parseInt(incomingData.get("warrantyMonths").toString()));
                }
            }
            case Vehicle vehicle -> {
                if (incomingData.containsKey("model")) vehicle.setModel((String) incomingData.get("model"));
                if (incomingData.containsKey("engineType")) vehicle.setEngineType((String) incomingData.get("engineType"));
                if (incomingData.containsKey("licensePlate")) vehicle.setLicensePlate((String) incomingData.get("licensePlate"));
                if (incomingData.containsKey("kmAge")) {
                    vehicle.setKmAge(Double.parseDouble(incomingData.get("kmAge").toString()));
                }
            }
            default -> {
            }
        }
    }
}