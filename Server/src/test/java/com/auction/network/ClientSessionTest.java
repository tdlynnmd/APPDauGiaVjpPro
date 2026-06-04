package com.auction.network;

import com.auction.enums.UserRole;
import com.auction.manage.ConnectionManage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;

class ClientSessionTest {

    @BeforeEach
    void setUp() throws Exception {
        clearConnectionManage();
    }

    // Clear ConnectionManage singleton để test close/remove không bị dính test cũ
    @SuppressWarnings("unchecked")
    private void clearConnectionManage() throws Exception {
        ConnectionManage connectionManage = ConnectionManage.getInstance();

        Field field = ConnectionManage.class.getDeclaredField("activeConnections");
        field.setAccessible(true);

        ConcurrentHashMap<String, Set<ClientSession>> map =
                (ConcurrentHashMap<String, Set<ClientSession>>) field.get(connectionManage);

        map.clear();
    }

    // Tạo session với writer thường
    private ClientSession sessionWithWriter(StringWriter stringWriter) {
        return new ClientSession(
                (Socket) null,
                new PrintWriter(stringWriter)
        );
    }

    // Tạo session không có writer, sendMessage sẽ false
    private ClientSession sessionWithoutWriter() {
        return new ClientSession(
                (Socket) null,
                null
        );
    }

    // PrintWriter giả lập lỗi checkError = true
    private static class BrokenPrintWriter extends PrintWriter {
        BrokenPrintWriter() {
            super(new StringWriter());
        }

        @Override
        public boolean checkError() {
            return true;
        }
    }

    // =========================================================
    // login state
    // =========================================================

    // Session mới tạo chưa được coi là login
    @Test
    void newSessionShouldNotBeLoggedIn() {
        ClientSession session = sessionWithoutWriter();

        assertFalse(session.isLoggedIn());
        assertNull(session.getUserId());
        assertNull(session.getRole());
        assertNull(session.getUsername());
    }

    // Có userId nhưng chưa có role thì vẫn chưa login
    @Test
    void isLoggedInShouldReturnFalseWhenOnlyUserIdExists() {
        ClientSession session = sessionWithoutWriter();

        session.setUserId("user-1");

        assertFalse(session.isLoggedIn());
    }

    // Có role nhưng chưa có userId thì vẫn chưa login
    @Test
    void isLoggedInShouldReturnFalseWhenOnlyRoleExists() {
        ClientSession session = sessionWithoutWriter();

        session.setRole(UserRole.BIDDER);

        assertFalse(session.isLoggedIn());
    }

    // Có cả userId và role thì được coi là đã login
    @Test
    void isLoggedInShouldReturnTrueWhenUserIdAndRoleExist() {
        ClientSession session = sessionWithoutWriter();

        session.setUserId("user-1");
        session.setRole(UserRole.BIDDER);

        assertTrue(session.isLoggedIn());
    }

    // setUsername/getUsername phải lưu đúng username
    @Test
    void usernameShouldBeStoredCorrectly() {
        ClientSession session = sessionWithoutWriter();

        session.setUsername("bidder01");

        assertEquals("bidder01", session.getUsername());
    }

    // clearLoginInfo phải xóa userId và role nhưng không xóa username
    @Test
    void clearLoginInfoShouldClearUserIdAndRoleOnly() {
        ClientSession session = sessionWithoutWriter();

        session.setUserId("user-1");
        session.setRole(UserRole.BIDDER);
        session.setUsername("bidder01");

        session.clearLoginInfo();

        assertNull(session.getUserId());
        assertNull(session.getRole());
        assertEquals("bidder01", session.getUsername());
        assertFalse(session.isLoggedIn());
    }

    // =========================================================
    // sendMessage()
    // =========================================================

    // sendMessage có writer hợp lệ phải ghi message và trả true
    @Test
    void sendMessageShouldWriteMessageAndReturnTrue() {
        StringWriter stringWriter = new StringWriter();
        ClientSession session = sessionWithWriter(stringWriter);

        boolean result = session.sendMessage("hello");

        assertTrue(result);
        assertTrue(stringWriter.toString().contains("hello"));
    }

