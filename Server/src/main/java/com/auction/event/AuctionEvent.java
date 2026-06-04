package com.auction.event;

/**
 * Lớp đóng gói sự kiện liên quan đến phiên đấu giá thời gian thực.
 */
public class AuctionEvent {
    private final String roomId;
    private final AuctionEventType type;
    private final Object payload;
    private final long timestamp;

    public AuctionEvent(String roomId, AuctionEventType type, Object payload) {
        this.roomId = roomId;
        this.type = type;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }

    public String getRoomId() {
        return roomId;
    }

    public AuctionEventType getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "AuctionEvent{" +
                "roomId='" + roomId + '\'' +
                ", type=" + type +
                ", payloadClass=" + (payload != null ? payload.getClass().getSimpleName() : "null") +
                ", timestamp=" + timestamp +
                '}';
    }
}

