package com.auction.dto;

import com.auction.enums.ActionType;
import com.auction.utils.GsonProvider;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SocketContractTest {

    private final com.google.gson.Gson gson = GsonProvider.getGson();

    // =========================================================
    // SocketRequest
    // =========================================================

    // Constructor rỗng cần cho Gson parse JSON
    @Test
    void socketRequestDefaultConstructorShouldAllowSetters() {
        SocketRequest request = new SocketRequest();

        request.setRequestId("req-1");
        request.setAction(ActionType.LOGIN);
        request.setBody("{\"usernameOrEmail\":\"bidder01\"}");

        assertEquals("req-1", request.getRequestId());
        assertEquals(ActionType.LOGIN, request.getAction());
        assertEquals("{\"usernameOrEmail\":\"bidder01\"}", request.getBody());
    }

    // Constructor với JsonObject phải tự sinh requestId và convert body thành JSON string
    @Test
    void socketRequestJsonObjectConstructorShouldGenerateRequestIdAndStoreBody() {
        JsonObject body = new JsonObject();
        body.addProperty("usernameOrEmail", "bidder01");
        body.addProperty("password", "Password@123");

        SocketRequest request = new SocketRequest(ActionType.LOGIN, body);

        assertNotNull(request.getRequestId());
        assertFalse(request.getRequestId().isBlank());
        assertEquals(ActionType.LOGIN, request.getAction());

        assertTrue(request.getBody().contains("bidder01"));
        assertTrue(request.getBody().contains("Password@123"));
    }

    // Constructor với String body phải giữ nguyên body string
    @Test
    void socketRequestStringConstructorShouldStoreRawBody() {
        String rawBody = """
                {
                  "auctionId": "auction-1",
                  "amount": 1500
                }
                """;

        SocketRequest request = new SocketRequest(ActionType.PLACE_BID, rawBody);

        assertNotNull(request.getRequestId());
        assertEquals(ActionType.PLACE_BID, request.getAction());
        assertSame(rawBody, request.getBody());
    }

    // SocketRequest có thể serialize rồi deserialize lại bằng Gson
    @Test
    void socketRequestShouldSerializeAndDeserializeWithGson() {
        SocketRequest original = new SocketRequest(
                ActionType.GET_AUCTION_DETAIL,
                "{\"auctionId\":\"auction-1\"}"
        );

        String json = gson.toJson(original);
        SocketRequest parsed = gson.fromJson(json, SocketRequest.class);

        assertEquals(original.getRequestId(), parsed.getRequestId());
        assertEquals(ActionType.GET_AUCTION_DETAIL, parsed.getAction());
        assertEquals("{\"auctionId\":\"auction-1\"}", parsed.getBody());
    }

    // Body null vẫn được giữ là null
    @Test
    void socketRequestShouldAllowNullBody() {
        SocketRequest request = new SocketRequest(ActionType.PING, (String) null);

        assertNotNull(request.getRequestId());
        assertEquals(ActionType.PING, request.getAction());
        assertNull(request.getBody());
    }

    // Action null vẫn set được, để tầng RequestDispatcher xử lý lỗi
    @Test
    void socketRequestShouldAllowNullAction() {
        SocketRequest request = new SocketRequest();

        request.setAction(null);
        request.setBody("{}");

        assertNull(request.getAction());
        assertEquals("{}", request.getBody());
    }

    // =========================================================
    // SocketResponse.success()
    // =========================================================

    // success phải tạo response type RESPONSE, success true, errorCode null
    @Test
    void socketResponseSuccessShouldCreateSuccessResponse() {
        UserDTO userDTO = new UserDTO(
                "user-1",
                "bidder01",
                "bidder01@example.com",
                com.auction.enums.UserRole.BIDDER,
                com.auction.enums.UserStatus.ACTIVE,
                1000.0,
                0.0
        );

        SocketResponse response = SocketResponse.success(
                "req-1",
                ActionType.LOGIN,
                "Đăng nhập thành công.",
                userDTO
        );

        assertEquals("req-1", response.getRequestId());
        assertEquals(SocketResponse.TYPE_RESPONSE, response.getType());
        assertEquals(ActionType.LOGIN.name(), response.getAction());
        assertTrue(response.isSuccess());
        assertEquals("Đăng nhập thành công.", response.getMessage());
        assertNull(response.getErrorCode());
        assertNotNull(response.getBody());

        assertTrue(response.getBody().toString().contains("bidder01"));
    }

    // success với body null thì body phải null
    @Test
    void socketResponseSuccessShouldAllowNullBody() {
        SocketResponse response = SocketResponse.success(
                "req-logout",
                ActionType.LOGOUT,
                "Đăng xuất thành công.",
                null
        );

        assertEquals("req-logout", response.getRequestId());
        assertEquals(SocketResponse.TYPE_RESPONSE, response.getType());
        assertEquals(ActionType.LOGOUT.name(), response.getAction());
        assertTrue(response.isSuccess());
        assertNull(response.getErrorCode());
        assertNull(response.getBody());
    }

    // =========================================================
    // SocketResponse.failure()
    // =========================================================

    // failure phải tạo response type RESPONSE, success false, body null
    @Test
    void socketResponseFailureShouldCreateFailureResponse() {
        SocketResponse response = SocketResponse.failure(
                "req-1",
                ActionType.LOGIN,
                "Sai tài khoản hoặc mật khẩu.",
                "AUT_001"
        );

        assertEquals("req-1", response.getRequestId());
        assertEquals(SocketResponse.TYPE_RESPONSE, response.getType());
        assertEquals(ActionType.LOGIN.name(), response.getAction());
        assertFalse(response.isSuccess());
        assertEquals("Sai tài khoản hoặc mật khẩu.", response.getMessage());
        assertEquals("AUT_001", response.getErrorCode());
        assertNull(response.getBody());
    }

    // failure có thể có requestId null khi request lỗi nặng
    @Test
    void socketResponseFailureShouldAllowNullRequestId() {
        SocketResponse response = SocketResponse.failure(
                null,
                null,
                "Request không hợp lệ.",
                "BAD_REQUEST"
        );

        assertNull(response.getRequestId());
        assertEquals(SocketResponse.TYPE_RESPONSE, response.getType());
        assertFalse(response.isSuccess());
        assertEquals("BAD_REQUEST", response.getErrorCode());
    }

    // getAction hiện tại sẽ NullPointerException nếu action null
    @Test
    void socketResponseGetActionShouldThrowWhenActionIsNull() {
        SocketResponse response = SocketResponse.failure(
                null,
                null,
                "Request không hợp lệ.",
                "BAD_REQUEST"
        );

        assertThrows(NullPointerException.class, response::getAction);
    }

    // =========================================================
    // SocketResponse.event()
    // =========================================================

    // event phải có type EVENT, requestId null, success true
    @Test
    void socketResponseEventShouldCreateRealtimeEvent() {
        JsonObject body = new JsonObject();
        body.addProperty("auctionId", "auction-1");
        body.addProperty("highestPrice", 1500.0);

        SocketResponse response = SocketResponse.event(
                ActionType.BID_UPDATE,
                "Có lượt đặt giá mới.",
                body
        );

        assertNull(response.getRequestId());
        assertEquals(SocketResponse.TYPE_EVENT, response.getType());
        assertEquals(ActionType.BID_UPDATE.name(), response.getAction());
        assertTrue(response.isSuccess());
        assertEquals("Có lượt đặt giá mới.", response.getMessage());
        assertNull(response.getErrorCode());
        assertNotNull(response.getBody());

        assertTrue(response.getBody().toString().contains("auction-1"));
        assertTrue(response.getBody().toString().contains("highestPrice"));
    }

    // event với body null vẫn hợp lệ
    @Test
    void socketResponseEventShouldAllowNullBody() {
        SocketResponse response = SocketResponse.event(
                ActionType.TIME_UPDATE,
                "Tick",
                null
        );

        assertNull(response.getRequestId());
        assertEquals(SocketResponse.TYPE_EVENT, response.getType());
        assertEquals(ActionType.TIME_UPDATE.name(), response.getAction());
        assertTrue(response.isSuccess());
        assertNull(response.getErrorCode());
        assertNull(response.getBody());
    }

    // =========================================================
    // Gson serialization
    // =========================================================

    // SocketResponse success serialize ra JSON phải có đủ field quan trọng
    @Test
    void socketResponseShouldSerializeToJsonCorrectly() {
        SocketResponse response = SocketResponse.success(
                "req-1",
                ActionType.PING,
                "PONG",
                null
        );

        String json = gson.toJson(response);

        assertTrue(json.contains("\"requestId\":\"req-1\""));
        assertTrue(json.contains("\"type\":\"RESPONSE\""));
        assertTrue(json.contains("\"action\":\"PING\""));
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"message\":\"PONG\""));
    }

    // SocketResponse deserialize lại phải giữ đúng dữ liệu
    @Test
    void socketResponseShouldDeserializeFromJsonCorrectly() {
        SocketResponse original = SocketResponse.failure(
                "req-1",
                ActionType.PLACE_BID,
                "Bid không hợp lệ.",
                "INVALID_BID"
        );

        String json = gson.toJson(original);
        SocketResponse parsed = gson.fromJson(json, SocketResponse.class);

        assertEquals("req-1", parsed.getRequestId());
        assertEquals(SocketResponse.TYPE_RESPONSE, parsed.getType());
        assertEquals(ActionType.PLACE_BID.name(), parsed.getAction());
        assertFalse(parsed.isSuccess());
        assertEquals("Bid không hợp lệ.", parsed.getMessage());
        assertEquals("INVALID_BID", parsed.getErrorCode());
        assertNull(parsed.getBody());
    }
}