    // sendMessage null vẫn ghi được chuỗi null và trả true
    @Test
    void sendMessageShouldAllowNullMessageWhenWriterExists() {
        StringWriter stringWriter = new StringWriter();
        ClientSession session = sessionWithWriter(stringWriter);

        boolean result = session.sendMessage(null);

        assertTrue(result);
        assertTrue(stringWriter.toString().contains("null"));
    }

    // sendMessage không có writer thì trả false
    @Test
    void sendMessageShouldReturnFalseWhenWriterIsNull() {
        ClientSession session = sessionWithoutWriter();

        boolean result = session.sendMessage("hello");

        assertFalse(result);
    }

    // sendMessage writer báo lỗi thì trả false
    @Test
    void sendMessageShouldReturnFalseWhenWriterHasError() {
        ClientSession session = new ClientSession(
                (Socket) null,
                new BrokenPrintWriter()
        );

        boolean result = session.sendMessage("hello");

        assertFalse(result);
    }

    // =========================================================
    // rate limiter
    // =========================================================

    // 10 request đầu tiên trong 1 giây không bị chặn
    @Test
    void isRateLimitedShouldAllowFirstTenRequestsInOneSecond() {
        ClientSession session = sessionWithoutWriter();

        for (int i = 0; i < 10; i++) {
            assertFalse(session.isRateLimited());
        }
    }

    // Request thứ 11 trong cùng cửa sổ 1 giây bị chặn
    @Test
    void isRateLimitedShouldBlockEleventhRequestInOneSecond() {
        ClientSession session = sessionWithoutWriter();

        for (int i = 0; i < 10; i++) {
            assertFalse(session.isRateLimited());
        }

        assertTrue(session.isRateLimited());
    }

    // Sau khi qua cửa sổ 1 giây, request lại được cho qua
    @Test
    void isRateLimitedShouldAllowAgainAfterWindowExpires() throws InterruptedException {
        ClientSession session = sessionWithoutWriter();

        for (int i = 0; i < 10; i++) {
            assertFalse(session.isRateLimited());
        }

        assertTrue(session.isRateLimited());

        Thread.sleep(1100);

        assertFalse(session.isRateLimited());
    }

    // Mỗi session có rate limiter riêng, không ảnh hưởng nhau
    @Test
    void isRateLimitedShouldBeIndependentPerSession() {
        ClientSession session1 = sessionWithoutWriter();
        ClientSession session2 = sessionWithoutWriter();

        for (int i = 0; i < 10; i++) {
            assertFalse(session1.isRateLimited());
        }

        assertTrue(session1.isRateLimited());

        assertFalse(session2.isRateLimited());
    }

    // =========================================================
    // getSessionExecutor()
    // =========================================================

    // getSessionExecutor phải trả executor không null
    @Test
    void getSessionExecutorShouldReturnExecutor() {
        ClientSession session = sessionWithoutWriter();

        ExecutorService executor = session.getSessionExecutor();

        assertNotNull(executor);
        assertFalse(executor.isShutdown());
    }

    // Mỗi session phải có executor riêng
    @Test
    void eachSessionShouldHaveDifferentExecutor() {
        ClientSession session1 = sessionWithoutWriter();
        ClientSession session2 = sessionWithoutWriter();

        assertNotSame(session1.getSessionExecutor(), session2.getSessionExecutor());
    }

    // =========================================================
    // close()
    // =========================================================

    // close session chưa login không được crash và phải shutdown executor
    @Test
    void closeShouldNotThrowWhenSessionIsNotLoggedIn() {
        ClientSession session = sessionWithoutWriter();

        assertDoesNotThrow(session::close);

        assertTrue(session.getSessionExecutor().isShutdown());
    }

