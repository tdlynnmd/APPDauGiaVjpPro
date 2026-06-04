package com.auction.event;

/**
 * Interface định nghĩa bộ lắng nghe (Observer) cho các sự kiện đấu giá.
 */
public interface AuctionObserver {
    
    void update(AuctionEvent event);
}

