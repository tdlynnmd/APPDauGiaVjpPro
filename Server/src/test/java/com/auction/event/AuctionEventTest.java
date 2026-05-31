package com.auction.event;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuctionEventTest {

    // Constructor phải lưu đúng roomId, type và payload
    @Test
    void constructorShouldStoreRoomIdTypeAndPayload() {
        Map<String, Object> payload = Map.of(
                "newStatus", "RUNNING",
                "message", "Phiên đã bắt đầu"
        );

        AuctionEvent event = new AuctionEvent(
                "auction-1",
                AuctionEventType.STATUS_CHANGED,
                payload
        );

        assertEquals("auction-1", event.getRoomId());
        assertEquals(AuctionEventType.STATUS_CHANGED, event.getType());
        assertSame(payload, event.getPayload());
    }

    // Constructor phải tự tạo timestamp lớn hơn 0
    @Test
    void constructorShouldCreateTimestamp() {
        AuctionEvent event = new AuctionEvent(
                "auction-1",
                AuctionEventType.TIMER_TICK,
                30
        );

        assertTrue(event.getTimestamp() > 0);
    }

    // Timestamp phải nằm trong khoảng thời gian tạo object
    @Test
    void constructorShouldCreateTimestampWithinCurrentTimeRange() {
        long before = System.currentTimeMillis();

        AuctionEvent event = new AuctionEvent(
                "auction-1",
                AuctionEventType.NEW_BID,
                "payload"
        );

        long after = System.currentTimeMillis();

        assertTrue(event.getTimestamp() >= before);
        assertTrue(event.getTimestamp() <= after);
    }

    // Payload null vẫn được chấp nhận
    @Test
    void constructorShouldAllowNullPayload() {
        AuctionEvent event = new AuctionEvent(
                "auction-1",
                AuctionEventType.TIMER_TICK,
                null
        );

        assertEquals("auction-1", event.getRoomId());
        assertEquals(AuctionEventType.TIMER_TICK, event.getType());
        assertNull(event.getPayload());
    }

    // RoomId null vẫn được lưu lại, không crash
    @Test
    void constructorShouldAllowNullRoomId() {
        AuctionEvent event = new AuctionEvent(
                null,
                AuctionEventType.TIMER_TICK,
                30
        );

        assertNull(event.getRoomId());
        assertEquals(AuctionEventType.TIMER_TICK, event.getType());
        assertEquals(30, event.getPayload());
    }

    // Type null vẫn được lưu lại, không crash
    @Test
    void constructorShouldAllowNullType() {
        AuctionEvent event = new AuctionEvent(
                "auction-1",
                null,
                30
        );

        assertEquals("auction-1", event.getRoomId());
        assertNull(event.getType());
        assertEquals(30, event.getPayload());
    }

    // toString phải chứa thông tin cơ bản để debug
    @Test
    void toStringShouldContainBasicEventInformation() {
        AuctionEvent event = new AuctionEvent(
                "auction-1",
                AuctionEventType.NEW_BID,
                "payload"
        );

        String result = event.toString();

        assertTrue(result.contains("auction-1"));
        assertTrue(result.contains("NEW_BID"));
        assertTrue(result.contains("String"));
    }

    // toString với payload null không được crash
    @Test
    void toStringShouldNotThrowWhenPayloadIsNull() {
        AuctionEvent event = new AuctionEvent(
                "auction-1",
                AuctionEventType.TIMER_TICK,
                null
        );

        assertDoesNotThrow(() -> {
            String result = event.toString();

            assertTrue(result.contains("auction-1"));
            assertTrue(result.contains("TIMER_TICK"));
            assertTrue(result.contains("null"));
        });
    }

    // Enum phải có các event realtime quan trọng
    @Test
    void auctionEventTypeShouldContainImportantRealtimeEvents() {
        assertNotNull(AuctionEventType.valueOf("NEW_BID"));
        assertNotNull(AuctionEventType.valueOf("TIMER_TICK"));
        assertNotNull(AuctionEventType.valueOf("STATUS_CHANGED"));
        assertNotNull(AuctionEventType.valueOf("LIVE_ENTERED"));
        assertNotNull(AuctionEventType.valueOf("LIVE_EXITED"));
        assertNotNull(AuctionEventType.valueOf("AUCTION_SUBSCRIBED"));
        assertNotNull(AuctionEventType.valueOf("AUCTION_UNSUBSCRIBED"));
    }
}