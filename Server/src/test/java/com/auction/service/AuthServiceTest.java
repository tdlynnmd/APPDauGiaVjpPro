package com.auction.service;

import com.auction.config.DatabaseConnection;
import com.auction.dao.UserDAO;
import com.auction.dto.AdminDTO;
import com.auction.dto.BidderDTO;
import com.auction.dto.SellerDTO;
import com.auction.dto.UserDTO;
import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;
import com.auction.exception.AuthErrorCode;
import com.auction.exception.AuthenticationException;
import com.auction.models.User.Admin;
import com.auction.models.User.AdminFactory;
import com.auction.models.User.Bidder;
import com.auction.models.User.BidderFactory;
import com.auction.models.User.Seller;
import com.auction.models.User.SellerFactory;
import com.auction.models.User.User;
import com.auction.models.User.UserFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private AuthService authService;
    private UserDAO userDAO;

    // 🔥 BỔ SUNG: Hạ tầng Mock tĩnh đánh chặn kết nối DB vật lý
    private MockedStatic<DatabaseConnection> mockedDbConnection;
    private Connection fakeConnection;

    /**
     * Hàm này chạy trước mỗi test.
     */
    @BeforeEach
    void setUp() throws Exception {
        UserFactory.setRegistry(UserRole.BIDDER, new BidderFactory());
        UserFactory.setRegistry(UserRole.SELLER, new SellerFactory());
        UserFactory.setRegistry(UserRole.ADMIN, new AdminFactory());

        authService = new AuthService();
        userDAO = mock(UserDAO.class);

        injectUserDAO(authService, userDAO);

        // 🔥 ĐỒNG BỘ MÔI TRƯỜNG: Dựng Connection giả lập thuần Java bypass bộ lọc JDK 25
        fakeConnection = new FakeDbConnection();
        mockedDbConnection = mockStatic(DatabaseConnection.class);
        mockedDbConnection.when(DatabaseConnection::getConnection).thenReturn(fakeConnection);
    }

    @AfterEach
    void tearDown() {
        // 🔥 ĐÓNG MOCK STATIC: Trả lại sự trong sạch cho các luồng chạy phía sau
        if (mockedDbConnection != null) {
            mockedDbConnection.close();
        }
    }

    private void injectUserDAO(AuthService authService, UserDAO mockUserDAO) throws Exception {
        Field field = AuthService.class.getDeclaredField("userDAO");
        field.setAccessible(true);
        field.set(authService, mockUserDAO);
    }

    private void assertAuthError(AuthenticationException exception, AuthErrorCode expectedError) {
        assertEquals(expectedError.getCode(), exception.getErrorCode());
    }

    // =========================================================
    // TEST REGISTER - VALIDATION USERNAME
    // =========================================================

    @Test
    void registerShouldThrowWhenUsernameIsNull() {
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.register(
                null, "Password@123", "user@example.com", UserRole.BIDDER
        ));
        assertAuthError(exception, AuthErrorCode.USERNAME_NULL_EMPTY);
    }

    @Test
    void registerShouldThrowWhenUsernameIsEmpty() {
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.register(
                "", "Password@123", "user@example.com", UserRole.BIDDER
        ));
        assertAuthError(exception, AuthErrorCode.USERNAME_NULL_EMPTY);
    }

    @Test
    void registerShouldThrowWhenUsernameIsTooShort() {
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.register(
                "abc", "Password@123", "user@example.com", UserRole.BIDDER
        ));
        assertAuthError(exception, AuthErrorCode.USERNAME_TOO_SHORT);
    }

    @Test
    void registerShouldThrowWhenUsernameIsTooLong() {
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.register(
                "this_username_is_too_long_123", "Password@123", "user@example.com", UserRole.BIDDER
        ));
        assertAuthError(exception, AuthErrorCode.USERNAME_TOO_LONG);
    }

    @Test
    void registerShouldThrowWhenUsernameHasInvalidFormat() {
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.register(
                "bad user", "Password@123", "user@example.com", UserRole.BIDDER
        ));
        assertAuthError(exception, AuthErrorCode.USERNAME_INVALID_FORMAT);
    }

    // =========================================================
    // TEST REGISTER - VALIDATION EMAIL
    // =========================================================

    @Test
    void registerShouldThrowWhenEmailIsNull() {
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.register(
                "user01", "Password@123", null, UserRole.BIDDER
        ));
        assertAuthError(exception, AuthErrorCode.EMAIL_NULL_EMPTY);
    }

    @Test
    void registerShouldThrowWhenEmailIsEmpty() {
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.register(
                "user01", "Password@123", "", UserRole.BIDDER
        ));
        assertAuthError(exception, AuthErrorCode.EMAIL_NULL_EMPTY);
    }

    @Test
    void registerShouldThrowWhenEmailFormatIsInvalid() {
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.register(
                "user01", "Password@123", "not-an-email", UserRole.BIDDER
        ));
        assertAuthError(exception, AuthErrorCode.EMAIL_INVALID_FORMAT);
    }

    // =========================================================
    // TEST REGISTER - VALIDATION PASSWORD
    // =========================================================

    @Test
    void registerShouldThrowWhenPasswordIsNull() {
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.register(
                "user01", null, "user@example.com", UserRole.BIDDER
        ));
        assertAuthError(exception, AuthErrorCode.PASSWORD_NULL_EMPTY);
    }

    @Test
    void registerShouldThrowWhenPasswordIsEmpty() {
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.register(
                "user01", "", "user@example.com", UserRole.BIDDER
        ));
        assertAuthError(exception, AuthErrorCode.PASSWORD_NULL_EMPTY);
    }

    @Test
    void registerShouldThrowWhenPasswordIsTooShort() {
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.register(
                "user01", "Aa@1", "user@example.com", UserRole.BIDDER
        ));
        assertAuthError(exception, AuthErrorCode.PASSWORD_TOO_SHORT);
    }

    @Test
    void registerShouldThrowWhenPasswordIsWeak() {
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.register(
                "user01", "password", "user@example.com", UserRole.BIDDER
        ));
        assertAuthError(exception, AuthErrorCode.PASSWORD_WEAK);
    }

    // =========================================================
    // TEST REGISTER - ROLE, DUPLICATE, DB SAVE
    // =========================================================

    @Test
    void registerShouldThrowWhenRoleIsNull() {
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.register(
                "user01", "Password@123", "user@example.com", null
        ));
        assertAuthError(exception, AuthErrorCode.ROLE_INVALID);
    }

    @Test
    void registerShouldThrowWhenUsernameAlreadyExists() {
        Bidder existingUser = new Bidder("user01", "old@example.com", "hashed-password");
        when(userDAO.findByUsername("user01")).thenReturn(Optional.of(existingUser));

        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.register(
                "user01", "Password@123", "user@example.com", UserRole.BIDDER
        ));
        assertAuthError(exception, AuthErrorCode.USERNAME_ALREADY_EXISTS);
    }

    @Test
    void registerShouldThrowWhenEmailAlreadyExists() {
        Bidder existingUser = new Bidder("otherUser", "user@example.com", "hashed-password");
        when(userDAO.findByUsername("user01")).thenReturn(Optional.empty());
        when(userDAO.findByEmail("user@example.com")).thenReturn(Optional.of(existingUser));

        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.register(
                "user01", "Password@123", "user@example.com", UserRole.BIDDER
        ));
        assertAuthError(exception, AuthErrorCode.EMAIL_ALREADY_EXISTS);
    }

    @Test
    void registerShouldThrowWhenInsertUserFails() throws Exception {
        when(userDAO.findByUsername("user01")).thenReturn(Optional.empty());
        when(userDAO.findByEmail("user@example.com")).thenReturn(Optional.empty());

        // MOCK ĐÃ PHÙ HỢP: Trả về false khi nhận kết nối giả lập ngắn hạn
        when(userDAO.insertUser(any(Connection.class), any(User.class))).thenReturn(false);

        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.register(
                "user01", "Password@123", "user@example.com", UserRole.BIDDER
        ));

        assertAuthError(exception, AuthErrorCode.REGISTRATION_FAILED);
    }

    // 🌟 TEST CASE THÊM MỚI: Kiểm tra bảo mật chặn đăng ký quyền Admin
    @Test
    void registerShouldThrowWhenRoleIsAdmin() {
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.register(
                "admin01", "Password@123", "admin@example.com", UserRole.ADMIN
        ));
        assertAuthError(exception, AuthErrorCode.ROLE_INVALID);
    }

    @Test
    void registerShouldReturnBidderDTOWhenRegisterBidderSuccessfully() throws Exception {
        when(userDAO.findByUsername("user01")).thenReturn(Optional.empty());
        when(userDAO.findByEmail("user@example.com")).thenReturn(Optional.empty());

        // MOCK ĐÃ PHÙ HỢP: Chấp thuận lưu tài khoản
        when(userDAO.insertUser(any(Connection.class), any(User.class))).thenReturn(true);

        UserDTO result = authService.register(
                "user01", "Password@123", "user@example.com", UserRole.BIDDER
        );

        assertNotNull(result);
        assertInstanceOf(BidderDTO.class, result);
        assertEquals("user01", result.getUsername());
        assertEquals("user@example.com", result.getEmail());
        assertEquals(UserRole.BIDDER, result.getRole());
        assertEquals(UserStatus.ACTIVE, result.getStatus());

        verify(userDAO).insertUser(any(Connection.class), any(User.class));
    }

    @Test
    void registerShouldReturnSellerDTOWhenRegisterSellerSuccessfully() throws Exception {
        when(userDAO.findByUsername("seller01")).thenReturn(Optional.empty());
        when(userDAO.findByEmail("seller@example.com")).thenReturn(Optional.empty());

        // MOCK ĐÃ PHÙ HỢP: Chấp thuận lưu tài khoản
        when(userDAO.insertUser(any(Connection.class), any(User.class))).thenReturn(true);

        UserDTO result = authService.register(
                "seller01", "Password@123", "seller@example.com", UserRole.SELLER
        );

        assertNotNull(result);
        assertInstanceOf(SellerDTO.class, result);
        assertEquals("seller01", result.getUsername());
        assertEquals("seller@example.com", result.getEmail());
        assertEquals(UserRole.SELLER, result.getRole());

        verify(userDAO).insertUser(any(Connection.class), any(User.class));
    }

    @Test
    void registerShouldNormalizeEmailToLowercase() throws Exception {
        when(userDAO.findByUsername("user02")).thenReturn(Optional.empty());
        when(userDAO.findByEmail("user02@example.com")).thenReturn(Optional.empty());
        when(userDAO.insertUser(any(Connection.class), any(User.class))).thenReturn(true);

        UserDTO result = authService.register(
                "user02", "Password@123", "User02@Example.Com", UserRole.BIDDER
        );

        assertNotNull(result);
        assertEquals("user02@example.com", result.getEmail());
    }

    // =========================================================
    // TEST LOGIN
    // =========================================================

    @Test
    void loginShouldThrowWhenUsernameOrEmailIsNull() {
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.login(null, "Password@123"));
        assertAuthError(exception, AuthErrorCode.INPUT_NULL_EMPTY);
    }

    @Test
    void loginShouldThrowWhenPasswordIsNull() {
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.login("user01", null));
        assertAuthError(exception, AuthErrorCode.INPUT_NULL_EMPTY);
    }

    @Test
    void loginShouldThrowWhenUsernameOrEmailIsEmpty() {
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.login("", "Password@123"));
        assertAuthError(exception, AuthErrorCode.INPUT_NULL_EMPTY);
    }

    @Test
    void loginShouldThrowWhenPasswordIsEmpty() {
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.login("user01", ""));
        assertAuthError(exception, AuthErrorCode.INPUT_NULL_EMPTY);
    }

    @Test
    void loginShouldThrowInvalidCredentialsWhenUsernameNotFound() {
        when(userDAO.findByUsername("notFoundUser")).thenReturn(Optional.empty());
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.login("notFoundUser", "Password@123"));
        assertAuthError(exception, AuthErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void loginShouldThrowInvalidCredentialsWhenEmailNotFound() {
        when(userDAO.findByEmail("notfound@example.com")).thenReturn(Optional.empty());
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.login("notfound@example.com", "Password@123"));
        assertAuthError(exception, AuthErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void loginShouldThrowInvalidCredentialsWhenPasswordIsWrong() {
        Bidder user = UserFactory.createUser(UserRole.BIDDER, "loginuser01", "loginuser01@example.com", "Correct@123");
        when(userDAO.findByUsername("loginuser01")).thenReturn(Optional.of(user));

        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.login("loginuser01", "Wrong@123"));
        assertAuthError(exception, AuthErrorCode.INVALID_CREDENTIALS);
    }

    // 🌟 TEST CASE THÊM MỚI: Chặn đăng nhập đối với tài khoản đã bị khóa (BANNED)
    @Test
    void loginShouldThrowWhenAccountIsBanned() {
        Bidder user = UserFactory.createUser(UserRole.BIDDER, "banneduser", "banned@example.com", "Correct@123");
        user.setStatus(UserStatus.BANNED); // Ép trạng thái khóa tài khoản
        when(userDAO.findByUsername("banneduser")).thenReturn(Optional.of(user));

        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.login("banneduser", "Correct@123"));
        assertEquals(AuthErrorCode.ACCOUNT_LOCKED.getCode(), exception.getErrorCode());
    }

    @Test
    void loginShouldReturnDTOWhenUsernameAndPasswordAreCorrect() {
        Bidder user = UserFactory.createUser(UserRole.BIDDER, "loginuser02", "loginuser02@example.com", "Correct@123");
        when(userDAO.findByUsername("loginuser02")).thenReturn(Optional.of(user));

        UserDTO result = authService.login("loginuser02", "Correct@123");

        assertNotNull(result);
        assertInstanceOf(BidderDTO.class, result);
        assertEquals("loginuser02", result.getUsername());
        assertEquals("loginuser02@example.com", result.getEmail());
        assertEquals(UserRole.BIDDER, result.getRole());
    }

    @Test
    void loginShouldReturnDTOWhenEmailAndPasswordAreCorrect() {
        Seller user = UserFactory.createUser(UserRole.SELLER, "sellerlogin01", "sellerlogin01@example.com", "Correct@123");
        when(userDAO.findByEmail("sellerlogin01@example.com")).thenReturn(Optional.of(user));

        UserDTO result = authService.login("sellerlogin01@example.com", "Correct@123");

        assertNotNull(result);
        assertInstanceOf(SellerDTO.class, result);
        assertEquals("sellerlogin01", result.getUsername());
        assertEquals("sellerlogin01@example.com", result.getEmail());
        assertEquals(UserRole.SELLER, result.getRole());
    }

    @Test
    void loginShouldBeCaseSensitiveForUsername() {
        Bidder user = UserFactory.createUser(UserRole.BIDDER, "LinhS", "linhs@example.com", "Correct@123");
        when(userDAO.findByUsername("LinhS")).thenReturn(Optional.of(user));

        // Login với username "Linhs" (sai case) -> Phải ném ngoại lệ INVALID_CREDENTIALS
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> 
            authService.login("Linhs", "Correct@123")
        );
        assertAuthError(exception, AuthErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void loginShouldBeCaseInsensitiveForEmail() {
        Seller user = UserFactory.createUser(UserRole.SELLER, "sellerlogin02", "seller02@example.com", "Correct@123");
        when(userDAO.findByEmail("seller02@example.com")).thenReturn(Optional.of(user));

        // Login với email "Seller02@Example.Com" (khác case) -> Đăng nhập thành công
        UserDTO result = authService.login("Seller02@Example.Com", "Correct@123");
        assertNotNull(result);
        assertEquals("sellerlogin02", result.getUsername());
        assertEquals("seller02@example.com", result.getEmail());
    }

    // =========================================================
    // TEST LOGOUT
    // =========================================================

    @Test
    void logoutShouldThrowWhenUserIdIsNull() {
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.logout(null));
        assertAuthError(exception, AuthErrorCode.USER_NOT_FOUND);
    }

    @Test
    void logoutShouldThrowWhenUserIdIsBlank() {
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.logout("   "));
        assertAuthError(exception, AuthErrorCode.USER_NOT_FOUND);
    }

    @Test
    void logoutShouldThrowWhenUserDoesNotExist() {
        when(userDAO.findById("missing-user-id")).thenReturn(Optional.empty());
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authService.logout("missing-user-id"));
        assertAuthError(exception, AuthErrorCode.USER_NOT_FOUND);
    }

    @Test
    void logoutShouldNotThrowWhenUserExistsInDatabase() {
        Bidder user = new Bidder("logoutUser", "logout@example.com", "hashed-password");
        when(userDAO.findById("existing-user-id")).thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> authService.logout("existing-user-id"));
    }

    // =========================================================
    // TEST convertUserToDTO
    // =========================================================

    @Test
    void convertUserToDTOShouldReturnNullWhenUserIsNull() {
        UserDTO result = authService.convertUserToDTO(null);
        assertNull(result);
    }

    @Test
    void convertUserToDTOShouldConvertBidderToBidderDTO() {
        Bidder bidder = new Bidder("bidderdto01", "bidderdto01@example.com", "hashed-password");
        bidder.addAvailableBalance(500.0);
        bidder.freeze(100.0);
        bidder.addJoinedAuction("auction-1");
        bidder.addJoinedAuction("auction-2");

        UserDTO result = authService.convertUserToDTO(bidder);

        assertNotNull(result);
        assertInstanceOf(BidderDTO.class, result);

        BidderDTO dto = (BidderDTO) result;
        assertEquals("bidderdto01", dto.getUsername());
        assertEquals("bidderdto01@example.com", dto.getEmail());
        assertEquals(UserRole.BIDDER, dto.getRole());
        assertEquals(UserStatus.ACTIVE, dto.getStatus());
        assertEquals(400.0, dto.getAvailableBalance());
        assertEquals(100.0, dto.getFrozenBalance());
        assertEquals(List.of("auction-1", "auction-2"), dto.getJoinedAuctionIds());
    }

    @Test
    void convertUserToDTOShouldConvertSellerToSellerDTO() {
        Seller seller = new Seller("sellerdto01", "sellerdto01@example.com", "hashed-password");
        seller.addAvailableBalance(1000.0);
        seller.freeze(200.0);

        UserDTO result = authService.convertUserToDTO(seller);

        assertNotNull(result);
        assertInstanceOf(SellerDTO.class, result);

        SellerDTO dto = (SellerDTO) result;
        assertEquals("sellerdto01", dto.getUsername());
        assertEquals("sellerdto01@example.com", dto.getEmail());
        assertEquals(UserRole.SELLER, dto.getRole());
        assertEquals(UserStatus.ACTIVE, dto.getStatus());
        assertEquals(800.0, dto.getAvailableBalance());
        assertEquals(200.0, dto.getFrozenBalance());
        assertEquals(0.0, dto.getRating());
    }

    @Test
    void convertUserToDTOShouldConvertAdminToAdminDTO() {
        Admin admin = new Admin("admindto01", "admindto01@example.com", "hashed-password");

        UserDTO result = authService.convertUserToDTO(admin);

        assertNotNull(result);
        assertInstanceOf(AdminDTO.class, result);

        AdminDTO dto = (AdminDTO) result;
        assertEquals("admindto01", dto.getUsername());
        assertEquals("admindto01@example.com", dto.getEmail());
        assertEquals(UserRole.ADMIN, dto.getRole());
        assertEquals(UserStatus.ACTIVE, dto.getStatus());
        assertEquals(0.0, dto.getAvailableBalance());
        assertEquals(0.0, dto.getFrozenBalance());
    }

    // ANONYMOUS CLASS BYPASS COMPILER LOCK CỦA JAVA 25 ĐỘC LẬP
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