package com.auction.event;

/**
 * Enum định nghĩa các phân loại sự kiện xảy ra trong vòng đời đấu giá.
 */
public enum AuctionEventType {
    
    NEW_BID,

    TIMER_TICK,

    STATUS_CHANGED,

    AUCTION_SUBSCRIBED,

    AUCTION_UNSUBSCRIBED,

    LIVE_ENTERED,

    LIVE_EXITED,

    AUTO_BID_DEACTIVATED
}

