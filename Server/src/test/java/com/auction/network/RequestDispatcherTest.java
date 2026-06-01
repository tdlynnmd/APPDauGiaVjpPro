package com.auction.network;

import com.auction.controller.AuthController;
import com.auction.dto.LoginRequest;
import com.auction.dto.LoginResultDTO;
import com.auction.dto.RegisterRequest;
import com.auction.dto.SocketRequest;
import com.auction.dto.UserDTO;
import com.auction.enums.ActionType;
import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;
import com.auction.exception.AuthErrorCode;
import com.auction.exception.AuthenticationException;
import com.auction.manage.ConnectionManage;
import com.auction.utils.GsonProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class RequestDispatcherTest {

    private RequestDispatcher dispatcher;
    private FakeClientSession session;
    private FakeAuthController authController;
    private com.google.gson.Gson gson;

    @BeforeEach
    void setUp() throws Exception {
        dispatcher = RequestDispatcher.getInstance();
        gson = GsonProvider.getGson();

        session = new FakeClientSession();
        authController = new FakeAuthController();

        clearConnectionManage();

        // Inject fake AuthController để LOGIN/REGISTER/LOGOUT không gọi DB thật
        injectField(dispatcher, "authController", authController);
    }

    @AfterEach
    void tearDown() {
        if (session != null) {
            session.getSessionExecutor().shutdownNow();
        }
    }

    // Inject fake controller vào field private
    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // Clear ConnectionManage singleton trước mỗi test
    @SuppressWarnings("unchecked")
    private void clearConnectionManage() throws Exception {
        Field field = ConnectionManage.class.getDeclaredField("activeConnections");
        field.setAccessible(true);

        ConcurrentHashMap<String, Set<ClientSession>> map =
                (ConcurrentHashMap<String, Set<ClientSession>>) field.get(ConnectionManage.getInstance());

        map.clear();
    }

    // Vì RequestDispatcher xử lý async, test phải đợi message được gửi về
    private void waitUntilMessageCount(int expectedCount) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 1500;

        while (System.currentTimeMillis() < deadline) {
            if (session.sentMessages.size() >= expectedCount) {
                return;
            }
            Thread.sleep(20);
        }

        fail("Timeout waiting for " + expectedCount + " message(s). Current messages: " + session.sentMessages);
    }

    // Tạo request với body là object Java
    private String toJsonRequest(ActionType action, Object body) {
        String bodyJson = body == null ? null : gson.toJson(body);
        SocketRequest request = new SocketRequest(action, bodyJson);
        return gson.toJson(request);
    }

    // Tạo request với body đã là JSON string, tránh dùng Map.of
    private String toJsonRequestWithRawBody(ActionType action, String rawBodyJson) {
        SocketRequest request = new SocketRequest(action, rawBodyJson);
        return gson.toJson(request);
    }

    // Fake session để bắt message thay vì gửi qua socket thật
    private static class FakeClientSession extends ClientSession {
        List<String> sentMessages = new ArrayList<>();

        FakeClientSession() {
            super((Socket) null, new PrintWriter(System.out));
        }

        @Override
        public boolean sendMessage(String jsonMessage) {
            sentMessages.add(jsonMessage);
            return true;
        }

        @Override
        public void close() {
            getSessionExecutor().shutdownNow();
        }
    }

    // Fake AuthController để không gọi AuthService thật/database thật
    private static class FakeAuthController extends AuthController {
        String lastLoginUsernameOrEmail;
        String lastLoginPassword;

        String lastRegisterUsername;
        String lastRegisterPassword;
        String lastRegisterEmail;
        UserRole lastRegisterRole;

        String lastLogoutUserId;

        boolean loginShouldThrow = false;
        boolean registerShouldThrow = false;
        boolean logoutShouldThrow = false;

        UserDTO userToReturn = new UserDTO(
                "user-1",
                "bidder01",
                "bidder01@example.com",
                UserRole.BIDDER,
                UserStatus.ACTIVE,
                1000.0,
                0.0
        );

        @Override
        public LoginResultDTO login(LoginRequest request) throws AuthenticationException {
            lastLoginUsernameOrEmail = request == null ? null : request.getUsernameOrEmail();
            lastLoginPassword = request == null ? null : request.getPassword();

            if (loginShouldThrow) {
                throw new AuthenticationException(AuthErrorCode.INVALID_CREDENTIALS);
            }

            return new LoginResultDTO("fake-token", userToReturn);
        }

        @Override
        public UserDTO register(RegisterRequest request) throws AuthenticationException {
            lastRegisterUsername = request == null ? null : request.getUsername();
            lastRegisterPassword = request == null ? null : request.getPassword();
            lastRegisterEmail = request == null ? null : request.getEmail();
            lastRegisterRole = request == null ? null : request.getRole();

            if (registerShouldThrow) {
                throw new AuthenticationException(AuthErrorCode.USERNAME_ALREADY_EXISTS);
            }

            return userToReturn;
        }

        @Override
        public void logout(String userId) throws AuthenticationException {
            lastLogoutUserId = userId;

            if (logoutShouldThrow) {
                throw new AuthenticationException(AuthErrorCode.USER_NOT_FOUND);
            }
        }
    }

    // =========================================================
    // getInstance()
    // =========================================================

    // getInstance nhiều lần phải trả cùng một singleton
    @Test
    void getInstanceShouldReturnSameObject() {
        RequestDispatcher first = RequestDispatcher.getInstance();
        RequestDispatcher second = RequestDispatcher.getInstance();

        assertSame(first, second);
    }

    // =========================================================
    // PING
    // =========================================================

    // PING là public action, chưa login vẫn được trả PONG
    @Test
    void processRequestShouldReturnPongForPing() throws Exception {
        String requestJson = toJsonRequest(ActionType.PING, null);

        dispatcher.processRequest(requestJson, session);

        waitUntilMessageCount(1);

        String response = session.sentMessages.get(0);

        assertTrue(response.contains("\"success\":true"));
        assertTrue(response.contains("\"action\":\"PING\""));
        assertTrue(response.contains("PONG"));
    }

    // PING không bị rate limit dù session đang bị giới hạn
    @Test
    void processRequestShouldNotRateLimitPing() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertFalse(session.isRateLimited());
        }

        assertTrue(session.isRateLimited());

        String requestJson = toJsonRequest(ActionType.PING, null);

        dispatcher.processRequest(requestJson, session);

        waitUntilMessageCount(1);

        String response = session.sentMessages.get(0);

        assertTrue(response.contains("\"success\":true"));
        assertTrue(response.contains("\"action\":\"PING\""));
        assertTrue(response.contains("PONG"));
    }

    // =========================================================
    // invalid request / invalid JSON
    // =========================================================

    // JSON sai cú pháp phải trả INVALID_JSON
    @Test
    void processRequestShouldReturnInvalidJsonWhenJsonSyntaxIsInvalid() throws Exception {
        dispatcher.processRequest("{ this-is-not-json", session);

        waitUntilMessageCount(1);

        String response = session.sentMessages.get(0);

        assertTrue(response.contains("\"success\":false"));
        assertTrue(response.contains("INVALID_JSON"));
    }

    // Request null JSON phải trả BAD_REQUEST hoặc SERVER_ERROR tùy cách Gson parse
    @Test
    void processRequestShouldReturnFailureWhenSocketRequestIsNull() throws Exception {
        dispatcher.processRequest("null", session);

        waitUntilMessageCount(1);

        String response = session.sentMessages.get(0);

        assertTrue(response.contains("\"success\":false"));
        assertTrue(response.contains("BAD_REQUEST") || response.contains("SERVER_ERROR"));
    }

    // Action không tồn tại hiện tại có thể rơi vào SERVER_ERROR do action parse thành null
    @Test
    void processRequestWithUnknownActionCurrentlyReturnsFailure() throws Exception {
        String badActionJson = """
                {
                  "requestId": "req-unknown",
                  "action": "UNKNOWN_ACTION",
                  "body": "{}"
                }
                """;

        dispatcher.processRequest(badActionJson, session);

        waitUntilMessageCount(1);

        String response = session.sentMessages.get(0);

        assertTrue(response.contains("\"success\":false"));
        assertTrue(response.contains("SERVER_ERROR") || response.contains("UNSUPPORTED_ACTION"));
    }

    // Thiếu action hiện tại rơi vào SERVER_ERROR vì getAction().toString()
    @Test
    void processRequestWithMissingActionCurrentlyReturnsServerError() throws Exception {
        String missingActionJson = """
                {
                  "requestId": "req-missing-action",
                  "body": "{}"
                }
                """;

        dispatcher.processRequest(missingActionJson, session);

        waitUntilMessageCount(1);

        String response = session.sentMessages.get(0);

        assertTrue(response.contains("\"success\":false"));
        assertTrue(response.contains("SERVER_ERROR"));
    }

    // =========================================================
    // rate limit
    // =========================================================

    // Không phải PING thì request thứ 11 trong 1 giây bị chặn TOO_MANY_REQUESTS
    @Test
    void processRequestShouldReturnTooManyRequestsWhenRateLimited() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertFalse(session.isRateLimited());
        }

        assertTrue(session.isRateLimited());

        String requestJson = toJsonRequest(ActionType.GET_ACTIVE_AUCTIONS, null);

        dispatcher.processRequest(requestJson, session);

        waitUntilMessageCount(1);

        String response = session.sentMessages.get(0);

        assertTrue(response.contains("\"success\":false"));
        assertTrue(response.contains("TOO_MANY_REQUESTS"));
        assertTrue(response.contains("Bạn đang gửi quá nhiều yêu cầu"));
    }

    // =========================================================
    // authorization
    // =========================================================

    // Action cần login nhưng session chưa login thì bị chặn
    @Test
    void processRequestShouldReturnFailureWhenActionRequiresLoginButSessionNotLoggedIn() throws Exception {
        String requestJson = toJsonRequest(ActionType.GET_ACTIVE_AUCTIONS, null);

        dispatcher.processRequest(requestJson, session);

        waitUntilMessageCount(1);

        String response = session.sentMessages.get(0);

        assertTrue(response.contains("\"success\":false"));
        assertFalse(response.contains("\"success\":true"));
    }

    // Bidder gọi action của seller thì bị chặn phân quyền
    @Test
    void processRequestShouldReturnFailureWhenRoleIsNotAllowed() throws Exception {
        session.setUserId("bidder-1");
        session.setRole(UserRole.BIDDER);

        String createItemBody = """
                {
                  "itemType": "ELECTRONICS",
                  "name": "Laptop"
                }
                """;

        String requestJson = toJsonRequestWithRawBody(ActionType.CREATE_ITEM, createItemBody);

        dispatcher.processRequest(requestJson, session);

        waitUntilMessageCount(1);

        String response = session.sentMessages.get(0);

        assertTrue(response.contains("\"success\":false"));
        assertFalse(response.contains("\"success\":true"));
    }

    // =========================================================
    // LOGIN
    // =========================================================

    // LOGIN thành công phải set session userId, role, username và register connection
    @Test
    void processRequestShouldLoginAndUpdateSession() throws Exception {
        LoginRequest body = new LoginRequest("bidder01", "Password@123");
        String requestJson = toJsonRequest(ActionType.LOGIN, body);

        dispatcher.processRequest(requestJson, session);

        waitUntilMessageCount(1);

        String response = session.sentMessages.get(0);

        assertTrue(response.contains("\"success\":true"));
        assertTrue(response.contains("\"action\":\"LOGIN\""));
        assertTrue(response.contains("Đăng nhập thành công"));
        assertTrue(response.contains("fake-token"));

        assertEquals("bidder01", authController.lastLoginUsernameOrEmail);
        assertEquals("Password@123", authController.lastLoginPassword);

        assertEquals("user-1", session.getUserId());
        assertEquals(UserRole.BIDDER, session.getRole());
        assertEquals("bidder01", session.getUsername());

        assertTrue(ConnectionManage.getInstance().isUserOnline("user-1"));
    }

    // LOGIN controller ném lỗi thì dispatcher trả failure
    @Test
    void processRequestShouldReturnFailureWhenLoginFails() throws Exception {
        authController.loginShouldThrow = true;

        LoginRequest body = new LoginRequest("wrong", "wrong");
        String requestJson = toJsonRequest(ActionType.LOGIN, body);

        dispatcher.processRequest(requestJson, session);

        waitUntilMessageCount(1);

        String response = session.sentMessages.get(0);

        assertTrue(response.contains("\"success\":false"));
        assertNull(session.getUserId());
        assertNull(session.getRole());
    }

    // =========================================================
    // REGISTER
    // =========================================================

    // REGISTER thành công phải gọi AuthController.register và trả success
    @Test
    void processRequestShouldRegisterSuccessfully() throws Exception {
        RegisterRequest body = new RegisterRequest(
                "bidder01",
                "Password@123",
                "bidder01@example.com",
                UserRole.BIDDER
        );

        String requestJson = toJsonRequest(ActionType.REGISTER, body);

        dispatcher.processRequest(requestJson, session);

        waitUntilMessageCount(1);

        String response = session.sentMessages.get(0);

        assertTrue(response.contains("\"success\":true"));
        assertTrue(response.contains("\"action\":\"REGISTER\""));
        assertTrue(response.contains("Đăng ký thành công"));

        assertEquals("bidder01", authController.lastRegisterUsername);
        assertEquals("Password@123", authController.lastRegisterPassword);
        assertEquals("bidder01@example.com", authController.lastRegisterEmail);
        assertEquals(UserRole.BIDDER, authController.lastRegisterRole);
    }

    // REGISTER controller ném lỗi thì dispatcher trả failure
    @Test
    void processRequestShouldReturnFailureWhenRegisterFails() throws Exception {
        authController.registerShouldThrow = true;

        RegisterRequest body = new RegisterRequest(
                "sameName",
                "Password@123",
                "same@example.com",
                UserRole.BIDDER
        );

        String requestJson = toJsonRequest(ActionType.REGISTER, body);

        dispatcher.processRequest(requestJson, session);

        waitUntilMessageCount(1);

        String response = session.sentMessages.get(0);

        assertTrue(response.contains("\"success\":false"));
        assertEquals("sameName", authController.lastRegisterUsername);
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    // LOGOUT chưa login bị AuthorizationService chặn trước handleLogout
    @Test
    void processRequestShouldReturnFailureWhenLogoutWithoutLogin() throws Exception {
        String requestJson = toJsonRequest(ActionType.LOGOUT, null);

        dispatcher.processRequest(requestJson, session);

        waitUntilMessageCount(1);

        String response = session.sentMessages.get(0);

        assertTrue(response.contains("\"success\":false"));
        assertFalse(response.contains("\"success\":true"));

        // Vì bị chặn ở AuthorizationService nên AuthController.logout không được gọi
        assertNull(authController.lastLogoutUserId);
    }

    // LOGOUT khi đã login phải gọi AuthController.logout và gửi success
    @Test
    void processRequestShouldLogoutWhenSessionLoggedIn() throws Exception {
        session.setUserId("user-1");
        session.setRole(UserRole.BIDDER);
        session.setUsername("bidder01");

        ConnectionManage.getInstance().registerConnection("user-1", session);

        String requestJson = toJsonRequest(ActionType.LOGOUT, null);

        dispatcher.processRequest(requestJson, session);

        waitUntilMessageCount(1);

        String response = session.sentMessages.get(0);

        assertTrue(response.contains("\"success\":true"));
        assertTrue(response.contains("\"action\":\"LOGOUT\""));
        assertTrue(response.contains("Đăng xuất thành công"));

        assertEquals("user-1", authController.lastLogoutUserId);
    }
}