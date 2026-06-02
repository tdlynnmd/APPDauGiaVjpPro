package com.auction.service;

import com.auction.config.DatabaseConnection;
import com.auction.dao.ItemDAO;
import com.auction.dto.ItemDetailDTO;
import com.auction.dto.ItemSummaryDTO;
import com.auction.enums.ItemStatus;
import com.auction.enums.ItemType;
import com.auction.exception.AuctionErrorCode;
import com.auction.exception.AuctionException;
import com.auction.exception.ValidationErrorCode;
import com.auction.exception.ValidationException;
import com.auction.manage.ProductManage;
import com.auction.models.Item.Art;
import com.auction.models.Item.ArtFactory;
import com.auction.models.Item.Electronics;
import com.auction.models.Item.ElectronicsFactory;
import com.auction.models.Item.Item;
import com.auction.models.Item.ItemFactory;
import com.auction.models.Item.VehicleFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ItemServiceTest {

    private ItemService itemService;
    private ItemDAO itemDAO;
    private ProductManage productManage;

    private MockedStatic<DatabaseConnection> mockedDbConnection;
    private Connection fakeConnection;

    @BeforeEach
    void setUp() throws Exception {
        itemService = new ItemService();

        itemDAO = mock(ItemDAO.class);
        injectField(itemService, "itemDAO", itemDAO);

        productManage = ProductManage.getInstance();
        clearProductManage();

        ItemFactory.register(ItemType.ART, new ArtFactory());
        ItemFactory.register(ItemType.ELECTRONICS, new ElectronicsFactory());
        ItemFactory.register(ItemType.VEHICLES, new VehicleFactory());

        fakeConnection = new FakeDbConnection();
        mockedDbConnection = mockStatic(DatabaseConnection.class);
        mockedDbConnection.when(DatabaseConnection::getConnection).thenReturn(fakeConnection);
    }

    @AfterEach
    void tearDown() {
        if (mockedDbConnection != null) {
            mockedDbConnection.close();
        }
    }

    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private void clearProductManage() throws Exception {
        Field itemsField = ProductManage.class.getDeclaredField("items");
        itemsField.setAccessible(true);
        Map<String, Item> items = (Map<String, Item>) itemsField.get(productManage);
        items.clear();

        Field timeField = ProductManage.class.getDeclaredField("lastAccessedTime");
        timeField.setAccessible(true);
        Map<String, LocalDateTime> times = (Map<String, LocalDateTime>) timeField.get(productManage);
        times.clear();
    }

    private void assertValidationError(ValidationException exception, ValidationErrorCode expectedError) {
        assertEquals(expectedError.getCode(), exception.getErrorCode());
    }

    private void assertAuctionError(AuctionException exception, AuctionErrorCode expectedError) {
        assertEquals(expectedError.getCode(), exception.getErrorCode());
    }

    private Map<String, Object> electronicsData() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Laptop Dell");
        data.put("startingPrice", 12000000);
        data.put("description", "Laptop văn phòng");
        data.put("yearCreated", 2022);
        data.put("sellerId", "seller-1");
        data.put("imageUrl", "dell.png");
        data.put("brand", "Dell");
        data.put("warrantyMonths", 24);
        return data;
    }

    private Map<String, Object> artData() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Mona Lisa");
        data.put("startingPrice", 50000000);
        data.put("description", "Tranh nổi tiếng");
        data.put("yearCreated", 1503);
        data.put("sellerId", "seller-2");
        data.put("imageUrl", "mona-lisa.png");
        data.put("painter", "Leonardo da Vinci");
        data.put("artStyle", "Renaissance");
        return data;
    }

    private Map<String, Object> vehicleData() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Toyota Camry");
        data.put("startingPrice", 600000000);
        data.put("description", "Xe còn mới");
        data.put("yearCreated", 2020);
        data.put("sellerId", "seller-3");
        data.put("imageUrl", "camry.png");
        data.put("model", "Camry 2.5Q");
        data.put("engineType", "Gasoline");
        data.put("licensePlate", "30A-12345");
        data.put("kmAge", 25000.5);
        return data;
    }

    private Electronics sampleElectronics(String itemId) {
        Electronics item = new Electronics(
                "Laptop Dell", 12000000, "Laptop văn phòng", 2022, "seller-1", "dell.png", "Dell", 24
        );
        item.setId(itemId);
        return item;
    }

    private Art sampleArt(String itemId) {
        Art item = new Art(
                "Mona Lisa", 50000000, "Tranh nổi tiếng", 1503, "seller-2", "mona-lisa.png", "Leonardo da Vinci", "Renaissance"
        );
        item.setId(itemId);
        return item;
    }

    // =========================================================
    // TEST addItem()
    // =========================================================

    @Test
    void addItemShouldThrowBadRequestWhenTypeIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> itemService.addItem((ItemType) null, electronicsData()));
        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    @Test
    void addItemShouldThrowBadRequestWhenDataIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> itemService.addItem(ItemType.ELECTRONICS, null));
        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    @Test
    void addItemShouldThrowInvalidParameterWhenTypeStringIsInvalid() {
        ValidationException exception = assertThrows(ValidationException.class, () -> itemService.addItem("BOOK", electronicsData()));
        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    @Test
    void addItemShouldThrowInvalidParameterWhenPriceIsNegative() {
        Map<String, Object> data = electronicsData();
        data.put("startingPrice", -5000);

        ValidationException exception = assertThrows(ValidationException.class, () -> itemService.addItem(ItemType.ELECTRONICS, data));
        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    @Test
    void addItemShouldThrowInvalidParameterWhenYearCreatedIsInFuture() {
        Map<String, Object> data = electronicsData();
        data.put("yearCreated", LocalDateTime.now().getYear() + 5);

        ValidationException exception = assertThrows(ValidationException.class, () -> itemService.addItem(ItemType.ELECTRONICS, data));
        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    @Test
    void addItemShouldAcceptLegacyVehicleTypeString() throws Exception {
        when(itemDAO.insertItem(any(Connection.class), any(Item.class))).thenReturn(true);

        ItemDetailDTO dto = itemService.addItem("VEHICLE", vehicleData());

        assertNotNull(dto);
        assertEquals("VEHICLES", dto.getItemType());
        assertEquals("Toyota Camry", dto.getItemName());
        assertEquals("Camry 2.5Q", dto.getModel());
    }

    @Test
    void addItemShouldThrowInvalidParameterWhenRequiredFieldIsMissing() {
        Map<String, Object> data = electronicsData();
        data.remove("name");

        ValidationException exception = assertThrows(ValidationException.class, () -> itemService.addItem(ItemType.ELECTRONICS, data));
        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    @Test
    void addItemShouldThrowDatabaseErrorWhenInsertItemFails() throws Exception {
        when(itemDAO.insertItem(any(Connection.class), any(Item.class))).thenReturn(false);

        AuctionException exception = assertThrows(AuctionException.class, () -> itemService.addItem(ItemType.ELECTRONICS, electronicsData()));
        assertAuctionError(exception, AuctionErrorCode.ITEM_SAVE_FAILED);
    }

    @Test
    void addItemShouldReturnElectronicsDetailDTOWhenSuccess() throws Exception {
        when(itemDAO.insertItem(any(Connection.class), any(Item.class))).thenReturn(true);

        ItemDetailDTO dto = itemService.addItem(ItemType.ELECTRONICS, electronicsData());

        assertNotNull(dto);
        assertEquals("Laptop Dell", dto.getItemName());
        assertEquals(12000000.0, dto.getStartingPrice());
        assertEquals("ELECTRONICS", dto.getItemType());
        assertEquals("ACTIVE", dto.getStatus());
        assertEquals("Dell", dto.getBrand());
        assertEquals(24, dto.getWarrantyMonths());

        verify(itemDAO).insertItem(any(Connection.class), any(Item.class));
    }

    @Test
    void addItemShouldReturnArtDetailDTOWhenSuccess() throws Exception {
        when(itemDAO.insertItem(any(Connection.class), any(Item.class))).thenReturn(true);

        ItemDetailDTO dto = itemService.addItem(ItemType.ART, artData());

        assertNotNull(dto);
        assertEquals("Mona Lisa", dto.getItemName());
        assertEquals("ART", dto.getItemType());
        assertEquals("Leonardo da Vinci", dto.getPainter());

        verify(itemDAO).insertItem(any(Connection.class), any(Item.class));
    }

    @Test
    void addItemShouldReturnVehicleDetailDTOWhenSuccess() throws Exception {
        when(itemDAO.insertItem(any(Connection.class), any(Item.class))).thenReturn(true);

        ItemDetailDTO dto = itemService.addItem(ItemType.VEHICLES, vehicleData());

        assertNotNull(dto);
        assertEquals("Toyota Camry", dto.getItemName());
        assertEquals("VEHICLES", dto.getItemType());
        assertEquals("Camry 2.5Q", dto.getModel());

        verify(itemDAO).insertItem(any(Connection.class), any(Item.class));
    }

    // =========================================================
    // TEST getDetailedItem()
    // =========================================================

    @Test
    void getDetailedItemShouldThrowWhenItemIdIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> itemService.getDetailedItem(null));
        assertValidationError(exception, ValidationErrorCode.MISSING_REQUIRED_FIELD);
    }

    @Test
    void getDetailedItemShouldThrowWhenItemIdIsBlank() {
        ValidationException exception = assertThrows(ValidationException.class, () -> itemService.getDetailedItem("   "));
        assertValidationError(exception, ValidationErrorCode.MISSING_REQUIRED_FIELD);
    }

    @Test
    void getDetailedItemShouldThrowWhenItemNotFound() {
        when(itemDAO.findById("missing-item")).thenReturn(Optional.empty());

        AuctionException exception = assertThrows(AuctionException.class, () -> itemService.getDetailedItem("missing-item"));
        assertAuctionError(exception, AuctionErrorCode.ITEM_NOT_FOUND);
    }

    @Test
    void getDetailedItemShouldReturnDetailDTOWhenItemExistsInDatabase() {
        Electronics item = sampleElectronics("item-detail-1");
        item.setStatus(ItemStatus.ACTIVE);

        when(itemDAO.findById("item-detail-1")).thenReturn(Optional.of(item));

        ItemDetailDTO dto = itemService.getDetailedItem("item-detail-1");

        assertNotNull(dto);
        assertEquals("item-detail-1", dto.getItemId());
        assertEquals("Laptop Dell", dto.getItemName());

        assertNotNull(productManage.getProduct("item-detail-1"));
    }

    // =========================================================
    // TEST getSellerItems()
    // =========================================================

    @Test
    void getSellerItemsShouldThrowWhenSellerIdIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> itemService.getSellerItems(null));
        assertValidationError(exception, ValidationErrorCode.MISSING_REQUIRED_FIELD);
    }

    @Test
    void getSellerItemsShouldThrowWhenSellerIdIsBlank() {
        ValidationException exception = assertThrows(ValidationException.class, () -> itemService.getSellerItems("   "));
        assertValidationError(exception, ValidationErrorCode.MISSING_REQUIRED_FIELD);
    }

    @Test
    void getSellerItemsShouldReturnEmptyListWhenSellerHasNoItems() {
        when(itemDAO.findBySellerId("seller-empty")).thenReturn(List.of());

        List<ItemSummaryDTO> result = itemService.getSellerItems("seller-empty");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getSellerItemsShouldReturnSummaryDTOs() {
        Electronics item = sampleElectronics("item-summary-1");
        item.setStatus(ItemStatus.ACTIVE);

        when(itemDAO.findBySellerId("seller-1")).thenReturn(List.of(item));

        List<ItemSummaryDTO> result = itemService.getSellerItems("seller-1");

        assertEquals(1, result.size());
        ItemSummaryDTO dto = result.getFirst();
        assertEquals("item-summary-1", dto.getItemId());
        assertEquals("Laptop Dell", dto.getItemName());
        assertEquals("ACTIVE", dto.getStatus());
    }

    // =========================================================
    // TEST updateItemStatus()
    // =========================================================

    @Test
    void updateItemStatusShouldThrowWhenItemIdIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> itemService.updateItemStatus(null, ItemStatus.SOLD));
        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    @Test
    void updateItemStatusShouldThrowWhenNewStatusIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> itemService.updateItemStatus("item-1", null));
        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    @Test
    void updateItemStatusShouldThrowDatabaseErrorWhenDaoUpdateFails() throws Exception {
        when(itemDAO.updateStatus(any(Connection.class), eq("item-1"), eq(ItemStatus.SOLD.name()))).thenReturn(false);

        AuctionException exception = assertThrows(AuctionException.class, () -> itemService.updateItemStatus("item-1", ItemStatus.SOLD));
        assertAuctionError(exception, AuctionErrorCode.ITEM_STATUS_UPDATE_FAILED);
    }

    @Test
    void updateItemStatusShouldUpdateRamItemWhenItemIsAlreadyCached() throws Exception {
        Electronics item = sampleElectronics("item-status-1");
        productManage.addProduct(item);

        when(itemDAO.updateStatus(any(Connection.class), eq("item-status-1"), eq(ItemStatus.SOLD.name()))).thenReturn(true);

        itemService.updateItemStatus("item-status-1", ItemStatus.SOLD);

        assertEquals(ItemStatus.SOLD, item.getStatus());

        verify(itemDAO).updateStatus(any(Connection.class), eq("item-status-1"), eq(ItemStatus.SOLD.name()));
    }

    @Test
    void updateItemStatusShouldLoadItemFromDatabaseWhenItemIsNotCached() throws Exception {
        Electronics item = sampleElectronics("item-status-2");
        item.setStatus(ItemStatus.ACTIVE);

        when(itemDAO.updateStatus(any(Connection.class), eq("item-status-2"), eq(ItemStatus.ACTIVE.name()))).thenReturn(true);
        when(itemDAO.findById("item-status-2")).thenReturn(Optional.of(item));

        itemService.updateItemStatus("item-status-2", ItemStatus.ACTIVE);

        verify(itemDAO).findById("item-status-2");
        assertNotNull(productManage.getProduct("item-status-2"));
    }

    // 🌟 TEST CASE THÊM MỚI: Trục xuất dọn dẹp cache RAM sạch sẽ khi vật phẩm đã bán chốt hạ (SOLD)
    @Test
    @org.junit.jupiter.api.DisplayName("Trục xuất vật phẩm khỏi bộ đệm RAM khi chuyển trạng thái sang SOLD")
    void updateItemStatusShouldEvictFromRamWhenStatusIsSold() throws Exception {
        Electronics item = sampleElectronics("item-status-evict");
        productManage.addProduct(item); // Nạp RAM trước

        when(itemDAO.updateStatus(any(Connection.class), eq("item-status-evict"), eq(ItemStatus.SOLD.name()))).thenReturn(true);

        itemService.updateItemStatus("item-status-evict", ItemStatus.SOLD);

        // Khẳng định chốt chặn: Vật phẩm bắt buộc bị bay màu khỏi ProductManage RAM để chống rò rỉ bộ nhớ
        assertNull(productManage.getProduct("item-status-evict"));
    }

    // =========================================================
    // TEST updateItemInfo()
    // =========================================================

    @Test
    void updateItemInfoShouldThrowBadRequestWhenItemIdIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> itemService.updateItemInfo(null, ItemType.ELECTRONICS, Map.of("name", "New Name")));
        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    @Test
    void updateItemInfoShouldThrowBadRequestWhenTypeIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> itemService.updateItemInfo("item-1", (ItemType) null, Map.of("name", "New Name")));
        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    @Test
    void updateItemInfoShouldThrowBadRequestWhenIncomingDataIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> itemService.updateItemInfo("item-1", ItemType.ELECTRONICS, null));
        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    @Test
    void updateItemInfoShouldThrowWhenItemNotFound() {
        when(itemDAO.findById("missing-item")).thenReturn(Optional.empty());

        AuctionException exception = assertThrows(AuctionException.class, () -> itemService.updateItemInfo("missing-item", ItemType.ELECTRONICS, Map.of("name", "New Name")));
        assertAuctionError(exception, AuctionErrorCode.ITEM_NOT_FOUND);
    }

    @Test
    void updateItemInfoShouldThrowWhenTypeDoesNotMatchExistingItemType() {
        Electronics item = sampleElectronics("item-type-mismatch");
        item.setStatus(ItemStatus.ACTIVE);

        when(itemDAO.findById("item-type-mismatch")).thenReturn(Optional.of(item));

        ValidationException exception = assertThrows(ValidationException.class, () -> itemService.updateItemInfo("item-type-mismatch", ItemType.ART, Map.of("name", "New Name")));
        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    @Test
    void updateItemInfoShouldThrowWhenItemIsLocked() {
        Electronics item = new Electronics(
                "locked-item", "Laptop Dell", 12000000, "Mô tả", 2022, "seller-1", "dell.png",
                ItemStatus.INACTIVE, LocalDateTime.now(), "Dell", 24
        );

        when(itemDAO.findById("locked-item")).thenReturn(Optional.of(item));

        AuctionException exception = assertThrows(AuctionException.class, () -> itemService.updateItemInfo("locked-item", ItemType.ELECTRONICS, Map.of("name", "New Name")));
        assertAuctionError(exception, AuctionErrorCode.ITEM_IS_LOCKED);
    }

    @Test
    void updateItemInfoShouldThrowInvalidParameterWhenIncomingDataIsInvalid() {
        Electronics item = sampleElectronics("item-invalid-update");
        item.setStatus(ItemStatus.ACTIVE);

        when(itemDAO.findById("item-invalid-update")).thenReturn(Optional.of(item));

        Map<String, Object> incomingData = new HashMap<>();
        incomingData.put("startingPrice", "abc");

        ValidationException exception = assertThrows(ValidationException.class, () -> itemService.updateItemInfo("item-invalid-update", ItemType.ELECTRONICS, incomingData));
        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    @Test
    void updateItemInfoShouldThrowDatabaseErrorWhenDaoUpdateFails() throws Exception {
        Electronics item = sampleElectronics("item-update-fail");
        item.setStatus(ItemStatus.ACTIVE);

        when(itemDAO.findById("item-update-fail")).thenReturn(Optional.of(item));
        when(itemDAO.updateItem(any(Connection.class), any(Item.class))).thenReturn(false);

        Map<String, Object> incomingData = new HashMap<>();
        incomingData.put("name", "Laptop Dell Updated");

        AuctionException exception = assertThrows(AuctionException.class, () -> itemService.updateItemInfo("item-update-fail", ItemType.ELECTRONICS, incomingData));
        assertAuctionError(exception, AuctionErrorCode.UPDATE_FAILED);
    }

    @Test
    void updateItemInfoShouldUpdateElectronicsWhenValid() throws Exception {
        Electronics item = sampleElectronics("item-update-success");
        item.setStatus(ItemStatus.ACTIVE);

        when(itemDAO.findById("item-update-success")).thenReturn(Optional.of(item));
        when(itemDAO.updateItem(any(Connection.class), any(Item.class))).thenReturn(true);

        Map<String, Object> incomingData = new HashMap<>();
        incomingData.put("name", "Laptop Dell Updated");
        incomingData.put("startingPrice", 15000000);
        incomingData.put("yearCreated", 2023);
        incomingData.put("brand", "Dell Pro");
        incomingData.put("warrantyMonths", 36);

        ItemDetailDTO dto = itemService.updateItemInfo("item-update-success", ItemType.ELECTRONICS, incomingData);

        assertNotNull(dto);
        assertEquals("Laptop Dell Updated", dto.getItemName());
        assertEquals(15000000.0, dto.getStartingPrice());
        assertEquals(2023, dto.getYearCreated());
        assertEquals("Dell Pro", dto.getBrand());
        assertEquals(36, dto.getWarrantyMonths());

        verify(itemDAO).updateItem(any(Connection.class), any(Item.class));
    }

    // 🌟 TEST CASE THÊM MỚI: Đa hình kiểm thử - Cập nhật thuộc tính đặc thù hình thái ART
    @Test
    @org.junit.jupiter.api.DisplayName("Cập nhật thành công thông tin vật phẩm loại hình Tranh Nghệ Thuật (ART)")
    void updateItemInfoShouldUpdateArtWhenValid() throws Exception {
        Art art = sampleArt("art-update-success");
        art.setStatus(ItemStatus.ACTIVE);

        when(itemDAO.findById("art-update-success")).thenReturn(Optional.of(art));
        when(itemDAO.updateItem(any(Connection.class), any(Item.class))).thenReturn(true);

        Map<String, Object> incomingData = new HashMap<>();
        incomingData.put("name", "Mona Lisa Smile");
        incomingData.put("painter", "Da Vinci Professional");
        incomingData.put("artStyle", "Modern Renaissance");

        ItemDetailDTO dto = itemService.updateItemInfo("art-update-success", ItemType.ART, incomingData);

        assertNotNull(dto);
        assertEquals("Mona Lisa Smile", dto.getItemName());
        assertEquals("Da Vinci Professional", dto.getPainter());
        assertEquals("Modern Renaissance", dto.getArtStyle());
    }

    // 🌟 TEST CASE THÊM MỚI: Đa hình kiểm thử - Cập nhật thuộc tính đặc thù hình thái VEHICLES
    @Test
    @org.junit.jupiter.api.DisplayName("Cập nhật thành công thông tin vật phẩm loại hình Phương Tiện (VEHICLES)")
    void updateItemInfoShouldUpdateVehicleWhenValid() throws Exception {
        // Sử dụng trực tiếp Factory để sinh đối tượng Vehicle hoàn hảo an toàn hình thái cấu trúc
        Map<String, Object> initData = vehicleData();
        initData.put("id", "vehicle-update-success");
        initData.put("status", ItemStatus.ACTIVE);
        Item vehicle = ItemFactory.createItem(ItemType.VEHICLES, initData);

        when(itemDAO.findById("vehicle-update-success")).thenReturn(Optional.of(vehicle));
        when(itemDAO.updateItem(any(Connection.class), any(Item.class))).thenReturn(true);

        Map<String, Object> incomingData = new HashMap<>();
        incomingData.put("name", "Toyota Camry Luxury");
        incomingData.put("model", "Camry 2026 Edition");
        incomingData.put("kmAge", 30000.0);

        ItemDetailDTO dto = itemService.updateItemInfo("vehicle-update-success", ItemType.VEHICLES, incomingData);

        assertNotNull(dto);
        assertEquals("Toyota Camry Luxury", dto.getItemName());
        assertEquals("Camry 2026 Edition", dto.getModel());
        assertEquals(30000.0, dto.getKmAge());
    }

    @Test
    void updateItemInfoShouldAcceptTypeString() throws Exception {
        Electronics item = sampleElectronics("item-update-string-type");
        item.setStatus(ItemStatus.ACTIVE);

        when(itemDAO.findById("item-update-string-type")).thenReturn(Optional.of(item));
        when(itemDAO.updateItem(any(Connection.class), any(Item.class))).thenReturn(true);

        Map<String, Object> incomingData = new HashMap<>();
        incomingData.put("name", "New Name From String Type");

        ItemDetailDTO dto = itemService.updateItemInfo("item-update-string-type", "ELECTRONICS", incomingData);

        assertEquals("New Name From String Type", dto.getItemName());
    }

    private static class FakeDbConnection implements Connection {
        @Override public void setAutoCommit(boolean autoCommit) {}
        @Override public boolean getAutoCommit() { return true; }
        @Override public void commit() {}
        @Override public void rollback() {}
        @Override public void close() {}
        @Override public boolean isClosed() { return false; }
        @Override public <T> T unwrap(Class<T> iface) { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
        @Override public java.sql.Statement createStatement() { return null; }
        @Override public java.sql.PreparedStatement prepareStatement(String sql) { return null; }
        @Override public java.sql.CallableStatement prepareCall(String sql) { return null; }
        @Override public String nativeSQL(String sql) { return null; }
        @Override public java.sql.DatabaseMetaData getMetaData() { return null; }
        @Override public void setReadOnly(boolean readOnly) {}
        @Override public boolean isReadOnly() { return false; }
        @Override public void setCatalog(String catalog) {}
        @Override public String getCatalog() { return null; }
        @Override public void setTransactionIsolation(int level) {}
        @Override public int getTransactionIsolation() { return Connection.TRANSACTION_NONE; }
        @Override public java.sql.SQLWarning getWarnings() { return null; }
        @Override public void clearWarnings() {}
        @Override public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency) { return null; }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) { return null; }
        @Override public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) { return null; }
        @Override public java.util.Map<String, Class<?>> getTypeMap() { return null; }
        @Override public void setTypeMap(java.util.Map<String, Class<?>> map) {}
        @Override public void setHoldability(int holdability) {}
        @Override public int getHoldability() { return 0; }
        @Override public java.sql.Savepoint setSavepoint() { return null; }
        @Override public java.sql.Savepoint setSavepoint(String name) { return null; }
        @Override public void rollback(java.sql.Savepoint savepoint) {}
        @Override public void releaseSavepoint(java.sql.Savepoint savepoint) {}
        @Override public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) { return null; }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) { return null; }
        @Override public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) { return null; }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) { return null; }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int[] columnIndexes) { return null; }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, String[] columnNames) { return null; }
        @Override public java.sql.Clob createClob() { return null; }
        @Override public java.sql.Blob createBlob() { return null; }
        @Override public java.sql.NClob createNClob() { return null; }
        @Override public java.sql.SQLXML createSQLXML() { return null; }
        @Override public boolean isValid(int timeout) { return true; }
        @Override public void setClientInfo(String name, String value) {}
        @Override public void setClientInfo(java.util.Properties properties) {}
        @Override public String getClientInfo(String name) { return null; }
        @Override public java.util.Properties getClientInfo() { return null; }
        @Override public java.sql.Array createArrayOf(String typeName, Object[] elements) { return null; }
        @Override public java.sql.Struct createStruct(String typeName, Object[] attributes) { return null; }
        @Override public void setSchema(String schema) {}
        @Override public String getSchema() { return null; }
        @Override public void abort(java.util.concurrent.Executor executor) {}
        @Override public void setNetworkTimeout(java.util.concurrent.Executor executor, int milliseconds) {}
        @Override public int getNetworkTimeout() { return 0; }
    }
}