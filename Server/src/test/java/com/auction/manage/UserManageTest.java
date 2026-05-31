package com.auction.manage;

import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;
import com.auction.exception.AuthErrorCode;
import com.auction.exception.AuthenticationException;
import com.auction.models.User.Admin;
import com.auction.models.User.Bidder;
import com.auction.models.User.Seller;
import com.auction.models.User.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UserManageTest {

    private UserManage userManage;

    @BeforeEach
    void setUp() throws Exception {
        userManage = UserManage.getInstance();
        clearUserManage();
    }

    // Clear singleton RAM trước mỗi test
    private void clearUserManage() throws Exception {
        clearMapField("users");
        clearMapField("usernameToIdMap");
        clearMapField("emailToIdMap");
    }

    // Clear một Map private bằng reflection
    private void clearMapField(String fieldName) throws Exception {
        Field field = UserManage.class.getDeclaredField(fieldName);
        field.setAccessible(true);

        Map<?, ?> map = (Map<?, ?>) field.get(userManage);
        map.clear();
    }

    // Lấy map users để kiểm tra dữ liệu RAM bên trong
    @SuppressWarnings("unchecked")
    private Map<String, User> getUsersMap() throws Exception {
        Field field = UserManage.class.getDeclaredField("users");
        field.setAccessible(true);

        return (Map<String, User>) field.get(userManage);
    }

    // Lấy map username -> userId
    @SuppressWarnings("unchecked")
    private Map<String, String> getUsernameToIdMap() throws Exception {
        Field field = UserManage.class.getDeclaredField("usernameToIdMap");
        field.setAccessible(true);

        return (Map<String, String>) field.get(userManage);
    }

    // Lấy map email -> userId
    @SuppressWarnings("unchecked")
    private Map<String, String> getEmailToIdMap() throws Exception {
        Field field = UserManage.class.getDeclaredField("emailToIdMap");
        field.setAccessible(true);

        return (Map<String, String>) field.get(userManage);
    }

    // Check đúng mã lỗi AuthenticationException
    private void assertAuthError(AuthenticationException exception, AuthErrorCode expectedError) {
        assertEquals(expectedError.getCode(), exception.getErrorCode());
    }

    // Tạo Bidder mẫu có id cố định
    private Bidder sampleBidder(String id, String username, String email) {
        return new Bidder(
                id,
                username,
                email,
                "hashed-password",
                UserRole.BIDDER,
                0.0,
                0.0,
                UserStatus.ACTIVE,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now()
        );
    }

    // Tạo Seller mẫu có id cố định
    private Seller sampleSeller(String id, String username, String email) {
        return new Seller(
                id,
                username,
                email,
                "hashed-password",
                UserRole.SELLER,
                0.0,
                0.0,
                UserStatus.ACTIVE,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now(),
                0.0
        );
    }

    // Tạo Admin mẫu có id cố định
    private Admin sampleAdmin(String id, String username, String email) {
        return new Admin(
                id,
                username,
                email,
                "hashed-password",
                UserRole.ADMIN,
                0.0,
                0.0,
                UserStatus.ACTIVE,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now()
        );
    }

    // =========================================================
    // getInstance()
    // =========================================================

    // getInstance nhiều lần phải trả cùng một singleton
    @Test
    void getInstanceShouldReturnSameObject() {
        UserManage first = UserManage.getInstance();
        UserManage second = UserManage.getInstance();

        assertSame(first, second);
    }

    // =========================================================
    // addUser()
    // =========================================================

    // addUser null hiện tại ném USER_NOT_FOUND
    @Test
    void addUserShouldThrowWhenUserIsNull() {
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            userManage.addUser(null);
        });

        assertAuthError(exception, AuthErrorCode.USER_NOT_FOUND);
        assertEquals(0, userManage.getUserCount());
    }

    // addUser hợp lệ phải lưu user vào cả 3 map
    @Test
    void addUserShouldStoreUserInAllMapsWhenValid() throws Exception {
        Bidder bidder = sampleBidder("u1", "bidder01", "bidder01@example.com");

        boolean result = userManage.addUser(bidder);

        assertTrue(result);
        assertEquals(1, userManage.getUserCount());

        assertSame(bidder, userManage.getUser("u1"));
        assertSame(bidder, userManage.getUserById("u1"));
        assertSame(bidder, userManage.getUserByUsername("bidder01"));
        assertSame(bidder, userManage.getUserByEmail("bidder01@example.com"));

        assertEquals("u1", getUsernameToIdMap().get("bidder01"));
        assertEquals("u1", getEmailToIdMap().get("bidder01@example.com"));
    }

    // addUser username trùng phải ném USERNAME_ALREADY_EXISTS
    @Test
    void addUserShouldThrowWhenUsernameAlreadyExists() throws AuthenticationException {
        Bidder bidder1 = sampleBidder("u1", "sameName", "user1@example.com");
        Bidder bidder2 = sampleBidder("u2", "sameName", "user2@example.com");

        userManage.addUser(bidder1);

        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            userManage.addUser(bidder2);
        });

        assertAuthError(exception, AuthErrorCode.USERNAME_ALREADY_EXISTS);
        assertEquals(1, userManage.getUserCount());
    }

    // addUser email trùng phải ném EMAIL_ALREADY_EXISTS
    @Test
    void addUserShouldThrowWhenEmailAlreadyExists() throws AuthenticationException {
        Bidder bidder1 = sampleBidder("u1", "user1", "same@example.com");
        Bidder bidder2 = sampleBidder("u2", "user2", "same@example.com");

        userManage.addUser(bidder1);

        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            userManage.addUser(bidder2);
        });

        assertAuthError(exception, AuthErrorCode.EMAIL_ALREADY_EXISTS);
        assertEquals(1, userManage.getUserCount());
    }

    // addUser id null hiện tại có thể lỗi vì ConcurrentHashMap không nhận null key
    @Test
    void addUserShouldThrowWhenUserIdIsNull() {
        Bidder bidder = sampleBidder("u1", "bidder01", "bidder01@example.com");
        bidder.setId(null);

        assertThrows(NullPointerException.class, () -> {
            userManage.addUser(bidder);
        });
    }

    // addUser username null hiện tại ném USER_NOT_FOUND
    @Test
    void addUserShouldThrowWhenUsernameIsNull() {
        Bidder bidder = sampleBidder("u1", "bidder01", "bidder01@example.com");
        bidder.setUsername(null);

        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            userManage.addUser(bidder);
        });

        assertAuthError(exception, AuthErrorCode.USER_NOT_FOUND);
        assertEquals(0, userManage.getUserCount());
    }

    // addUser email null hiện tại ném USER_NOT_FOUND
    @Test
    void addUserShouldThrowWhenEmailIsNull() {
        Bidder bidder = sampleBidder("u1", "bidder01", "bidder01@example.com");
        bidder.setEmail(null);

        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            userManage.addUser(bidder);
        });

        assertAuthError(exception, AuthErrorCode.USER_NOT_FOUND);
        assertEquals(0, userManage.getUserCount());
    }

    // =========================================================
    // getUser / getUserBy...
    // =========================================================

    // getUser null trả null
    @Test
    void getUserShouldReturnNullWhenUserIdIsNull() {
        assertNull(userManage.getUser(null));
    }

    // getUserById null trả null
    @Test
    void getUserByIdShouldReturnNullWhenIdIsNull() {
        assertNull(userManage.getUserById(null));
    }

    // getUserByUsername null trả null
    @Test
    void getUserByUsernameShouldReturnNullWhenUsernameIsNull() {
        assertNull(userManage.getUserByUsername(null));
    }

    // getUserByEmail null trả null
    @Test
    void getUserByEmailShouldReturnNullWhenEmailIsNull() {
        assertNull(userManage.getUserByEmail(null));
    }

    // getUser id không tồn tại trả null
    @Test
    void getUserShouldReturnNullWhenUserDoesNotExist() {
        assertNull(userManage.getUser("missing-id"));
    }

    // getUserByUsername username không tồn tại trả null
    @Test
    void getUserByUsernameShouldReturnNullWhenUsernameDoesNotExist() {
        assertNull(userManage.getUserByUsername("missing-username"));
    }

    // getUserByEmail email không tồn tại trả null
    @Test
    void getUserByEmailShouldReturnNullWhenEmailDoesNotExist() {
        assertNull(userManage.getUserByEmail("missing@example.com"));
    }

    // =========================================================
    // updateUser()
    // =========================================================

    // updateUser userId null trả false
    @Test
    void updateUserShouldReturnFalseWhenUserIdIsNull() throws AuthenticationException {
        Bidder updatedUser = sampleBidder("u1", "newName", "new@example.com");

        boolean result = userManage.updateUser(null, updatedUser);

        assertFalse(result);
    }

    // updateUser updatedUser null trả false
    @Test
    void updateUserShouldReturnFalseWhenUpdatedUserIsNull() throws AuthenticationException {
        boolean result = userManage.updateUser("u1", null);

        assertFalse(result);
    }

    // updateUser userId không tồn tại phải ném USER_NOT_FOUND
    @Test
    void updateUserShouldThrowWhenUserIdDoesNotExist() {
        Bidder updatedUser = sampleBidder("u1", "newName", "new@example.com");

        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            userManage.updateUser("missing-id", updatedUser);
        });

        assertAuthError(exception, AuthErrorCode.USER_NOT_FOUND);
    }

    // updateUser đổi username/email/password thành công và cập nhật map phụ
    @Test
    void updateUserShouldUpdateUserAndLookupMapsWhenValid() throws Exception {
        Bidder oldUser = sampleBidder("u1", "oldName", "old@example.com");
        Bidder updatedUser = sampleBidder("u1", "newName", "new@example.com");

        userManage.addUser(oldUser);

        boolean result = userManage.updateUser("u1", updatedUser);

        assertTrue(result);

        User savedUser = userManage.getUser("u1");

        assertSame(oldUser, savedUser);
        assertEquals("newName", savedUser.getUsername());
        assertEquals("new@example.com", savedUser.getEmail());
        assertEquals(updatedUser.getPassword(), savedUser.getPassword());

        assertNull(userManage.getUserByUsername("oldName"));
        assertNull(userManage.getUserByEmail("old@example.com"));

        assertSame(oldUser, userManage.getUserByUsername("newName"));
        assertSame(oldUser, userManage.getUserByEmail("new@example.com"));

        assertFalse(getUsernameToIdMap().containsKey("oldName"));
        assertFalse(getEmailToIdMap().containsKey("old@example.com"));
        assertEquals("u1", getUsernameToIdMap().get("newName"));
        assertEquals("u1", getEmailToIdMap().get("new@example.com"));
    }

    // updateUser không đổi username/email vẫn phải thành công
    @Test
    void updateUserShouldAllowSameUsernameAndEmail() throws AuthenticationException {
        Bidder oldUser = sampleBidder("u1", "sameName", "same@example.com");
        Bidder updatedUser = sampleBidder("u1", "sameName", "same@example.com");

        userManage.addUser(oldUser);

        boolean result = userManage.updateUser("u1", updatedUser);

        assertTrue(result);
        assertEquals("sameName", userManage.getUser("u1").getUsername());
        assertEquals("same@example.com", userManage.getUser("u1").getEmail());
    }

    // updateUser đổi sang username của user khác phải bị chặn
    @Test
    void updateUserShouldThrowWhenNewUsernameAlreadyExists() throws AuthenticationException {
        Bidder user1 = sampleBidder("u1", "user1", "user1@example.com");
        Bidder user2 = sampleBidder("u2", "user2", "user2@example.com");
        Bidder updatedUser = sampleBidder("u1", "user2", "new@example.com");

        userManage.addUser(user1);
        userManage.addUser(user2);

        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            userManage.updateUser("u1", updatedUser);
        });

        assertAuthError(exception, AuthErrorCode.USERNAME_ALREADY_EXISTS);
        assertSame(user1, userManage.getUserByUsername("user1"));
        assertSame(user2, userManage.getUserByUsername("user2"));
    }

    // updateUser đổi sang email của user khác phải bị chặn
    @Test
    void updateUserShouldThrowWhenNewEmailAlreadyExists() throws AuthenticationException {
        Bidder user1 = sampleBidder("u1", "user1", "user1@example.com");
        Bidder user2 = sampleBidder("u2", "user2", "user2@example.com");
        Bidder updatedUser = sampleBidder("u1", "newName", "user2@example.com");

        userManage.addUser(user1);
        userManage.addUser(user2);

        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            userManage.updateUser("u1", updatedUser);
        });

        assertAuthError(exception, AuthErrorCode.EMAIL_ALREADY_EXISTS);
        assertSame(user1, userManage.getUserByEmail("user1@example.com"));
        assertSame(user2, userManage.getUserByEmail("user2@example.com"));
    }

    // updateUser username null hiện tại có thể gây NullPointerException
    @Test
    void updateUserShouldThrowWhenNewUsernameIsNull() throws AuthenticationException {
        Bidder oldUser = sampleBidder("u1", "oldName", "old@example.com");
        Bidder updatedUser = sampleBidder("u1", "newName", "new@example.com");
        updatedUser.setUsername(null);

        userManage.addUser(oldUser);

        assertThrows(NullPointerException.class, () -> {
            userManage.updateUser("u1", updatedUser);
        });
    }

    // updateUser email null hiện tại có thể gây NullPointerException
    @Test
    void updateUserShouldThrowWhenNewEmailIsNull() throws AuthenticationException {
        Bidder oldUser = sampleBidder("u1", "oldName", "old@example.com");
        Bidder updatedUser = sampleBidder("u1", "newName", "new@example.com");
        updatedUser.setEmail(null);

        userManage.addUser(oldUser);

        assertThrows(NullPointerException.class, () -> {
            userManage.updateUser("u1", updatedUser);
        });
    }

    // =========================================================
    // deleteUser()
    // =========================================================

    // deleteUser null trả false
    @Test
    void deleteUserShouldReturnFalseWhenUserIdIsNull() throws AuthenticationException {
        boolean result = userManage.deleteUser(null);

        assertFalse(result);
    }

    // deleteUser userId không tồn tại phải ném USER_NOT_FOUND
    @Test
    void deleteUserShouldThrowWhenUserIdDoesNotExist() {
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            userManage.deleteUser("missing-id");
        });

        assertAuthError(exception, AuthErrorCode.USER_NOT_FOUND);
    }

    // deleteUser phải xóa user khỏi cả 3 map
    @Test
    void deleteUserShouldRemoveUserFromAllMaps() throws Exception {
        Bidder bidder = sampleBidder("u1", "bidder01", "bidder01@example.com");

        userManage.addUser(bidder);

        boolean result = userManage.deleteUser("u1");

        assertTrue(result);
        assertEquals(0, userManage.getUserCount());

        assertNull(userManage.getUser("u1"));
        assertNull(userManage.getUserByUsername("bidder01"));
        assertNull(userManage.getUserByEmail("bidder01@example.com"));

        assertFalse(getUsersMap().containsKey("u1"));
        assertFalse(getUsernameToIdMap().containsKey("bidder01"));
        assertFalse(getEmailToIdMap().containsKey("bidder01@example.com"));
    }

    // deleteUser chỉ xóa đúng user cần xóa
    @Test
    void deleteUserShouldOnlyRemoveTargetUser() throws AuthenticationException {
        Bidder user1 = sampleBidder("u1", "user1", "user1@example.com");
        Bidder user2 = sampleBidder("u2", "user2", "user2@example.com");

        userManage.addUser(user1);
        userManage.addUser(user2);

        userManage.deleteUser("u1");

        assertNull(userManage.getUser("u1"));
        assertSame(user2, userManage.getUser("u2"));
        assertEquals(1, userManage.getUserCount());
    }

    // =========================================================
    // getAllUsers()
    // =========================================================

    // getAllUsers trả list rỗng khi RAM không có user
    @Test
    void getAllUsersShouldReturnEmptyListWhenNoUserExists() {
        List<User> result = userManage.getAllUsers();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // getAllUsers trả tất cả user trong RAM
    @Test
    void getAllUsersShouldReturnAllUsersInMemory() throws AuthenticationException {
        Bidder bidder = sampleBidder("u1", "bidder01", "bidder01@example.com");
        Seller seller = sampleSeller("u2", "seller01", "seller01@example.com");

        userManage.addUser(bidder);
        userManage.addUser(seller);

        List<User> result = userManage.getAllUsers();

        assertEquals(2, result.size());
        assertTrue(result.contains(bidder));
        assertTrue(result.contains(seller));
    }

    // getAllUsers trả bản copy, sửa list result không làm mất dữ liệu RAM
    @Test
    void getAllUsersShouldReturnCopyList() throws AuthenticationException {
        Bidder bidder = sampleBidder("u1", "bidder01", "bidder01@example.com");

        userManage.addUser(bidder);

        List<User> result = userManage.getAllUsers();
        result.clear();

        assertEquals(1, userManage.getUserCount());
        assertSame(bidder, userManage.getUser("u1"));
    }

    // =========================================================
    // getUsersByRole()
    // =========================================================

    // getUsersByRole null trả list rỗng
    @Test
    void getUsersByRoleShouldReturnEmptyListWhenRoleIsNull() {
        List<User> result = userManage.getUsersByRole(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // getUsersByRole BIDDER chỉ trả bidder
    @Test
    void getUsersByRoleShouldReturnOnlyBidders() throws AuthenticationException {
        Bidder bidder = sampleBidder("u1", "bidder01", "bidder01@example.com");
        Seller seller = sampleSeller("u2", "seller01", "seller01@example.com");
        Admin admin = sampleAdmin("u3", "admin01", "admin01@example.com");

        userManage.addUser(bidder);
        userManage.addUser(seller);
        userManage.addUser(admin);

        List<User> result = userManage.getUsersByRole(UserRole.BIDDER);

        assertEquals(1, result.size());
        assertSame(bidder, result.get(0));
    }

    // getUsersByRole SELLER chỉ trả seller
    @Test
    void getUsersByRoleShouldReturnOnlySellers() throws AuthenticationException {
        Bidder bidder = sampleBidder("u1", "bidder01", "bidder01@example.com");
        Seller seller = sampleSeller("u2", "seller01", "seller01@example.com");

        userManage.addUser(bidder);
        userManage.addUser(seller);

        List<User> result = userManage.getUsersByRole(UserRole.SELLER);

        assertEquals(1, result.size());
        assertSame(seller, result.get(0));
    }

    // getUsersByRole ADMIN chỉ trả admin
    @Test
    void getUsersByRoleShouldReturnOnlyAdmins() throws AuthenticationException {
        Bidder bidder = sampleBidder("u1", "bidder01", "bidder01@example.com");
        Admin admin = sampleAdmin("u3", "admin01", "admin01@example.com");

        userManage.addUser(bidder);
        userManage.addUser(admin);

        List<User> result = userManage.getUsersByRole(UserRole.ADMIN);

        assertEquals(1, result.size());
        assertSame(admin, result.get(0));
    }

    // getUsersByRole role không có user nào thì trả list rỗng
    @Test
    void getUsersByRoleShouldReturnEmptyListWhenNoUserHasRole() throws AuthenticationException {
        Bidder bidder = sampleBidder("u1", "bidder01", "bidder01@example.com");

        userManage.addUser(bidder);

        List<User> result = userManage.getUsersByRole(UserRole.ADMIN);

        assertTrue(result.isEmpty());
    }

    // =========================================================
    // getUserCount()
    // =========================================================

    // getUserCount ban đầu bằng 0
    @Test
    void getUserCountShouldReturnZeroWhenNoUserExists() {
        assertEquals(0, userManage.getUserCount());
    }

    // getUserCount tăng sau khi add user
    @Test
    void getUserCountShouldIncreaseAfterAddUser() throws AuthenticationException {
        userManage.addUser(sampleBidder("u1", "bidder01", "bidder01@example.com"));
        userManage.addUser(sampleSeller("u2", "seller01", "seller01@example.com"));

        assertEquals(2, userManage.getUserCount());
    }

    // getUserCount giảm sau khi delete user
    @Test
    void getUserCountShouldDecreaseAfterDeleteUser() throws AuthenticationException {
        userManage.addUser(sampleBidder("u1", "bidder01", "bidder01@example.com"));
        userManage.addUser(sampleSeller("u2", "seller01", "seller01@example.com"));

        userManage.deleteUser("u1");

        assertEquals(1, userManage.getUserCount());
    }

    // =========================================================
    // INTERNAL CONSISTENCY
    // =========================================================

    // Sau add/update/delete, 3 map phải luôn đồng bộ
    @Test
    void mapsShouldStayConsistentAfterAddUpdateAndDelete() throws Exception {
        Bidder user = sampleBidder("u1", "oldName", "old@example.com");
        Bidder updatedUser = sampleBidder("u1", "newName", "new@example.com");

        userManage.addUser(user);
        userManage.updateUser("u1", updatedUser);

        assertSame(user, userManage.getUserByUsername("newName"));
        assertSame(user, userManage.getUserByEmail("new@example.com"));
        assertNull(userManage.getUserByUsername("oldName"));
        assertNull(userManage.getUserByEmail("old@example.com"));

        userManage.deleteUser("u1");

        assertTrue(getUsersMap().isEmpty());
        assertTrue(getUsernameToIdMap().isEmpty());
        assertTrue(getEmailToIdMap().isEmpty());
    }
}