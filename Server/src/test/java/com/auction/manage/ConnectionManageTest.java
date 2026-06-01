package com.auction.manage;

import com.auction.network.ClientSession;
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

class ConnectionManageTest {

    private ConnectionManage connectionManage;

    @BeforeEach
    void setUp() throws Exception {
        connectionManage = ConnectionManage.getInstance();
        clearConnectionManage();
    }

    // Clear singleton RAM trước mỗi test
    private void clearConnectionManage() throws Exception {
        getActiveConnectionsMap().clear();
    }

    // Lấy map activeConnections bằng reflection
    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<String, Set<ClientSession>> getActiveConnectionsMap() throws Exception {
        Field field = ConnectionManage.class.getDeclaredField("activeConnections");
        field.setAccessible(true);

        return (ConcurrentHashMap<String, Set<ClientSession>>) field.get(connectionManage);
    }

    // Tạo fake session để không cần socket thật
    private FakeClientSession fakeSession() {
        return new FakeClientSession();
    }

    // Fake ClientSession để kiểm tra sendMessage và close
    private static class FakeClientSession extends ClientSession {
        boolean closed = false;
        boolean sendSuccess = true;
        List<String> receivedMessages = new ArrayList<>();

        FakeClientSession() {
            super((Socket) null, new PrintWriter(System.out));
        }

