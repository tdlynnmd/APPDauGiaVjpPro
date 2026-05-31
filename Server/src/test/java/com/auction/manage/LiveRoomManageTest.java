package com.auction.manage;

import com.auction.dto.BidTransactionDTO;
import com.auction.event.AuctionEvent;
import com.auction.event.AuctionEventType;
import com.auction.network.ClientSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class LiveRoomManageTest {

    private LiveRoomManage liveRoomManage;

    @BeforeEach
    void setUp() throws Exception {
        liveRoomManage = LiveRoomManage.getInstance();
        clearLiveRoomManage();
    }

    // Clear singleton rooms trước mỗi test
    private void clearLiveRoomManage() throws Exception {
        getRoomsMap().clear();
    }

    // Lấy map rooms private bằng reflection
    @SuppressWarnings("unchecked")
    private Map<String, CopyOnWriteArrayList<ClientSession>> getRoomsMap() throws Exception {
        Field field = LiveRoomManage.class.getDeclaredField("rooms");
        field.setAccessible(true);

        return (Map<String, CopyOnWriteArrayList<ClientSession>>) field.get(liveRoomManage);
    }

    // Tạo fake session để không cần socket thật
    private FakeClientSession fakeSession() {
        return new FakeClientSession();
    }

    // Fake ClientSession để bắt message và currentAuctionId
    private static class FakeClientSession extends ClientSession {
        List<String> receivedMessages = new ArrayList<>();
        String currentAuctionIdValue;
        boolean sendSuccess = true;

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
        public void setCurrentAuctionId(String currentAuctionId) {
            this.currentAuctionIdValue = currentAuctionId;
            super.setCurrentAuctionId(currentAuctionId);
        }
    }

    // =========================================================
    // getInstance()
    // =========================================================

    // getInstance nhiều lần phải trả cùng singleton
    @Test
    void getInstanceShouldReturnSameObject() {
        LiveRoomManage first = LiveRoomManage.getInstance();
        LiveRoomManage second = LiveRoomManage.getInstance();

        assertSame(first, second);
    }

    // =========================================================
    // joinRoom()
    // =========================================================

    // joinRoom hợp lệ phải tạo phòng và add session
    @Test
    void joinRoomShouldAddSessionToRoom() {
        FakeClientSession session = fakeSession();

        liveRoomManage.joinRoom("auction-1", session);

        assertEquals(1, liveRoomManage.getRoomSize("auction-1"));
        assertEquals(1, liveRoomManage.getTotalRooms());
        assertEquals("auction-1", session.currentAuctionIdValue);
    }

    // joinRoom auctionId null thì không thêm gì
    @Test
    void joinRoomShouldIgnoreNullAuctionId() {
        FakeClientSession session = fakeSession();

        assertDoesNotThrow(() -> {
            liveRoomManage.joinRoom(null, session);
        });

        assertEquals(0, liveRoomManage.getTotalRooms());
        assertNull(session.currentAuctionIdValue);
    }

    // joinRoom session null thì không thêm gì
    @Test
    void joinRoomShouldIgnoreNullSession() {
        assertDoesNotThrow(() -> {
            liveRoomManage.joinRoom("auction-1", null);
        });

        assertEquals(0, liveRoomManage.getTotalRooms());
        assertEquals(0, liveRoomManage.getRoomSize("auction-1"));
    }

    // joinRoom cùng session 2 lần không được duplicate
    @Test
    void joinRoomShouldNotDuplicateSameSession() {
        FakeClientSession session = fakeSession();

        liveRoomManage.joinRoom("auction-1", session);
        liveRoomManage.joinRoom("auction-1", session);

        assertEquals(1, liveRoomManage.getRoomSize("auction-1"));
        assertEquals(1, liveRoomManage.getTotalRooms());
    }

    // nhiều session join cùng phòng thì room size tăng đúng
    @Test
    void joinRoomShouldAllowMultipleSessionsInSameRoom() {
        FakeClientSession session1 = fakeSession();
        FakeClientSession session2 = fakeSession();

        liveRoomManage.joinRoom("auction-1", session1);
        liveRoomManage.joinRoom("auction-1", session2);

        assertEquals(2, liveRoomManage.getRoomSize("auction-1"));
        assertEquals(1, liveRoomManage.getTotalRooms());
    }

    // nhiều phòng khác nhau thì totalRooms tăng đúng
    @Test
    void joinRoomShouldCreateMultipleRooms() {
        liveRoomManage.joinRoom("auction-1", fakeSession());
        liveRoomManage.joinRoom("auction-2", fakeSession());

        assertEquals(1, liveRoomManage.getRoomSize("auction-1"));
        assertEquals(1, liveRoomManage.getRoomSize("auction-2"));
        assertEquals(2, liveRoomManage.getTotalRooms());
    }

    // =========================================================
    // leaveRoom()
    // =========================================================

    // leaveRoom phải xóa session khỏi phòng
    @Test
    void leaveRoomShouldRemoveSessionFromRoom() {
        FakeClientSession session = fakeSession();

        liveRoomManage.joinRoom("auction-1", session);
        liveRoomManage.leaveRoom("auction-1", session);

        assertEquals(0, liveRoomManage.getRoomSize("auction-1"));
        assertEquals(0, liveRoomManage.getTotalRooms());
        assertNull(session.currentAuctionIdValue);
    }

    // leaveRoom một session nhưng phòng còn session khác thì phòng vẫn tồn tại
    @Test
    void leaveRoomShouldKeepRoomWhenOtherSessionsRemain() {
        FakeClientSession session1 = fakeSession();
        FakeClientSession session2 = fakeSession();

        liveRoomManage.joinRoom("auction-1", session1);
        liveRoomManage.joinRoom("auction-1", session2);

        liveRoomManage.leaveRoom("auction-1", session1);

        assertEquals(1, liveRoomManage.getRoomSize("auction-1"));
        assertEquals(1, liveRoomManage.getTotalRooms());
        assertNull(session1.currentAuctionIdValue);
        assertEquals("auction-1", session2.currentAuctionIdValue);
    }

    // leaveRoom auctionId null thì không crash
    @Test
    void leaveRoomShouldIgnoreNullAuctionId() {
        FakeClientSession session = fakeSession();

        liveRoomManage.joinRoom("auction-1", session);

        assertDoesNotThrow(() -> {
            liveRoomManage.leaveRoom(null, session);
        });

        assertEquals(1, liveRoomManage.getRoomSize("auction-1"));
    }

    // leaveRoom session null thì không crash
    @Test
    void leaveRoomShouldIgnoreNullSession() {
        FakeClientSession session = fakeSession();

        liveRoomManage.joinRoom("auction-1", session);

        assertDoesNotThrow(() -> {
            liveRoomManage.leaveRoom("auction-1", null);
        });

        assertEquals(1, liveRoomManage.getRoomSize("auction-1"));
    }

    // leaveRoom phòng không tồn tại thì không crash
    @Test
    void leaveRoomShouldNotThrowWhenRoomDoesNotExist() {
        assertDoesNotThrow(() -> {
            liveRoomManage.leaveRoom("missing-room", fakeSession());
        });
    }

    // =========================================================
    // getRoomSize() / getTotalRooms()
    // =========================================================

    // getRoomSize phòng không tồn tại trả 0
    @Test
    void getRoomSizeShouldReturnZeroWhenRoomDoesNotExist() {
        assertEquals(0, liveRoomManage.getRoomSize("missing-room"));
    }

    // getRoomSize null hiện tại có thể lỗi vì ConcurrentHashMap không nhận null key
    @Test
    void getRoomSizeShouldThrowWhenAuctionIdIsNull() {
        assertThrows(NullPointerException.class, () -> {
            liveRoomManage.getRoomSize(null);
        });
    }

    // getTotalRooms ban đầu bằng 0
    @Test
    void getTotalRoomsShouldReturnZeroWhenNoRoomExists() {
        assertEquals(0, liveRoomManage.getTotalRooms());
    }

    // =========================================================
    // clearRoom()
    // =========================================================

    // clearRoom phải xóa toàn bộ session khỏi phòng
    @Test
    void clearRoomShouldRemoveRoom() {
        liveRoomManage.joinRoom("auction-1", fakeSession());
        liveRoomManage.joinRoom("auction-1", fakeSession());

        liveRoomManage.clearRoom("auction-1");

        assertEquals(0, liveRoomManage.getRoomSize("auction-1"));
        assertEquals(0, liveRoomManage.getTotalRooms());
    }

    // clearRoom phòng không tồn tại thì không crash
    @Test
    void clearRoomShouldNotThrowWhenRoomDoesNotExist() {
        assertDoesNotThrow(() -> {
            liveRoomManage.clearRoom("missing-room");
        });
    }

    // clearRoom null hiện tại có thể lỗi vì ConcurrentHashMap không nhận null key
    @Test
    void clearRoomShouldThrowWhenAuctionIdIsNull() {
        assertThrows(NullPointerException.class, () -> {
            liveRoomManage.clearRoom(null);
        });
    }

    // =========================================================
    // update() - event handling
    // =========================================================

    // update null event thì không crash
    @Test
    void updateShouldIgnoreNullEvent() {
        assertDoesNotThrow(() -> {
            liveRoomManage.update(null);
        });
    }

    // NEW_BID đúng payload thì broadcast BID_UPDATE cho client trong room
    @Test
    void updateShouldBroadcastNewBidEvent() {
        FakeClientSession session = fakeSession();
        liveRoomManage.joinRoom("auction-1", session);

        BidTransactionDTO bidDTO = new BidTransactionDTO(
                "alice",
                1500.0,
                LocalDateTime.now(),
                "ACCEPTED"
        );

        AuctionEvent event = new AuctionEvent(
                "auction-1",
                AuctionEventType.NEW_BID,
                bidDTO
        );

        liveRoomManage.update(event);

        assertEquals(1, session.receivedMessages.size());

        String message = session.receivedMessages.get(0);
        System.out.println("NEW_BID message = " + message);

        assertTrue(message.contains("BID_UPDATE"));
        assertTrue(message.contains("alice"));
        assertTrue(message.contains("auction-1"));
        assertTrue(message.contains("highestPrice"));
        assertTrue(message.contains("bidTransaction"));
    }

    // NEW_BID sai payload thì không broadcast
    @Test
    void updateShouldNotBroadcastNewBidWhenPayloadInvalid() {
        FakeClientSession session = fakeSession();
        liveRoomManage.joinRoom("auction-1", session);

        AuctionEvent event = new AuctionEvent(
                "auction-1",
                AuctionEventType.NEW_BID,
                "invalid-payload"
        );

        liveRoomManage.update(event);

        assertTrue(session.receivedMessages.isEmpty());
    }

    // TIMER_TICK payload number thì broadcast TIME_UPDATE
    @Test
    void updateShouldBroadcastTimerTickEvent() {
        FakeClientSession session = fakeSession();
        liveRoomManage.joinRoom("auction-1", session);

        AuctionEvent event = new AuctionEvent(
                "auction-1",
                AuctionEventType.TIMER_TICK,
                30
        );

        liveRoomManage.update(event);

        assertEquals(1, session.receivedMessages.size());
        assertTrue(session.receivedMessages.get(0).contains("TIME_UPDATE"));
        assertTrue(session.receivedMessages.get(0).contains("secondsRemaining"));
        assertTrue(session.receivedMessages.get(0).contains("30"));
    }

    // TIMER_TICK sai payload thì không broadcast
    @Test
    void updateShouldNotBroadcastTimerTickWhenPayloadInvalid() {
        FakeClientSession session = fakeSession();
        liveRoomManage.joinRoom("auction-1", session);

        AuctionEvent event = new AuctionEvent(
                "auction-1",
                AuctionEventType.TIMER_TICK,
                "30"
        );

        liveRoomManage.update(event);

        assertTrue(session.receivedMessages.isEmpty());
    }

    // STATUS_CHANGED payload map thì broadcast STATUS_UPDATED
    @Test
    void updateShouldBroadcastStatusChangedEvent() {
        FakeClientSession session = fakeSession();
        liveRoomManage.joinRoom("auction-1", session);

        AuctionEvent event = new AuctionEvent(
                "auction-1",
                AuctionEventType.STATUS_CHANGED,
                Map.of(
                        "newStatus", "RUNNING",
                        "message", "Phiên đã bắt đầu"
                )
        );

        liveRoomManage.update(event);

        assertEquals(1, session.receivedMessages.size());
        assertTrue(session.receivedMessages.get(0).contains("STATUS_UPDATED"));
        assertTrue(session.receivedMessages.get(0).contains("RUNNING"));
        assertEquals(1, liveRoomManage.getTotalRooms());
    }

    // STATUS_CHANGED FINISHED thì broadcast xong phải clear room
    @Test
    void updateShouldClearRoomWhenStatusFinished() {
        FakeClientSession session = fakeSession();
        liveRoomManage.joinRoom("auction-1", session);

        AuctionEvent event = new AuctionEvent(
                "auction-1",
                AuctionEventType.STATUS_CHANGED,
                Map.of(
                        "newStatus", "FINISHED",
                        "message", "Phiên đã kết thúc"
                )
        );

        liveRoomManage.update(event);

        assertEquals(1, session.receivedMessages.size());
        assertEquals(0, liveRoomManage.getTotalRooms());
        assertEquals(0, liveRoomManage.getRoomSize("auction-1"));
    }

    // STATUS_CHANGED CANCELED thì broadcast xong phải clear room
    @Test
    void updateShouldClearRoomWhenStatusCanceled() {
        FakeClientSession session = fakeSession();
        liveRoomManage.joinRoom("auction-1", session);

        AuctionEvent event = new AuctionEvent(
                "auction-1",
                AuctionEventType.STATUS_CHANGED,
                Map.of(
                        "newStatus", "CANCELED",
                        "message", "Phiên đã bị hủy"
                )
        );

        liveRoomManage.update(event);

        assertEquals(1, session.receivedMessages.size());
        assertEquals(0, liveRoomManage.getTotalRooms());
    }

    // STATUS_CHANGED sai payload thì không broadcast
    @Test
    void updateShouldNotBroadcastStatusChangedWhenPayloadInvalid() {
        FakeClientSession session = fakeSession();
        liveRoomManage.joinRoom("auction-1", session);

        AuctionEvent event = new AuctionEvent(
                "auction-1",
                AuctionEventType.STATUS_CHANGED,
                "invalid-payload"
        );

        liveRoomManage.update(event);

        assertTrue(session.receivedMessages.isEmpty());
    }

    // LIVE_ENTERED event thì broadcast LIVE_ENTERED
    @Test
    void updateShouldBroadcastLiveEnteredEvent() {
        FakeClientSession session = fakeSession();
        liveRoomManage.joinRoom("auction-1", session);

        AuctionEvent event = new AuctionEvent(
                "auction-1",
                AuctionEventType.LIVE_ENTERED,
                Map.of(
                        "message", "alice đã vào phòng",
                        "viewerCount", 1,
                        "username", "alice"
                )
        );

        liveRoomManage.update(event);

        assertEquals(1, session.receivedMessages.size());
        assertTrue(session.receivedMessages.get(0).contains("LIVE_ENTERED"));
        assertTrue(session.receivedMessages.get(0).contains("alice"));
    }

    // LIVE_EXITED event thì broadcast LIVE_EXITED
    @Test
    void updateShouldBroadcastLiveExitedEvent() {
        FakeClientSession session = fakeSession();
        liveRoomManage.joinRoom("auction-1", session);

        AuctionEvent event = new AuctionEvent(
                "auction-1",
                AuctionEventType.LIVE_EXITED,
                Map.of(
                        "message", "alice đã rời phòng",
                        "viewerCount", 1,
                        "username", "alice"
                )
        );

        liveRoomManage.update(event);

        assertEquals(1, session.receivedMessages.size());
        assertTrue(session.receivedMessages.get(0).contains("LIVE_EXITED"));
        assertTrue(session.receivedMessages.get(0).contains("alice"));
    }

    // Room notification sai payload thì không broadcast
    @Test
    void updateShouldNotBroadcastRoomNotificationWhenPayloadInvalid() {
        FakeClientSession session = fakeSession();
        liveRoomManage.joinRoom("auction-1", session);

        AuctionEvent event = new AuctionEvent(
                "auction-1",
                AuctionEventType.LIVE_ENTERED,
                "invalid-payload"
        );

        liveRoomManage.update(event);

        assertTrue(session.receivedMessages.isEmpty());
    }

    // broadcast gửi lỗi thì tự remove client khỏi room
    @Test
    void updateShouldRemoveClientWhenSendMessageFails() {
        FakeClientSession brokenSession = fakeSession();
        brokenSession.sendSuccess = false;

        FakeClientSession normalSession = fakeSession();

        liveRoomManage.joinRoom("auction-1", brokenSession);
        liveRoomManage.joinRoom("auction-1", normalSession);

        AuctionEvent event = new AuctionEvent(
                "auction-1",
                AuctionEventType.TIMER_TICK,
                10
        );

        liveRoomManage.update(event);

        assertEquals(1, liveRoomManage.getRoomSize("auction-1"));
        assertEquals(1, normalSession.receivedMessages.size());
        assertNull(brokenSession.currentAuctionIdValue);
    }
}