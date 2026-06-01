package com.auction.controller;

import com.auction.dto.ItemDetailDTO;
import com.auction.dto.ItemSummaryDTO;
import com.auction.enums.ItemStatus;
import com.auction.enums.UserRole;
import com.auction.exception.AuthorizationErrorCode;
import com.auction.exception.AuthorizationException;
import com.auction.exception.ValidationErrorCode;
import com.auction.exception.ValidationException;
import com.auction.network.ClientSession;
import com.auction.service.ItemService;
import com.auction.utils.GsonProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ItemControllerTest {

    private ItemController itemController;
    private FakeItemService itemService;
    private com.google.gson.Gson gson;

    @BeforeEach
    void setUp() throws Exception {
        itemController = new ItemController();
        itemService = new FakeItemService();
        gson = GsonProvider.getGson();

        injectField(itemController, "itemService", itemService);
    }

    // Inject fake ItemService để không gọi DB/service thật
    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // Check đúng mã lỗi ValidationException
    private void assertValidationError(ValidationException exception, ValidationErrorCode expectedError) {
        assertEquals(expectedError.getCode(), exception.getErrorCode());
    }

    // Check đúng mã lỗi AuthorizationException
    private void assertAuthorizationError(AuthorizationException exception, AuthorizationErrorCode expectedError) {
        assertEquals(expectedError.getCode(), exception.getErrorCode());
    }

    // Tạo session đã login
    private ClientSession loggedInSession(String userId, UserRole role) {
        ClientSession session = new ClientSession((Socket) null, new PrintWriter(System.out));
        session.setUserId(userId);
        session.setRole(role);
        return session;
    }

    // Tạo item detail mẫu
    private ItemDetailDTO sampleItemDetail(String itemId, String sellerId) {
        return new ItemDetailDTO(
                itemId,
                "Laptop Dell",
                12000000.0,
                "ELECTRONICS",
                "ACTIVE",
                "Laptop văn phòng",
                2022,
                "dell.png",
                sellerId,
                LocalDateTime.now(),
                null,
                null,
                "Dell",
                24,
                null,
                null,
                null,
                null
        );
    }

    // Fake ItemService để bắt tham số controller truyền xuống
    private static class FakeItemService extends ItemService {
        ItemDetailDTO detailToReturn;
        List<ItemSummaryDTO> sellerItemsToReturn = List.of();

        String lastAddItemType;
        Map<String, Object> lastAddItemData;

        String lastUpdateItemId;
        String lastUpdateItemType;
        Map<String, Object> lastUpdateData;

        String lastDetailedItemId;

        String lastSellerId;

        String lastUpdateStatusItemId;
        ItemStatus lastUpdateStatus;

        @Override
        public ItemDetailDTO addItem(String itemType, Map<String, Object> itemData) {
            lastAddItemType = itemType;
            lastAddItemData = itemData;
            return detailToReturn;
        }

        @Override
        public ItemDetailDTO updateItemInfo(String itemId, String itemType, Map<String, Object> updates) {
            lastUpdateItemId = itemId;
            lastUpdateItemType = itemType;
            lastUpdateData = updates;
            return detailToReturn;
        }

        @Override
        public ItemDetailDTO getDetailedItem(String itemId) {
            lastDetailedItemId = itemId;
            return detailToReturn;
        }

        @Override
        public List<ItemSummaryDTO> getSellerItems(String sellerId) {
            lastSellerId = sellerId;
            return sellerItemsToReturn;
        }

        @Override
        public void updateItemStatus(String itemId, ItemStatus status) {
            lastUpdateStatusItemId = itemId;
            lastUpdateStatus = status;
        }
    }

    // =========================================================
    // createItem()
    // =========================================================

    // createItem body rỗng phải ném BAD_REQUEST
    @Test
    void createItemShouldThrowWhenBodyIsBlank() {
        ClientSession session = loggedInSession("seller-1", UserRole.SELLER);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            itemController.createItem("   ", session);
        });

        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    // createItem session null phải ném BAD_REQUEST
    @Test
    void createItemShouldThrowWhenSessionIsNull() {
        String bodyJson = """
                {
                  "itemType": "ELECTRONICS",
                  "name": "Laptop Dell",
                  "startingPrice": 12000000,
                  "brand": "Dell",
                  "warrantyMonths": 24
                }
                """;

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            itemController.createItem(bodyJson, null);
        });

        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    // createItem userId null phải ném BAD_REQUEST
    @Test
    void createItemShouldThrowWhenSessionHasNoUserId() {
        ClientSession session = loggedInSession(null, UserRole.SELLER);

        String bodyJson = """
                {
                  "itemType": "ELECTRONICS",
                  "name": "Laptop Dell",
                  "startingPrice": 12000000,
                  "brand": "Dell",
                  "warrantyMonths": 24
                }
                """;

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            itemController.createItem(bodyJson, session);
        });

        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    // createItem thiếu itemType phải ném MISSING_REQUIRED_FIELD
    @Test
    void createItemShouldThrowWhenItemTypeIsMissing() {
        ClientSession session = loggedInSession("seller-1", UserRole.SELLER);

        String bodyJson = """
                {
                  "name": "Laptop Dell",
                  "startingPrice": 12000000
                }
                """;

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            itemController.createItem(bodyJson, session);
        });

        assertValidationError(exception, ValidationErrorCode.MISSING_REQUIRED_FIELD);
    }

    // createItem hợp lệ phải lấy sellerId từ session và gọi ItemService.addItem
    @Test
    void createItemShouldCallServiceWithSellerIdFromSession() {
        ClientSession session = loggedInSession("seller-1", UserRole.SELLER);
        ItemDetailDTO detail = sampleItemDetail("item-1", "seller-1");
        itemService.detailToReturn = detail;

        String bodyJson = """
                {
                  "itemType": "electronics",
                  "name": "Laptop Dell",
                  "startingPrice": 12000000,
                  "description": "Laptop văn phòng",
                  "yearCreated": 2022,
                  "imageUrl": "dell.png",
                  "brand": "Dell",
                  "warrantyMonths": 24
                }
                """;

        ItemDetailDTO result = itemController.createItem(bodyJson, session);

        assertSame(detail, result);

        assertEquals("ELECTRONICS", itemService.lastAddItemType);
        assertEquals("seller-1", itemService.lastAddItemData.get("sellerId"));
        assertEquals("Laptop Dell", itemService.lastAddItemData.get("name"));
        assertEquals(12000000.0, itemService.lastAddItemData.get("startingPrice"));
        assertEquals("Dell", itemService.lastAddItemData.get("brand"));
        assertEquals(24.0, ((Number) itemService.lastAddItemData.get("warrantyMonths")).doubleValue());
    }

    // createItem VEHICLE phải normalize thành VEHICLES
    @Test
    void createItemShouldNormalizeVehicleToVehicles() {
        ClientSession session = loggedInSession("seller-1", UserRole.SELLER);
        itemService.detailToReturn = sampleItemDetail("item-1", "seller-1");

        String bodyJson = """
                {
                  "itemType": "vehicle",
                  "name": "Honda",
                  "startingPrice": 10000000,
                  "model": "Wave",
                  "engineType": "Gasoline"
                }
                """;

        itemController.createItem(bodyJson, session);

        assertEquals("VEHICLES", itemService.lastAddItemType);
    }

    // =========================================================
    // updateItem()
    // =========================================================

    // updateItem thiếu itemId phải ném MISSING_REQUIRED_FIELD
    @Test
    void updateItemShouldThrowWhenItemIdIsMissing() {
        ClientSession session = loggedInSession("seller-1", UserRole.SELLER);

        String bodyJson = """
                {
                  "itemType": "ELECTRONICS",
                  "name": "Laptop Updated"
                }
                """;

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            itemController.updateItem(bodyJson, session);
        });

        assertValidationError(exception, ValidationErrorCode.MISSING_REQUIRED_FIELD);
    }

    // updateItem thiếu itemType phải ném MISSING_REQUIRED_FIELD
    @Test
    void updateItemShouldThrowWhenItemTypeIsMissing() {
        ClientSession session = loggedInSession("seller-1", UserRole.SELLER);

        String bodyJson = """
                {
                  "itemId": "item-1",
                  "name": "Laptop Updated"
                }
                """;

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            itemController.updateItem(bodyJson, session);
        });

        assertValidationError(exception, ValidationErrorCode.MISSING_REQUIRED_FIELD);
    }

    // updateItem seller là chủ sở hữu thì được update
    @Test
    void updateItemShouldAllowOwnerSeller() {
        ClientSession session = loggedInSession("seller-1", UserRole.SELLER);
        itemService.detailToReturn = sampleItemDetail("item-1", "seller-1");

        String bodyJson = """
                {
                  "itemId": "item-1",
                  "itemType": "ELECTRONICS",
                  "name": "Laptop Updated",
                  "startingPrice": 15000000
                }
                """;

        ItemDetailDTO result = itemController.updateItem(bodyJson, session);

        assertSame(itemService.detailToReturn, result);
        assertEquals("item-1", itemService.lastDetailedItemId);
        assertEquals("item-1", itemService.lastUpdateItemId);
        assertEquals("ELECTRONICS", itemService.lastUpdateItemType);
        assertEquals("Laptop Updated", itemService.lastUpdateData.get("name"));
        assertEquals(15000000.0, itemService.lastUpdateData.get("startingPrice"));
    }

    // updateItem admin được update dù không phải seller
    @Test
    void updateItemShouldAllowAdminEvenIfNotOwner() {
        ClientSession session = loggedInSession("admin-1", UserRole.ADMIN);
        itemService.detailToReturn = sampleItemDetail("item-1", "seller-1");

        String bodyJson = """
                {
                  "itemId": "item-1",
                  "itemType": "ELECTRONICS",
                  "name": "Admin Updated"
                }
                """;

        ItemDetailDTO result = itemController.updateItem(bodyJson, session);

        assertSame(itemService.detailToReturn, result);
        assertEquals("item-1", itemService.lastUpdateItemId);
    }

    // updateItem seller không phải chủ sở hữu phải bị chặn
    @Test
    void updateItemShouldDenyNonOwnerSeller() {
        ClientSession session = loggedInSession("seller-2", UserRole.SELLER);
        itemService.detailToReturn = sampleItemDetail("item-1", "seller-1");

        String bodyJson = """
                {
                  "itemId": "item-1",
                  "itemType": "ELECTRONICS",
                  "name": "Hack Update"
                }
                """;

        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            itemController.updateItem(bodyJson, session);
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.RESOURCE_OWNERSHIP_VIOLATION);
        assertNull(itemService.lastUpdateItemId);
    }

    // =========================================================
    // deleteItem()
    // =========================================================

    // deleteItem thiếu itemId phải ném MISSING_REQUIRED_FIELD
    @Test
    void deleteItemShouldThrowWhenItemIdIsMissing() {
        ClientSession session = loggedInSession("seller-1", UserRole.SELLER);

        String bodyJson = """
                {
                  "reason": "wrong item"
                }
                """;

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            itemController.deleteItem(bodyJson, session);
        });

        assertValidationError(exception, ValidationErrorCode.MISSING_REQUIRED_FIELD);
    }

    // deleteItem seller là chủ sở hữu thì update status INACTIVE
    @Test
    void deleteItemShouldSetInactiveWhenOwnerSeller() {
        ClientSession session = loggedInSession("seller-1", UserRole.SELLER);
        itemService.detailToReturn = sampleItemDetail("item-1", "seller-1");

        String bodyJson = """
                {
                  "itemId": "item-1",
                  "reason": "seller remove"
                }
                """;

        itemController.deleteItem(bodyJson, session);

        assertEquals("item-1", itemService.lastDetailedItemId);
        assertEquals("item-1", itemService.lastUpdateStatusItemId);
        assertEquals(ItemStatus.INACTIVE, itemService.lastUpdateStatus);
    }

    // deleteItem admin được xóa dù không phải chủ sở hữu
    @Test
    void deleteItemShouldAllowAdminEvenIfNotOwner() {
        ClientSession session = loggedInSession("admin-1", UserRole.ADMIN);
        itemService.detailToReturn = sampleItemDetail("item-1", "seller-1");

        String bodyJson = """
                {
                  "itemId": "item-1",
                  "reason": "violation"
                }
                """;

        itemController.deleteItem(bodyJson, session);

        assertEquals("item-1", itemService.lastUpdateStatusItemId);
        assertEquals(ItemStatus.INACTIVE, itemService.lastUpdateStatus);
    }

    // deleteItem seller không phải chủ sở hữu phải bị chặn
    @Test
    void deleteItemShouldDenyNonOwnerSeller() {
        ClientSession session = loggedInSession("seller-2", UserRole.SELLER);
        itemService.detailToReturn = sampleItemDetail("item-1", "seller-1");

        String bodyJson = """
                {
                  "itemId": "item-1",
                  "reason": "hack"
                }
                """;

        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            itemController.deleteItem(bodyJson, session);
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.RESOURCE_OWNERSHIP_VIOLATION);
        assertNull(itemService.lastUpdateStatusItemId);
    }

    // =========================================================
    // getSellerItems()
    // =========================================================

    // getSellerItems phải lấy sellerId từ session
    @Test
    void getSellerItemsShouldUseSessionUserId() {
        ClientSession session = loggedInSession("seller-1", UserRole.SELLER);

        List<ItemSummaryDTO> items = List.of(
                sampleItemDetail("item-1", "seller-1").toSummaryDTO()
        );
        itemService.sellerItemsToReturn = items;

        List<ItemSummaryDTO> result = itemController.getSellerItems(session);

        assertSame(items, result);
        assertEquals("seller-1", itemService.lastSellerId);
    }

    // getSellerItems session null phải ném BAD_REQUEST
    @Test
    void getSellerItemsShouldThrowWhenSessionIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            itemController.getSellerItems(null);
        });

        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    // =========================================================
    // getItemDetail()
    // =========================================================

    // getItemDetail owner seller được xem
    @Test
    void getItemDetailShouldAllowOwnerSeller() {
        ClientSession session = loggedInSession("seller-1", UserRole.SELLER);
        ItemDetailDTO detail = sampleItemDetail("item-1", "seller-1");
        itemService.detailToReturn = detail;

        String bodyJson = """
                {
                  "itemId": "item-1"
                }
                """;

        ItemDetailDTO result = itemController.getItemDetail(bodyJson, session);

        assertSame(detail, result);
        assertEquals("item-1", itemService.lastDetailedItemId);
    }

    // getItemDetail admin được xem
    @Test
    void getItemDetailShouldAllowAdmin() {
        ClientSession session = loggedInSession("admin-1", UserRole.ADMIN);
        ItemDetailDTO detail = sampleItemDetail("item-1", "seller-1");
        itemService.detailToReturn = detail;

        String bodyJson = """
                {
                  "itemId": "item-1"
                }
                """;

        ItemDetailDTO result = itemController.getItemDetail(bodyJson, session);

        assertSame(detail, result);
    }

    // getItemDetail seller không phải chủ sở hữu bị chặn
    @Test
    void getItemDetailShouldDenyNonOwnerSeller() {
        ClientSession session = loggedInSession("seller-2", UserRole.SELLER);
        itemService.detailToReturn = sampleItemDetail("item-1", "seller-1");

        String bodyJson = """
                {
                  "itemId": "item-1"
                }
                """;

        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            itemController.getItemDetail(bodyJson, session);
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.RESOURCE_OWNERSHIP_VIOLATION);
    }

    // getItemDetail thiếu itemId phải ném MISSING_REQUIRED_FIELD
    @Test
    void getItemDetailShouldThrowWhenItemIdIsMissing() {
        ClientSession session = loggedInSession("seller-1", UserRole.SELLER);

        String bodyJson = """
                {
                }
                """;

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            itemController.getItemDetail(bodyJson, session);
        });

        assertValidationError(exception, ValidationErrorCode.MISSING_REQUIRED_FIELD);
    }
}