        @Override
        public boolean sendMessage(String jsonMessage) {
            if (!sendSuccess) {
                return false;
            }

            receivedMessages.add(jsonMessage);
            return true;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    // =========================================================
    // getInstance()
    // =========================================================

    // getInstance nhiều lần phải trả về cùng singleton
    @Test
    void getInstanceShouldReturnSameObject() {
        ConnectionManage first = ConnectionManage.getInstance();
        ConnectionManage second = ConnectionManage.getInstance();

        assertSame(first, second);
    }

    // =========================================================
    // registerConnection()
    // =========================================================

    // registerConnection hợp lệ phải đưa user online
    @Test
    void registerConnectionShouldMakeUserOnline() {
        FakeClientSession session = fakeSession();

        connectionManage.registerConnection("user-1", session);

        assertTrue(connectionManage.isUserOnline("user-1"));
        assertEquals(1, connectionManage.getOnlineUserCount());
        assertEquals(1, connectionManage.getOnlineCount());
    }

    // registerConnection userId null thì không thêm gì
    @Test
    void registerConnectionShouldIgnoreNullUserId() {
        FakeClientSession session = fakeSession();

        assertDoesNotThrow(() -> {
            connectionManage.registerConnection(null, session);
        });

        assertEquals(0, connectionManage.getOnlineUserCount());
        assertEquals(0, connectionManage.getOnlineCount());
    }

    // registerConnection session null thì không thêm gì
    @Test
    void registerConnectionShouldIgnoreNullSession() {
        connectionManage.registerConnection("user-1", null);

        assertFalse(connectionManage.isUserOnline("user-1"));
        assertEquals(0, connectionManage.getOnlineUserCount());
        assertEquals(0, connectionManage.getOnlineCount());
    }

    // Một user có nhiều session nhưng chỉ tính là 1 user online
    @Test
    void registerConnectionShouldAllowMultipleSessionsForSameUser() {
        FakeClientSession session1 = fakeSession();
        FakeClientSession session2 = fakeSession();

        connectionManage.registerConnection("user-1", session1);
        connectionManage.registerConnection("user-1", session2);

        assertTrue(connectionManage.isUserOnline("user-1"));
        assertEquals(1, connectionManage.getOnlineUserCount());
        assertEquals(2, connectionManage.getOnlineCount());
    }

    // Nhiều user online thì getOnlineUserCount đếm số user
    @Test
    void registerConnectionShouldTrackMultipleUsers() {
        connectionManage.registerConnection("user-1", fakeSession());
        connectionManage.registerConnection("user-2", fakeSession());

        assertTrue(connectionManage.isUserOnline("user-1"));
        assertTrue(connectionManage.isUserOnline("user-2"));
        assertEquals(2, connectionManage.getOnlineUserCount());
        assertEquals(2, connectionManage.getOnlineCount());
    }

    // Add cùng session 2 lần không được nhân đôi vì dùng Set
    @Test
    void registerConnectionShouldNotDuplicateSameSession() {
        FakeClientSession session = fakeSession();

        connectionManage.registerConnection("user-1", session);
        connectionManage.registerConnection("user-1", session);

        assertEquals(1, connectionManage.getOnlineUserCount());
        assertEquals(1, connectionManage.getOnlineCount());
    }

    // =========================================================
    // removeConnection()
    // =========================================================

    // removeConnection xóa session cuối thì user offline
    @Test
    void removeConnectionShouldMakeUserOfflineWhenLastSessionRemoved() {
        FakeClientSession session = fakeSession();

        connectionManage.registerConnection("user-1", session);
        connectionManage.removeConnection("user-1", session);

        assertFalse(connectionManage.isUserOnline("user-1"));
        assertEquals(0, connectionManage.getOnlineUserCount());
        assertEquals(0, connectionManage.getOnlineCount());
    }

    // removeConnection một session nhưng user còn session khác thì vẫn online
    @Test
    void removeConnectionShouldKeepUserOnlineWhenOtherSessionsRemain() {
        FakeClientSession session1 = fakeSession();
        FakeClientSession session2 = fakeSession();

        connectionManage.registerConnection("user-1", session1);
        connectionManage.registerConnection("user-1", session2);

        connectionManage.removeConnection("user-1", session1);

        assertTrue(connectionManage.isUserOnline("user-1"));
        assertEquals(1, connectionManage.getOnlineUserCount());
        assertEquals(1, connectionManage.getOnlineCount());
    }

    // removeConnection user không tồn tại thì không crash
    @Test
    void removeConnectionShouldNotThrowWhenUserDoesNotExist() {
        assertDoesNotThrow(() -> {
            connectionManage.removeConnection("missing-user", fakeSession());
        });
    }

    // removeConnection session không tồn tại thì không xóa user đang online
    @Test
    void removeConnectionShouldNotRemoveUserWhenSessionDoesNotExist() {
        FakeClientSession realSession = fakeSession();
        FakeClientSession otherSession = fakeSession();

        connectionManage.registerConnection("user-1", realSession);

        connectionManage.removeConnection("user-1", otherSession);

        assertTrue(connectionManage.isUserOnline("user-1"));
        assertEquals(1, connectionManage.getOnlineCount());
    }

    // removeConnection userId null hiện tại có thể lỗi vì ConcurrentHashMap không nhận null key
    @Test
    void removeConnectionShouldThrowWhenUserIdIsNull() {
        assertThrows(NullPointerException.class, () -> {
            connectionManage.removeConnection(null, fakeSession());
        });
    }

    // removeConnection session null sẽ xóa không được session thật
    @Test
    void removeConnectionShouldNotRemoveRealSessionWhenSessionIsNull() {
        FakeClientSession session = fakeSession();

        connectionManage.registerConnection("user-1", session);
        connectionManage.removeConnection("user-1", null);

        assertTrue(connectionManage.isUserOnline("user-1"));
        assertEquals(1, connectionManage.getOnlineCount());
    }

    // =========================================================
    // isUserOnline()
    // =========================================================

    // isUserOnline true nếu user có ít nhất 1 session
    @Test
    void isUserOnlineShouldReturnTrueWhenUserHasSession() {
        connectionManage.registerConnection("user-1", fakeSession());

        assertTrue(connectionManage.isUserOnline("user-1"));
    }

    // isUserOnline false nếu user không có session
    @Test
    void isUserOnlineShouldReturnFalseWhenUserHasNoSession() {
        assertFalse(connectionManage.isUserOnline("missing-user"));
    }

    // isUserOnline(null) hiện tại lỗi vì ConcurrentHashMap không nhận null key
    @Test
    void isUserOnlineShouldThrowWhenUserIdIsNull() {
        assertThrows(NullPointerException.class, () -> {
            connectionManage.isUserOnline(null);
        });
    }

    // =========================================================
    // getOnlineUserCount() / getOnlineCount()
    // =========================================================

    // Khi rỗng thì số user online và số session đều bằng 0
    @Test
    void onlineCountsShouldBeZeroWhenNoConnectionExists() {
        assertEquals(0, connectionManage.getOnlineUserCount());
        assertEquals(0, connectionManage.getOnlineCount());
    }

    // getOnlineUserCount đếm user, getOnlineCount đếm thiết bị/session
    @Test
    void onlineCountsShouldDistinguishUsersAndDevices() {
        connectionManage.registerConnection("user-1", fakeSession());
        connectionManage.registerConnection("user-1", fakeSession());
        connectionManage.registerConnection("user-2", fakeSession());

        assertEquals(2, connectionManage.getOnlineUserCount());
        assertEquals(3, connectionManage.getOnlineCount());
    }

    // =========================================================
    // sendMessageToUser()
    // =========================================================

    // sendMessageToUser gửi message cho tất cả session của user
    @Test
    void sendMessageToUserShouldSendMessageToAllUserSessions() {
        FakeClientSession session1 = fakeSession();
        FakeClientSession session2 = fakeSession();

        connectionManage.registerConnection("user-1", session1);
        connectionManage.registerConnection("user-1", session2);

        connectionManage.sendMessageToUser("user-1", "hello");

        assertEquals(List.of("hello"), session1.receivedMessages);
        assertEquals(List.of("hello"), session2.receivedMessages);
    }

    // sendMessageToUser user không online thì không crash
    @Test
    void sendMessageToUserShouldNotThrowWhenUserIsOffline() {
        assertDoesNotThrow(() -> {
            connectionManage.sendMessageToUser("missing-user", "hello");
        });
    }

    // sendMessageToUser userId null hiện tại lỗi vì ConcurrentHashMap không nhận null key
    @Test
    void sendMessageToUserShouldThrowWhenUserIdIsNull() {
        assertThrows(NullPointerException.class, () -> {
            connectionManage.sendMessageToUser(null, "hello");
        });
    }

    // sendMessageToUser vẫn gửi được message null nếu session send thành công
    @Test
    void sendMessageToUserShouldAllowNullMessage() {
        FakeClientSession session = fakeSession();

        connectionManage.registerConnection("user-1", session);
        connectionManage.sendMessageToUser("user-1", null);

        assertEquals(1, session.receivedMessages.size());
        assertNull(session.receivedMessages.get(0));
    }

    // sendMessageToUser nếu một session gửi thất bại thì tự remove session đó
    @Test
    void sendMessageToUserShouldRemoveFailedSession() {
        FakeClientSession failedSession = fakeSession();
        failedSession.sendSuccess = false;

        FakeClientSession normalSession = fakeSession();

        connectionManage.registerConnection("user-1", failedSession);
        connectionManage.registerConnection("user-1", normalSession);

        connectionManage.sendMessageToUser("user-1", "hello");

        assertTrue(failedSession.closed);
        assertTrue(failedSession.receivedMessages.isEmpty());

        assertEquals(List.of("hello"), normalSession.receivedMessages);

        assertTrue(connectionManage.isUserOnline("user-1"));
        assertEquals(1, connectionManage.getOnlineUserCount());
        assertEquals(1, connectionManage.getOnlineCount());
    }

    // sendMessageToUser nếu session cuối cùng gửi thất bại thì user offline
    @Test
    void sendMessageToUserShouldMakeUserOfflineWhenLastSessionFails() {
        FakeClientSession failedSession = fakeSession();
        failedSession.sendSuccess = false;

        connectionManage.registerConnection("user-1", failedSession);

        connectionManage.sendMessageToUser("user-1", "hello");

        assertTrue(failedSession.closed);
        assertFalse(connectionManage.isUserOnline("user-1"));
        assertEquals(0, connectionManage.getOnlineUserCount());
        assertEquals(0, connectionManage.getOnlineCount());
    }

    // =========================================================
    // forceDisconnectUser()
    // =========================================================

    // forceDisconnectUser phải close toàn bộ session của user và xóa user khỏi online map
    @Test
    void forceDisconnectUserShouldCloseAllSessionsOfUser() {
        FakeClientSession session1 = fakeSession();
        FakeClientSession session2 = fakeSession();

        connectionManage.registerConnection("user-1", session1);
        connectionManage.registerConnection("user-1", session2);

        connectionManage.forceDisconnectUser("user-1");

        assertTrue(session1.closed);
        assertTrue(session2.closed);
        assertFalse(connectionManage.isUserOnline("user-1"));
        assertEquals(0, connectionManage.getOnlineCount());
    }

    // forceDisconnectUser user không tồn tại thì không crash
    @Test
    void forceDisconnectUserShouldNotThrowWhenUserDoesNotExist() {
        assertDoesNotThrow(() -> {
            connectionManage.forceDisconnectUser("missing-user");
        });
    }

    // forceDisconnectUser null hiện tại lỗi vì ConcurrentHashMap không nhận null key
    @Test
    void forceDisconnectUserShouldThrowWhenUserIdIsNull() {
        assertThrows(NullPointerException.class, () -> {
            connectionManage.forceDisconnectUser(null);
        });
    }

    // forceDisconnectUser chỉ disconnect đúng user cần khóa
    @Test
    void forceDisconnectUserShouldOnlyDisconnectTargetUser() {
        FakeClientSession user1Session = fakeSession();
        FakeClientSession user2Session = fakeSession();

        connectionManage.registerConnection("user-1", user1Session);
        connectionManage.registerConnection("user-2", user2Session);

        connectionManage.forceDisconnectUser("user-1");

        assertTrue(user1Session.closed);
        assertFalse(user2Session.closed);

        assertFalse(connectionManage.isUserOnline("user-1"));
        assertTrue(connectionManage.isUserOnline("user-2"));
        assertEquals(1, connectionManage.getOnlineUserCount());
        assertEquals(1, connectionManage.getOnlineCount());
    }

    // =========================================================
    // closeAllConnections()
    // =========================================================

    // closeAllConnections khi rỗng không crash
    @Test
    void closeAllConnectionsShouldNotThrowWhenNoConnectionExists() {
        assertDoesNotThrow(() -> {
            connectionManage.closeAllConnections();
        });
    }

    // closeAllConnections phải close tất cả session và clear map
    @Test
    void closeAllConnectionsShouldCloseEverySessionAndClearMemory() {
        FakeClientSession user1Session1 = fakeSession();
        FakeClientSession user1Session2 = fakeSession();
        FakeClientSession user2Session = fakeSession();

        connectionManage.registerConnection("user-1", user1Session1);
        connectionManage.registerConnection("user-1", user1Session2);
        connectionManage.registerConnection("user-2", user2Session);

        connectionManage.closeAllConnections();

        assertTrue(user1Session1.closed);
        assertTrue(user1Session2.closed);
        assertTrue(user2Session.closed);

        assertEquals(0, connectionManage.getOnlineUserCount());
        assertEquals(0, connectionManage.getOnlineCount());
    }
}