    // close phải idempotent: gọi nhiều lần không crash
    @Test
    void closeShouldBeIdempotent() {
        ClientSession session = sessionWithoutWriter();

        assertDoesNotThrow(session::close);
        assertDoesNotThrow(session::close);
        assertDoesNotThrow(session::close);

        assertTrue(session.getSessionExecutor().isShutdown());
    }

    // close phải đóng writer, sau close gửi tiếp sẽ false hoặc không ghi thêm được dữ liệu đáng tin cậy
    @Test
    void closeShouldCloseWriterAndShutdownExecutor() {
        StringWriter stringWriter = new StringWriter();
        ClientSession session = sessionWithWriter(stringWriter);

        session.close();

        assertTrue(session.getSessionExecutor().isShutdown());
    }

    // close session đã đăng ký trong ConnectionManage phải remove connection
    @Test
    void closeShouldRemoveConnectionWhenUserIdExists() {
        ClientSession session = sessionWithoutWriter();

        session.setUserId("user-1");
        session.setRole(UserRole.BIDDER);

        ConnectionManage.getInstance().registerConnection("user-1", session);

        assertTrue(ConnectionManage.getInstance().isUserOnline("user-1"));

        session.close();

        assertFalse(ConnectionManage.getInstance().isUserOnline("user-1"));
        assertEquals(0, ConnectionManage.getInstance().getOnlineCount());
        assertTrue(session.getSessionExecutor().isShutdown());
    }

    // close không nên xóa username field, chỉ đóng session
    @Test
    void closeShouldKeepUsernameValue() {
        ClientSession session = sessionWithoutWriter();

        session.setUsername("bidder01");

        session.close();

        assertEquals("bidder01", session.getUsername());
    }

    // close khi client ở trong phòng live (currentAuctionId != null) phải rời phòng live và phát LIVE_EXITED event
    @Test
    void closeShouldCleanupLiveRoomAndPublishExitedEvent() {
        ClientSession session = sessionWithoutWriter();
        session.setUserId("user-1");
        session.setUsername("bidder01");
        session.setRole(UserRole.BIDDER);
        
        String auctionId = "auction-test-123";
        
        // Đưa session vào phòng live
        com.auction.manage.LiveRoomManage.getInstance().joinRoom(auctionId, session);
        session.setCurrentAuctionId(auctionId);
        
        assertEquals(1, com.auction.manage.LiveRoomManage.getInstance().getRoomSize(auctionId));
        
        // Đăng ký một observer để hứng event kiểm tra
        java.util.concurrent.atomic.AtomicReference<com.auction.event.AuctionEvent> receivedEvent = new java.util.concurrent.atomic.AtomicReference<>();
        com.auction.event.AuctionObserver observer = receivedEvent::set;
        com.auction.event.AuctionEventBus.getInstance().attach(observer);
        
        try {
            // Thực thi đóng session đột ngột (giả lập sập nguồn/mất mạng)
            session.close();
            
            // 1. Phải dọn dẹp sạch sẽ danh sách phòng live
            assertEquals(0, com.auction.manage.LiveRoomManage.getInstance().getRoomSize(auctionId));
            
            // 2. Phải bắn sự kiện LIVE_EXITED đồng bộ
            com.auction.event.AuctionEvent event = receivedEvent.get();
            assertNotNull(event);
            assertEquals(auctionId, event.getRoomId());
            assertEquals(com.auction.event.AuctionEventType.LIVE_EXITED, event.getType());
            
            // 3. Payload phải chuẩn để Client cập nhật số người xem: viewerCount = 0
            Object payload = event.getPayload();
            assertTrue(payload instanceof java.util.Map);
            java.util.Map<?, ?> payloadMap = (java.util.Map<?, ?>) payload;
            assertEquals("bidder01", payloadMap.get("username"));
            assertEquals(0, payloadMap.get("viewerCount"));
        } finally {
            com.auction.event.AuctionEventBus.getInstance().detach(observer);
            com.auction.manage.LiveRoomManage.getInstance().clearRoom(auctionId);
        }
    }
}