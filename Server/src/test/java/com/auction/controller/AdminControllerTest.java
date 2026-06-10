package com.auction.controller;

import com.auction.config.DatabaseConnection;
import com.auction.dto.ActionLogDTO;
import com.auction.dto.CancelAuctionRequest;
import com.auction.dto.DeleteItemRequest;
import com.auction.dto.GetAuditLogsRequest;
import com.auction.dto.GetUserDashboardRequest;
import com.auction.dto.LockUserAccountRequest;
import com.auction.dto.PageDTO;
import com.auction.dto.UserDTO;
import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;
import com.auction.exception.ValidationErrorCode;
import com.auction.exception.ValidationException;
import com.auction.manage.AuctionManage;
import com.auction.models.Auction.Auction;
import com.auction.models.Entity.Entity;
import com.auction.models.Item.Electronics;
import com.auction.service.AuctionService;
import com.auction.service.ItemService;
import com.auction.service.LogService;
import com.auction.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminControllerTest {

    private AdminController adminController;
    private FakeUserService userService;
    private FakeAuctionService auctionService;
    private FakeItemService itemService;
    private FakeLogService logService;

    private MockedStatic<DatabaseConnection> mockedDbConnection;
    private Connection fakeConnection;

    @BeforeEach
    void setUp() throws Exception {
        fakeConnection = new FakeDbConnection();
        mockedDbConnection = mockStatic(DatabaseConnection.class);
        mockedDbConnection.when(DatabaseConnection::getConnection).thenReturn(fakeConnection);

        adminController = new AdminController();

        userService = new FakeUserService();
        auctionService = new FakeAuctionService();
        itemService = new FakeItemService();
        logService = new FakeLogService();

        injectField(adminController, "userService", userService);
        injectField(adminController, "auctionService", auctionService);
        injectField(adminController, "itemService", itemService);
        injectField(adminController, "logService", logService);

        clearAuctionManage();
    }

    @AfterEach
    void tearDown() {
        if (mockedDbConnection != null) {
            mockedDbConnection.close();
        }
    }

    // Inject fake service vào field private final
    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // Set id cho Entity bằng reflection
    private void setEntityId(Entity entity, String id) throws Exception {
        Field idField = Entity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    // Clear AuctionManage singleton để test deleteItem không dính auction cũ
    @SuppressWarnings("unchecked")
    private void clearAuctionManage() throws Exception {
        AuctionManage auctionManage = AuctionManage.getInstance();

        Field activeAuctionsField = AuctionManage.class.getDeclaredField("activeAuctions");
        activeAuctionsField.setAccessible(true);
        Map<String, Auction> activeAuctions = (Map<String, Auction>) activeAuctionsField.get(auctionManage);
        activeAuctions.clear();

        Field lastAccessedTimeField = AuctionManage.class.getDeclaredField("lastAccessedTime");
        lastAccessedTimeField.setAccessible(true);
        Map<String, LocalDateTime> lastAccessedTime =
                (Map<String, LocalDateTime>) lastAccessedTimeField.get(auctionManage);
        lastAccessedTime.clear();
    }

    // Check đúng mã lỗi ValidationException
    private void assertValidationError(ValidationException exception, ValidationErrorCode expectedError) {
        assertEquals(expectedError.getCode(), exception.getErrorCode());
    }

    // Tạo UserDTO mẫu
    private UserDTO sampleUserDTO() {
        return new UserDTO(
                "user-1",
                "bidder01",
                "bidder01@example.com",
                UserRole.BIDDER,
                UserStatus.ACTIVE,
                1000.0,
                0.0
        );
    }

    // Tạo ActionLogDTO mẫu
    private ActionLogDTO sampleLogDTO() {
        return new ActionLogDTO(
                "log-1",
                "admin-1",
                "LOCK_USER",
                "USER",
                "user-1",
                LocalDateTime.now()
        );
    }

    // Tạo auction mẫu có itemId liên quan đến item cần xóa
    private Auction sampleAuctionLinkedToItem(String auctionId, String itemId, String sellerId) throws Exception {
        Electronics item = new Electronics(
                "Laptop Dell",
                12000000.0,
                "Laptop văn phòng",
                2022,
                sellerId,
                "image.png",
                "Dell",
                24
        );

        setEntityId(item, itemId);

        Auction auction = new Auction(
                item,
                sellerId,
                100.0,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusHours(1)
        );

        setEntityId(auction, auctionId);

        return auction;
    }

    // Fake UserService
    private static class FakeUserService extends UserService {
        PageDTO<UserDTO> dashboardToReturn;

        int lastDashboardPage;
        int lastDashboardPageSize;

        String lastLockAdminId;
        String lastLockTargetUserId;
        UserStatus lastLockStatus;
        String lastLockReason;

        @Override
        public PageDTO<UserDTO> getAdminUserDashboard(int page, int pageSize) {
            lastDashboardPage = page;
            lastDashboardPageSize = pageSize;
            return dashboardToReturn;
        }

        @Override
        public void lockUserAccount(String adminId, String targetUserId, UserStatus status, String reason) {
            lastLockAdminId = adminId;
            lastLockTargetUserId = targetUserId;
            lastLockStatus = status;
            lastLockReason = reason;
        }
    }

    // Fake AuctionService
    private static class FakeAuctionService extends AuctionService {
        String lastCancelAuctionId;
        String lastCancelOperatorId;
        UserRole lastCancelOperatorRole;
        String lastCancelReason;

        @Override
        public void cancelAuction(String auctionId, String operatorId, UserRole operatorRole, String reason) {
            lastCancelAuctionId = auctionId;
            lastCancelOperatorId = operatorId;
            lastCancelOperatorRole = operatorRole;
            lastCancelReason = reason;
        }
    }

    // Fake ItemService
    private static class FakeItemService extends ItemService {
        String lastDeleteItemId;
        String lastDeleteAdminId;
        String lastDeleteReason;

        @Override
        public void deleteItemByAdmin(String itemId, String adminId, String reason) {
            lastDeleteItemId = itemId;
            lastDeleteAdminId = adminId;
            lastDeleteReason = reason;
        }
    }

    // Fake LogService
    private static class FakeLogService extends LogService {
        PageDTO<ActionLogDTO> logsToReturn;

        int lastPage;
        int lastPageSize;

        @Override
        public PageDTO<ActionLogDTO> getLogsForAdminDashboard(int page, int pageSize) {
            lastPage = page;
            lastPageSize = pageSize;
            return logsToReturn;
        }
    }

    // =========================================================
    // getUsersDashboard()
    // =========================================================

    // getUsersDashboard request null phải ném BAD_REQUEST
    @Test
    void getUsersDashboardShouldThrowWhenRequestIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            adminController.getUsersDashboard(null);
        });

        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    // getUsersDashboard hợp lệ phải gọi UserService.getAdminUserDashboard
    @Test
    void getUsersDashboardShouldReturnPageFromService() {
        PageDTO<UserDTO> page = new PageDTO<>(
                List.of(sampleUserDTO()),
                2,
                5,
                41
        );

        userService.dashboardToReturn = page;

        GetUserDashboardRequest request = new GetUserDashboardRequest(2, 10);

        PageDTO<UserDTO> result = adminController.getUsersDashboard(request);

        assertSame(page, result);
        assertEquals(2, userService.lastDashboardPage);
        assertEquals(10, userService.lastDashboardPageSize);
    }

    // getUsersDashboard page/pageSize không validate ở controller, truyền thẳng xuống service
    @Test
    void getUsersDashboardShouldPassInvalidPagingToServiceForServiceValidation() {
        PageDTO<UserDTO> page = new PageDTO<>(
                List.of(),
                0,
                0,
                0
        );

        userService.dashboardToReturn = page;

        GetUserDashboardRequest request = new GetUserDashboardRequest(0, -1);

        PageDTO<UserDTO> result = adminController.getUsersDashboard(request);

        assertSame(page, result);
        assertEquals(0, userService.lastDashboardPage);
        assertEquals(-1, userService.lastDashboardPageSize);
    }

    // =========================================================
    // lockUserAccount()
    // =========================================================

    // lockUserAccount request null phải ném BAD_REQUEST
    @Test
    void lockUserAccountShouldThrowWhenRequestIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            adminController.lockUserAccount("admin-1", null);
        });

        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    // lockUserAccount truyền đúng adminId, userId, status và reason xuống service
    @Test
    void lockUserAccountShouldCallUserServiceWithRequestStatusAndReason() {
        LockUserAccountRequest request = new LockUserAccountRequest(
                "user-1",
                UserStatus.BANNED,
                "vi phạm"
        );

        adminController.lockUserAccount("admin-1", request);

        assertEquals("admin-1", userService.lastLockAdminId);
        assertEquals("user-1", userService.lastLockTargetUserId);
        assertEquals(UserStatus.BANNED, userService.lastLockStatus);
        assertEquals("vi phạm", userService.lastLockReason);
    }

    // lockUserAccount hiện tại không ép BANNED, mà truyền targetStatus từ request
    @Test
    void lockUserAccountShouldPassTargetStatusFromRequest() {
        LockUserAccountRequest request = new LockUserAccountRequest(
                "user-1",
                UserStatus.ACTIVE,
                "mở lại"
        );

        adminController.lockUserAccount("admin-1", request);

        assertEquals(UserStatus.ACTIVE, userService.lastLockStatus);
        assertEquals("mở lại", userService.lastLockReason);
    }

    // lockUserAccount adminId null vẫn truyền xuống service để service validate
    @Test
    void lockUserAccountShouldPassNullAdminIdToService() {
        LockUserAccountRequest request = new LockUserAccountRequest(
                "user-1",
                UserStatus.BANNED,
                "vi phạm"
        );

        adminController.lockUserAccount(null, request);

        assertNull(userService.lastLockAdminId);
        assertEquals("user-1", userService.lastLockTargetUserId);
        assertEquals(UserStatus.BANNED, userService.lastLockStatus);
        assertEquals("vi phạm", userService.lastLockReason);
    }

    // lockUserAccount userId null vẫn truyền xuống service để service validate
    @Test
    void lockUserAccountShouldPassNullTargetUserIdToService() {
        LockUserAccountRequest request = new LockUserAccountRequest(
                null,
                UserStatus.BANNED,
                "vi phạm"
        );

        adminController.lockUserAccount("admin-1", request);

        assertEquals("admin-1", userService.lastLockAdminId);
        assertNull(userService.lastLockTargetUserId);
        assertEquals(UserStatus.BANNED, userService.lastLockStatus);
        assertEquals("vi phạm", userService.lastLockReason);
    }

    // =========================================================
    // cancelAuction()
    // =========================================================

    // cancelAuction request null phải ném BAD_REQUEST
    @Test
    void cancelAuctionShouldThrowWhenRequestIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            adminController.cancelAuction("admin-1", null);
        });

        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    // cancelAuction phải gọi AuctionService.cancelAuction với role ADMIN
    @Test
    void cancelAuctionShouldCallAuctionServiceWithAdminRole() {
        CancelAuctionRequest request = new CancelAuctionRequest(
                "auction-1",
                "phiên vi phạm",
                null
        );

        adminController.cancelAuction("admin-1", request);

        assertEquals("auction-1", auctionService.lastCancelAuctionId);
        assertEquals("admin-1", auctionService.lastCancelOperatorId);
        assertEquals(UserRole.ADMIN, auctionService.lastCancelOperatorRole);
        assertEquals("phiên vi phạm", auctionService.lastCancelReason);
    }

    // cancelAuction reason null vẫn truyền xuống service
    @Test
    void cancelAuctionShouldPassNullReasonToService() {
        CancelAuctionRequest request = new CancelAuctionRequest(
                "auction-1",
                null,
                null
        );

        adminController.cancelAuction("admin-1", request);

        assertEquals("auction-1", auctionService.lastCancelAuctionId);
        assertEquals("admin-1", auctionService.lastCancelOperatorId);
        assertEquals(UserRole.ADMIN, auctionService.lastCancelOperatorRole);
        assertNull(auctionService.lastCancelReason);
    }

    // =========================================================
    // deleteItem()
    // =========================================================

    // deleteItem request null phải ném BAD_REQUEST
    @Test
    void deleteItemShouldThrowWhenRequestIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            adminController.deleteItem("admin-1", null);
        });

        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    // deleteItem không có auction liên quan thì chỉ gọi ItemService.deleteItemByAdmin
    @Test
    void deleteItemShouldCallItemServiceWhenNoActiveAuctionLinked() {
        DeleteItemRequest request = new DeleteItemRequest(
                "item-1",
                "hàng vi phạm",
                null
        );

        adminController.deleteItem("admin-1", request);

        assertNull(auctionService.lastCancelAuctionId);

        assertEquals("item-1", itemService.lastDeleteItemId);
        assertEquals("admin-1", itemService.lastDeleteAdminId);
        assertEquals("hàng vi phạm", itemService.lastDeleteReason);
    }

    // deleteItem nếu có auction đang active liên quan thì hủy auction trước rồi xóa item
    @Test
    void deleteItemShouldCancelLinkedActiveAuctionBeforeDeletingItem() throws Exception {
        Auction linkedAuction = sampleAuctionLinkedToItem("auction-1", "item-1", "seller-1");
        AuctionManage.getInstance().addAuction(linkedAuction);

        DeleteItemRequest request = new DeleteItemRequest(
                "item-1",
                "hàng cấm",
                null
        );

        adminController.deleteItem("admin-1", request);

        assertEquals("auction-1", auctionService.lastCancelAuctionId);
        assertEquals("admin-1", auctionService.lastCancelOperatorId);
        assertEquals(UserRole.ADMIN, auctionService.lastCancelOperatorRole);
        assertTrue(auctionService.lastCancelReason.contains("Hủy tự động do vật phẩm bị Admin gỡ bỏ khỏi sàn"));
        assertTrue(auctionService.lastCancelReason.contains("hàng cấm"));

        assertEquals("item-1", itemService.lastDeleteItemId);
        assertEquals("admin-1", itemService.lastDeleteAdminId);
        assertEquals("hàng cấm", itemService.lastDeleteReason);
    }

    // deleteItem itemId null dù có active auction cũng không throw, vì "item-1".equals(null) trả false
    @Test
    void deleteItemShouldPassNullItemIdToServiceEvenWhenActiveAuctionExists() throws Exception {
        Auction linkedAuction = sampleAuctionLinkedToItem("auction-1", "item-1", "seller-1");
        AuctionManage.getInstance().addAuction(linkedAuction);

        DeleteItemRequest request = new DeleteItemRequest(
                null,
                "reason",
                null
        );

        assertDoesNotThrow(() -> {
            adminController.deleteItem("admin-1", request);
        });

        // Không hủy auction nào vì itemId null không match auction itemId
        assertNull(auctionService.lastCancelAuctionId);

        // Vẫn gọi ItemService để service validate tiếp
        assertNull(itemService.lastDeleteItemId);
        assertEquals("admin-1", itemService.lastDeleteAdminId);
        assertEquals("reason", itemService.lastDeleteReason);
    }

    // deleteItem itemId null nhưng không có auction nào thì vẫn truyền xuống service
    @Test
    void deleteItemShouldPassNullItemIdToServiceWhenNoAuctionExists() {
        DeleteItemRequest request = new DeleteItemRequest(
                null,
                "reason",
                null
        );

        adminController.deleteItem("admin-1", request);

        assertNull(auctionService.lastCancelAuctionId);
        assertNull(itemService.lastDeleteItemId);
        assertEquals("admin-1", itemService.lastDeleteAdminId);
        assertEquals("reason", itemService.lastDeleteReason);
    }

    // =========================================================
    // getAuditLogs()
    // =========================================================

    // getAuditLogs request null phải ném BAD_REQUEST
    @Test
    void getAuditLogsShouldThrowWhenRequestIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            adminController.getAuditLogs(null);
        });

        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    // getAuditLogs hợp lệ phải gọi LogService.getLogsForAdminDashboard
    @Test
    void getAuditLogsShouldReturnPageFromService() {
        PageDTO<ActionLogDTO> page = new PageDTO<>(
                List.of(sampleLogDTO()),
                1,
                3,
                25
        );

        logService.logsToReturn = page;

        GetAuditLogsRequest request = new GetAuditLogsRequest(1, 10);

        PageDTO<ActionLogDTO> result = adminController.getAuditLogs(request);

        assertSame(page, result);
        assertEquals(1, logService.lastPage);
        assertEquals(10, logService.lastPageSize);
    }

    // getAuditLogs page/pageSize không validate ở controller, truyền thẳng xuống service
    @Test
    void getAuditLogsShouldPassInvalidPagingToServiceForServiceValidation() {
        PageDTO<ActionLogDTO> page = new PageDTO<>(
                List.of(),
                0,
                0,
                0
        );

        logService.logsToReturn = page;

        GetAuditLogsRequest request = new GetAuditLogsRequest(0, -1);

        PageDTO<ActionLogDTO> result = adminController.getAuditLogs(request);

        assertSame(page, result);
        assertEquals(0, logService.lastPage);
        assertEquals(-1, logService.lastPageSize);
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