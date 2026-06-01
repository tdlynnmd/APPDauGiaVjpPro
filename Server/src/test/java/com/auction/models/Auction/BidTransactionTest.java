package com.auction.models.Auction;

import com.auction.enums.BidStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BidTransactionTest {

    // Constructor có id dùng khi load từ DB phải lưu đúng toàn bộ field
    @Test
    void constructorWithIdShouldStoreAllFields() {
        LocalDateTime time = LocalDateTime.now();

        BidTransaction bid = new BidTransaction(
                "bid-1",
                "bidder-1",
                "auction-1",
                1500.0,
                time,
                BidStatus.ACCEPTED
        );

        assertEquals("bid-1", bid.getId());
        assertEquals("bidder-1", bid.getBidderId());
        assertEquals("auction-1", bid.getAuctionId());
        assertEquals(1500.0, bid.getAmount());
        assertSame(time, bid.getTime());
        assertEquals(BidStatus.ACCEPTED, bid.getStatus());
    }

    // Constructor không id dùng khi tạo bid mới phải tự có id từ Entity
    @Test
    void constructorWithoutIdShouldGenerateEntityIdAndStoreFields() {
        LocalDateTime time = LocalDateTime.now();

        BidTransaction bid = new BidTransaction(
                "bidder-1",
                "auction-1",
                1500.0,
                time,
                BidStatus.ACCEPTED
        );

        assertNotNull(bid.getId());
        assertFalse(bid.getId().isBlank());

        assertEquals("bidder-1", bid.getBidderId());
        assertEquals("auction-1", bid.getAuctionId());
        assertEquals(1500.0, bid.getAmount());
        assertSame(time, bid.getTime());
        assertEquals(BidStatus.ACCEPTED, bid.getStatus());
    }

    // setStatus phải đổi trạng thái bid
    @Test
    void setStatusShouldUpdateBidStatus() {
        LocalDateTime time = LocalDateTime.now();

        BidTransaction bid = new BidTransaction(
                "bid-1",
                "bidder-1",
                "auction-1",
                1500.0,
                time,
                BidStatus.ACCEPTED
        );

        bid.setStatus(BidStatus.REFUNDED);

        assertEquals(BidStatus.REFUNDED, bid.getStatus());
    }

    // Bid có thể chuyển sang REJECTED
    @Test
    void setStatusShouldAllowRejected() {
        BidTransaction bid = new BidTransaction(
                "bid-1",
                "bidder-1",
                "auction-1",
                1500.0,
                LocalDateTime.now(),
                BidStatus.ACCEPTED
        );

        bid.setStatus(BidStatus.REJECTED);

        assertEquals(BidStatus.REJECTED, bid.getStatus());
    }

    // bidderId null hiện tại vẫn được lưu, validate nằm ở AuctionService/Auction
    @Test
    void constructorShouldAllowNullBidderId() {
        BidTransaction bid = new BidTransaction(
                "bid-1",
                null,
                "auction-1",
                1500.0,
                LocalDateTime.now(),
                BidStatus.ACCEPTED
        );

        assertNull(bid.getBidderId());
        assertEquals("auction-1", bid.getAuctionId());
    }

    // auctionId null hiện tại vẫn được lưu, validate nằm ở service
    @Test
    void constructorShouldAllowNullAuctionId() {
        BidTransaction bid = new BidTransaction(
                "bid-1",
                "bidder-1",
                null,
                1500.0,
                LocalDateTime.now(),
                BidStatus.ACCEPTED
        );

        assertEquals("bidder-1", bid.getBidderId());
        assertNull(bid.getAuctionId());
    }

    // time null hiện tại vẫn được lưu, validate nằm ở DAO/service
    @Test
    void constructorShouldAllowNullTime() {
        BidTransaction bid = new BidTransaction(
                "bid-1",
                "bidder-1",
                "auction-1",
                1500.0,
                null,
                BidStatus.ACCEPTED
        );

        assertNull(bid.getTime());
        assertEquals(BidStatus.ACCEPTED, bid.getStatus());
    }

    // status null hiện tại vẫn được lưu
    @Test
    void constructorShouldAllowNullStatus() {
        BidTransaction bid = new BidTransaction(
                "bid-1",
                "bidder-1",
                "auction-1",
                1500.0,
                LocalDateTime.now(),
                null
        );

        assertNull(bid.getStatus());
    }

    // amount âm hiện tại vẫn được lưu, validate nằm ở Auction.placeBid/service
    @Test
    void constructorShouldAllowNegativeAmount() {
        BidTransaction bid = new BidTransaction(
                "bid-1",
                "bidder-1",
                "auction-1",
                -100.0,
                LocalDateTime.now(),
                BidStatus.REJECTED
        );

        assertEquals(-100.0, bid.getAmount());
        assertEquals(BidStatus.REJECTED, bid.getStatus());
    }

    // amount bằng 0 hiện tại vẫn được lưu, validate nằm ở tầng trên
    @Test
    void constructorShouldAllowZeroAmount() {
        BidTransaction bid = new BidTransaction(
                "bid-1",
                "bidder-1",
                "auction-1",
                0.0,
                LocalDateTime.now(),
                BidStatus.REJECTED
        );

        assertEquals(0.0, bid.getAmount());
    }

    // Constructor có id null hiện tại vẫn cho phép, phụ thuộc Entity xử lý
    @Test
    void constructorWithNullIdShouldNotThrow() {
        LocalDateTime time = LocalDateTime.now();

        assertDoesNotThrow(() -> {
            BidTransaction bid = new BidTransaction(
                    null,
                    "bidder-1",
                    "auction-1",
                    1500.0,
                    time,
                    BidStatus.ACCEPTED
            );

            assertNull(bid.getId());
        });
    }
}