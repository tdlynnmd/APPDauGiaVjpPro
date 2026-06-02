package com.auction.service;

import com.auction.config.DatabaseConnection;
import com.auction.dao.LogDAO;
import com.auction.dao.impl.UserDAOImpl;
import com.auction.dto.*;
import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;
import com.auction.exception.AuthErrorCode;
import com.auction.exception.AuthenticationException;
import com.auction.exception.ValidationErrorCode;
import com.auction.exception.ValidationException;
import com.auction.exception.WalletErrorCode;
import com.auction.exception.WalletException;
import com.auction.manage.ConnectionManage;
import com.auction.manage.UserManage;
import com.auction.models.User.Admin;
import com.auction.models.User.Bidder;
import com.auction.models.User.Seller;
import com.auction.models.User.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

class UserServiceTest {

    private UserService userService;
    private FakeUserDAO userDAO;
    private FakeLogDAO logDAO;
    private ConnectionManage connectionManage;

    private MockedStatic<DatabaseConnection> mockedDbConnection;
    private Connection fakeConnection;

    @BeforeEach
    void setUp() throws Exception {
        userService = new UserService();

        userDAO = new FakeUserDAO();
        logDAO = new FakeLogDAO();

        injectField(userService, "userDAO", userDAO);
        injectField(userService, "logDAO", logDAO);

        connectionManage = ConnectionManage.getInstance();
        injectField(userService, "connectionManage", connectionManage);

        fakeConnection = new FakeDbConnection();
        mockedDbConnection = mockStatic(DatabaseConnection.class);
        mockedDbConnection.when(DatabaseConnection::getConnection).thenReturn(fakeConnection);

        clearUserManageCache();
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

    private void clearUserManageCache() {
        try {
            UserManage userManage = UserManage.getInstance();
            for (Field field : UserManage.class.getDeclaredFields()) {
                if (java.util.Map.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    java.util.Map<?, ?> map = (java.util.Map<?, ?>) field.get(userManage);
                    if (map != null) map.clear();
                } else if (java.util.Collection.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    java.util.Collection<?> col = (java.util.Collection<?>) field.get(userManage);
                    if (col != null) col.clear();
                }
            }

            for (Field field : ConnectionManage.class.getDeclaredFields()) {
                if (java.util.Map.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    java.util.Map<Object, Object> map = (java.util.Map<Object, Object>) field.get(connectionManage);
                    if (map != null) map.clear();
                } else if (java.util.Collection.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    java.util.Collection<Object> col = (java.util.Collection<Object>) field.get(connectionManage);
                    if (col != null) col.clear();
                }
            }
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("unchecked")
    private void setBidderOnline(String userId, boolean online) throws Exception {
        for (Field field : ConnectionManage.class.getDeclaredFields()) {
            if (java.util.Map.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                java.util.Map<Object, Object> map = (java.util.Map<Object, Object>) field.get(connectionManage);
                if (map != null) {
                    if (online) {
                        map.put(userId, new java.util.HashSet<>());
                    } else {
                        map.remove(userId);
                    }
                }
            } else if (java.util.Collection.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                java.util.Collection<Object> col = (java.util.Collection<Object>) field.get(connectionManage);
                if (col != null) {
                    if (online) col.add(userId);
                    else col.remove(userId);
                }
            }
        }
    }

    private void assertValidationError(ValidationException exception, ValidationErrorCode expectedError) {
        assertEquals(expectedError.getCode(), exception.getErrorCode());
    }

    private void assertWalletError(WalletException exception) {
        assertEquals(WalletErrorCode.TRANSACTION_FAILED.getCode(), exception.getErrorCode());
    }

    private void assertAuthError(AuthenticationException exception) {
        assertEquals(AuthErrorCode.USER_NOT_FOUND.getCode(), exception.getErrorCode());
    }

    // =========================================================
    // FAKE DAO
    // =========================================================

    private static class FakeUserDAO extends UserDAOImpl {
        boolean addAvailableBalanceResult = true;
        boolean withdrawAvailableBalanceResult = true;
        boolean updateStatusResult = true;
        User findByIdResult = null;
        // 🔥 THÊM BIẾN NÀY: Để chủ động cấu hình tổng số user trả về khi test
        long countTotalUsersResult = 3;

        String lastAddBalanceUserId;
        double lastAddBalanceAmount;

        String lastWithdrawUserId;
        double lastWithdrawAmount;

        String lastUpdateStatusUserId;
        String lastUpdateStatus;

        List<User> paginatedUsers = new ArrayList<>();
        int lastPageSize;
        int lastOffset;

        // 🔥 GHI ĐÈ THÊM HÀM NÀY: Chặn đứng không cho chọc xuống hàm DB thật nữa
        @Override
        public long countTotalUsers() {
            return countTotalUsersResult;
        }

        @Override
        public Optional<User> findById(String id) {
            return Optional.ofNullable(findByIdResult);
        }

        @Override
        public boolean addAvailableBalance(Connection conn, String userId, double amount) {
            lastAddBalanceUserId = userId;
            lastAddBalanceAmount = amount;
            return addAvailableBalanceResult;
        }

        @Override
        public boolean withdrawAvailableBalance(Connection conn, String userId, double amount) {
            lastWithdrawUserId = userId;
            lastWithdrawAmount = amount;
            return withdrawAvailableBalanceResult;
        }

        @Override
        public boolean updateStatus(Connection conn, String userId, String status) {
            lastUpdateStatusUserId = userId;
            lastUpdateStatus = status;
            return updateStatusResult;
        }

        @Override
        public List<User> findPaginated(int limit, int offset) {
            lastPageSize = limit;
            lastOffset = offset;
            return paginatedUsers;
        }
    }

    private static class FakeLogDAO implements LogDAO {
        boolean insertLogCalled = false;

        String lastAdminId;
        String lastActionDetail;
        String lastTargetType;
        String lastTargetId;

        @Override
        public void insertLog(Connection conn, String logId, String adminId, String actionDetail, String targetType, String targetId) {
            insertLogCalled = true;
            lastAdminId = adminId;
            lastActionDetail = actionDetail;
            lastTargetType = targetType;
            lastTargetId = targetId;
        }

        @Override
        public List<com.auction.dto.ActionLogDTO> findPaginatedLogs(int limit, int offset) {
            return new ArrayList<>();
        }

        @Override
        public long getTotalLogCount() {
            return 0;
        }
    }

    // =========================================================
    // HELPER TẠO USER MẪU
    // =========================================================

    private Bidder sampleBidder(String id) {
        return new Bidder(
                id, "bidder_" + id, id + "@example.com", "hashed-password",
                UserRole.BIDDER, 500.0, 100.0, UserStatus.ACTIVE,
                LocalDateTime.now().minusDays(1), LocalDateTime.now()
        );
    }

    private Seller sampleSeller(String id) {
        return new Seller(
                id, "seller_" + id, id + "@example.com", "hashed-password",
                UserRole.SELLER, 800.0, 50.0, UserStatus.ACTIVE,
                LocalDateTime.now().minusDays(1), LocalDateTime.now(), 4.5
        );
    }

    private Admin sampleAdmin(String id) {
        return new Admin(
                id, "admin_" + id, id + "@example.com", "hashed-password",
                UserRole.ADMIN, 0.0, 0.0, UserStatus.ACTIVE,
                LocalDateTime.now().minusDays(1), LocalDateTime.now()
        );
    }

    // =========================================================
    // depositMoney()
    // =========================================================

    @Test
    void depositMoneyShouldThrowWhenBidderIdIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> userService.depositMoney(null, 100.0));
        assertValidationError(exception, ValidationErrorCode.MISSING_REQUIRED_FIELD);
    }

    @Test
    void depositMoneyShouldThrowWhenAmountIsZero() {
        ValidationException exception = assertThrows(ValidationException.class, () -> userService.depositMoney("bidder-1", 0.0));
        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    @Test
    void depositMoneyShouldThrowWhenAmountIsNegative() {
        ValidationException exception = assertThrows(ValidationException.class, () -> userService.depositMoney("bidder-1", -50.0));
        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    @Test
    void depositMoneyShouldThrowWhenDaoAddBalanceFails() {
        userDAO.addAvailableBalanceResult = false;
        WalletException exception = assertThrows(WalletException.class, () -> userService.depositMoney("bidder-1", 100.0));
        assertWalletError(exception);
    }

    @Test
    void depositMoneyShouldNotThrowWhenDaoAddBalanceSuccess() {
        userDAO.addAvailableBalanceResult = true;

        assertDoesNotThrow(() -> userService.depositMoney("bidder-1", 100.0));

        assertEquals("bidder-1", userDAO.lastAddBalanceUserId);
        assertEquals(100.0, userDAO.lastAddBalanceAmount);
    }

    // =========================================================
    // withdrawMoney()
    // =========================================================

    @Test
    void withdrawMoneyShouldThrowWhenBidderIdIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> userService.withdrawMoney(null, 100.0));
        assertValidationError(exception, ValidationErrorCode.MISSING_REQUIRED_FIELD);
    }

    @Test
    void withdrawMoneyShouldThrowWhenAmountIsZero() {
        ValidationException exception = assertThrows(ValidationException.class, () -> userService.withdrawMoney("bidder-1", 0.0));
        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    @Test
    void withdrawMoneyShouldThrowWhenAmountIsNegative() {
        ValidationException exception = assertThrows(ValidationException.class, () -> userService.withdrawMoney("bidder-1", -100.0));
        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    @Test
    void withdrawMoneyShouldThrowWhenDaoWithdrawFails() {
        userDAO.findByIdResult = sampleBidder("bidder-1");
        userDAO.withdrawAvailableBalanceResult = false;

        WalletException exception = assertThrows(WalletException.class, () -> userService.withdrawMoney("bidder-1", 100.0));
        assertWalletError(exception);
    }

    @Test
    void withdrawMoneyShouldThrowWhenBalanceIsInsufficient() {
        userDAO.findByIdResult = sampleBidder("bidder-low-balance");

        WalletException exception = assertThrows(WalletException.class, () -> userService.withdrawMoney("bidder-low-balance", 600.0));
        assertEquals(WalletErrorCode.INSUFFICIENT_BALANCE.getCode(), exception.getErrorCode());
    }

    @Test
    void withdrawMoneyShouldThrowWhenUserIsBanned() {
        Bidder bannedBidder = sampleBidder("bidder-banned");
        bannedBidder.setStatus(UserStatus.BANNED);
        userDAO.findByIdResult = bannedBidder;

        WalletException exception = assertThrows(WalletException.class, () -> userService.withdrawMoney("bidder-banned", 100.0));
        assertEquals(WalletErrorCode.ACCOUNT_BANNED.getCode(), exception.getErrorCode());
    }

    @Test
    void withdrawMoneyShouldNotThrowWhenDaoWithdrawSuccess() {
        userDAO.findByIdResult = sampleBidder("bidder-1");
        userDAO.withdrawAvailableBalanceResult = true;

        assertDoesNotThrow(() -> userService.withdrawMoney("bidder-1", 100.0));

        assertEquals("bidder-1", userDAO.lastWithdrawUserId);
        assertEquals(100.0, userDAO.lastWithdrawAmount);
    }

    // =========================================================================
    // 🛠️ ĐÃ ĐỒNG BỘ CHUẨN XÁC: Gọi hàm .getData() khớp với cấu trúc PageDTO mới
    // =========================================================================

    @Test
    void getAdminUserDashboardShouldThrowWhenPageInvalid() {
        ValidationException exception = assertThrows(ValidationException.class, () -> userService.getAdminUserDashboard(0, 10));
        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    @Test
    void getAdminUserDashboardShouldThrowWhenPageSizeInvalid() {
        ValidationException exception = assertThrows(ValidationException.class, () -> userService.getAdminUserDashboard(1, 0));
        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    @Test
    void getAdminUserDashboardShouldReturnEmptyListWhenDaoReturnsEmptyList() {
        userDAO.paginatedUsers = List.of();

        PageDTO<UserDTO> result = userService.getAdminUserDashboard(1, 10);

        assertNotNull(result);
        assertTrue(result.getData().isEmpty()); // 🔥 SỬA: Dùng .getData()
        assertEquals(10, userDAO.lastPageSize);
        assertEquals(0, userDAO.lastOffset);
    }

    @Test
    void getAdminUserDashboardShouldCalculateOffsetCorrectly() {
        userDAO.paginatedUsers = List.of();
        userService.getAdminUserDashboard(3, 20);
        assertEquals(20, userDAO.lastPageSize);
        assertEquals(40, userDAO.lastOffset);
    }

    @Test
    void getAdminUserDashboardShouldConvertBidderToBidderDTO() {
        Bidder bidder = sampleBidder("bidder-dashboard-1");
        bidder.addJoinedAuction("auction-1");
        bidder.addJoinedAuction("auction-2");

        userDAO.paginatedUsers = List.of(bidder);

        PageDTO<UserDTO> result = userService.getAdminUserDashboard(1, 10);
        List<UserDTO> content = result.getData(); // 🔥 SỬA: Dùng .getData() thay cho .getContent()

        assertEquals(1, content.size());
        assertInstanceOf(BidderDTO.class, content.getFirst());

        BidderDTO dto = (BidderDTO) content.getFirst();
        assertEquals("bidder-dashboard-1", dto.getId());
        assertEquals("bidder_bidder-dashboard-1", dto.getUsername());
        assertEquals("bidder-dashboard-1@example.com", dto.getEmail());
        assertEquals(UserRole.BIDDER, dto.getRole());
        assertEquals(UserStatus.ACTIVE, dto.getStatus());
        assertEquals(500.0, dto.getAvailableBalance());
        assertEquals(100.0, dto.getFrozenBalance());
        assertEquals(List.of("auction-1", "auction-2"), dto.getJoinedAuctionIds());
    }

    @Test
    void getAdminUserDashboardShouldConvertSellerToSellerDTO() {
        Seller seller = sampleSeller("seller-dashboard-1");
        userDAO.paginatedUsers = List.of(seller);

        PageDTO<UserDTO> result = userService.getAdminUserDashboard(1, 10);
        List<UserDTO> content = result.getData(); // 🔥 SỬA: Dùng .getData()

        assertEquals(1, content.size());
        assertInstanceOf(SellerDTO.class, content.getFirst());

        SellerDTO dto = (SellerDTO) content.getFirst();
        assertEquals("seller-dashboard-1", dto.getId());
        assertEquals("seller_seller-dashboard-1", dto.getUsername());
        assertEquals("seller-dashboard-1@example.com", dto.getEmail());
        assertEquals(UserRole.SELLER, dto.getRole());
        assertEquals(UserStatus.ACTIVE, dto.getStatus());
        assertEquals(800.0, dto.getAvailableBalance());
        assertEquals(50.0, dto.getFrozenBalance());
        assertEquals(4.5, dto.getRating());
    }

    @Test
    void getAdminUserDashboardShouldConvertAdminToAdminDTO() {
        Admin admin = sampleAdmin("admin-dashboard-1");
        userDAO.paginatedUsers = List.of(admin);

        PageDTO<UserDTO> result = userService.getAdminUserDashboard(1, 10);
        List<UserDTO> content = result.getData(); // 🔥 SỬA: Dùng .getData()

        assertEquals(1, content.size());
        assertInstanceOf(AdminDTO.class, content.getFirst());

        AdminDTO dto = (AdminDTO) content.getFirst();
        assertEquals("admin-dashboard-1", dto.getId());
        assertEquals("admin_admin-dashboard-1", dto.getUsername());
        assertEquals("admin-dashboard-1@example.com", dto.getEmail());
        assertEquals(UserRole.ADMIN, dto.getRole());
        assertEquals(UserStatus.ACTIVE, dto.getStatus());
    }

    @Test
    void getAdminUserDashboardShouldConvertMixedUsersToCorrectDTOs() {
        Bidder bidder = sampleBidder("bidder-mixed");
        Seller seller = sampleSeller("seller-mixed");
        Admin admin = sampleAdmin("admin-mixed");

        userDAO.paginatedUsers = List.of(bidder, seller, admin);

        PageDTO<UserDTO> result = userService.getAdminUserDashboard(1, 10);
        List<UserDTO> content = result.getData(); // 🔥 SỬA: Dùng .getData()

        assertEquals(3, content.size());
        assertInstanceOf(BidderDTO.class, content.get(0));
        assertInstanceOf(SellerDTO.class, content.get(1));
        assertInstanceOf(AdminDTO.class, content.get(2));
    }

    // =========================================================
    // lockUserAccount()
    // =========================================================

    @Test
    void lockUserAccountShouldThrowWhenAdminIdIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> userService.lockUserAccount(null, "user-1", UserStatus.BANNED));
        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    @Test
    void lockUserAccountShouldThrowWhenUserIdIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> userService.lockUserAccount("admin-1", null, UserStatus.BANNED));
        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    @Test
    void lockUserAccountShouldThrowWhenTargetStatusIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> userService.lockUserAccount("admin-1", "user-1", null));
        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    @Test
    void lockUserAccountShouldThrowWhenTargetStatusIsNotBanned() {
        ValidationException exception = assertThrows(ValidationException.class, () -> userService.lockUserAccount("admin-1", "user-1", UserStatus.ACTIVE));
        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    @Test
    void lockUserAccountShouldThrowWhenDaoUpdateStatusFails() {
        userDAO.updateStatusResult = false;
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> userService.lockUserAccount("admin-1", "user-1", UserStatus.BANNED));
        assertAuthError(exception);
    }

    @Test
    void lockUserAccountShouldForceDisconnectUserWhenOnline() throws Exception {
        userDAO.updateStatusResult = true;

        Bidder onlineUser = sampleBidder("user-online-123");
        UserManage.getInstance().addUser(onlineUser);
        setBidderOnline("user-online-123", true);

        assertDoesNotThrow(() -> userService.lockUserAccount("admin-1", "user-online-123", UserStatus.BANNED));

        // Chờ tối đa 3 giây (3000ms) để luồng chạy ẩn FORCE_LOGOUT_SCHEDULER (hoãn 300ms) hoàn tất xử lý
        boolean isLoggedOut = false;
        for (int i = 0; i < 60; i++) {
            if (UserManage.getInstance().getUser("user-online-123") == null && !connectionManage.isUserOnline("user-online-123")) {
                isLoggedOut = true;
                break;
            }
            Thread.sleep(50);
        }

        assertTrue(isLoggedOut, "Người dùng phải bị ngắt kết nối và xóa khỏi bộ nhớ đệm RAM trong vòng 3 giây");
    }

    @Test
    void lockUserAccountShouldInsertLogWhenSuccess() {
        userDAO.updateStatusResult = true;

        assertDoesNotThrow(() -> userService.lockUserAccount("admin-1", "user-1", UserStatus.BANNED, "policy violation"));

        assertEquals("user-1", userDAO.lastUpdateStatusUserId);
        assertEquals(UserStatus.BANNED.name(), userDAO.lastUpdateStatus);

        assertTrue(logDAO.insertLogCalled);
        assertEquals("admin-1", logDAO.lastAdminId);
        assertEquals("USER", logDAO.lastTargetType);
        assertEquals("user-1", logDAO.lastTargetId);
        assertTrue(logDAO.lastActionDetail.contains("BANNED"));
        assertTrue(logDAO.lastActionDetail.contains("policy violation"));
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
