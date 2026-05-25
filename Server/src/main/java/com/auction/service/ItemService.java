package com.auction.service;

import com.auction.dao.ItemDAO;
import com.auction.dao.impl.ItemDAOImpl;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ItemService {
    private final ItemDAO itemDAO = new ItemDAOImpl();
    private final ProductManage productManage = ProductManage.getInstance();

    public ItemDetailDTO addItem(String type, Map<String, Object> data) {
        return addItem(ItemFactory.parseItemType(type), data);
    }

    public ItemDetailDTO addItem(ItemType type, Map<String, Object> data) {
        if (type == null || data == null) {
            throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Missing type or parameter payload data.");
        }

        try {
            data.put("id", UUID.randomUUID().toString());
            data.put("status", ItemStatus.ACTIVE);
            data.put("createdAt", LocalDateTime.now());

            Item newItem = ItemFactory.createItem(type, data);

            boolean isSavedDB = itemDAO.insertItem(newItem);
            if (!isSavedDB) {
                throw new AuctionException(AuctionErrorCode.DATABASE_ERROR, "Persisting new item failed.");
            }
            productManage.addProduct(newItem);
            return toItemDetailDTO(newItem);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Factory payload evaluation error: " + e.getMessage());
        }
    }

    public ItemDetailDTO updateItemInfo(String itemId, String type, Map<String, Object> incomingData) {
        return updateItemInfo(itemId, ItemFactory.parseItemType(type), incomingData);
    }

    public ItemDetailDTO updateItemInfo(String itemId, ItemType type, Map<String, Object> incomingData) {
        if (itemId == null || type == null || incomingData == null) {
            throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Invalid update request mapping criteria.");
        }

        Item liveItem = getItemById(itemId);
        if (liveItem == null) {
            throw new AuctionException(AuctionErrorCode.ITEM_NOT_FOUND);
        }

        if (liveItem.getItemType() != type) {
            throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Mâu thuẫn loại vật phẩm. Không thể thay đổi loại của vật phẩm đang tồn tại.");
        }

        ItemFactory.createItem(type, incomingData);

        synchronized (liveItem.getId().intern()) {
            if (liveItem.getStatus() != ItemStatus.ACTIVE) {
                throw new AuctionException(AuctionErrorCode.ITEM_IS_LOCKED);
            }

            updateLiveItemFields(liveItem, incomingData);
            updateSubClassFields(liveItem, type, incomingData);

            boolean isUpdatedDB = itemDAO.updateItem(liveItem);
            if (!isUpdatedDB) {
                throw new AuctionException(AuctionErrorCode.DATABASE_ERROR, "Synchronizing modified item properties to store failed.");
            }

            productManage.updateProduct(itemId, liveItem);
            return toItemDetailDTO(liveItem);
        }
    }

    private void updateSubClassFields(Item liveItem, ItemType type, Map<String, Object> incomingData) {
        switch (type) {
            case ELECTRONICS:
                if (liveItem instanceof Electronics electronics) {
                    electronics.setBrand((String) incomingData.get("brand"));
                    if (incomingData.get("warrantyMonths") instanceof Number number) {
                        electronics.setWarrantyMonths(number.intValue());
                    }
                }
                break;

            case ART:
                if (liveItem instanceof Art art) {
                    art.setPainter((String) incomingData.get("painter"));
                    art.setArtStyle((String) incomingData.get("artStyle"));
                }
                break;

            case VEHICLES:
                if (liveItem instanceof Vehicle vehicle) {
                    vehicle.setModel((String) incomingData.get("model"));
                    vehicle.setLicensePlate((String) incomingData.get("licensePlate"));
                    if (incomingData.get("kmAge") instanceof Number number) {
                        vehicle.setKmAge(number.doubleValue());
                    }
                }
                break;
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
                productManage.addProduct(item);
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

        boolean isUpdatedDB = itemDAO.updateStatus(itemId, newStatus.name());
        if (!isUpdatedDB) {
            throw new AuctionException(AuctionErrorCode.DATABASE_ERROR, "Failed to update item status.");
        }

        Item ramItem = productManage.getProduct(itemId);
        if (ramItem == null) {
            itemDAO.findById(itemId).ifPresent(productManage::addProduct);
        } else {
            ramItem.setStatus(newStatus);
        }
    }

    private Item getItemById(String itemId) {
        Item item = productManage.getProduct(itemId);
        if (item == null) {
            item = itemDAO.findById(itemId).orElse(null);
            if (item != null) {
                productManage.addProduct(item);
            }
        }
        return item;
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
