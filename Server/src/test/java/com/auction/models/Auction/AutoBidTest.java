package com.auction.models.Auction;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AutoBidTest {

    // Constructor tạo mới phải lưu đúng userId, auctionId, maxBid, increment
    @Test
    void constructorShouldStoreBasicFields() {
        AutoBid autoBid = new AutoBid(
                "bidder-1",
                "auction-1",
                5000.0,
                200.0
        );

        assertEquals("bidder-1", autoBid.getUserId());
        assertEquals("auction-1", autoBid.getAuctionId());
        assertEquals(5000.0, autoBid.getMaxBid());
        assertEquals(200.0, autoBid.getIncrement());
    }

    // AutoBid mới tạo phải active
    @Test
    void newAutoBidShouldBeActiveByDefault() {
        AutoBid autoBid = new AutoBid(
                "bidder-1",
                "auction-1",
                5000.0,
                200.0
        );

        assertTrue(autoBid.isActive());
    }

    // AutoBid mới tạo phải có createdAt
    @Test
    void newAutoBidShouldHaveCreatedAt() {
        LocalDateTime before = LocalDateTime.now();

        AutoBid autoBid = new AutoBid(
                "bidder-1",
                "auction-1",
                5000.0,
                200.0
        );

        LocalDateTime after = LocalDateTime.now();

        assertNotNull(autoBid.getCreatedAt());
        assertFalse(autoBid.getCreatedAt().isBefore(before));
        assertFalse(autoBid.getCreatedAt().isAfter(after));
    }

    // Constructor load DB phải giữ đúng id, active và createdAt
    @Test
    void hydrationConstructorShouldStoreAllFields() {
        LocalDateTime createdAt = LocalDateTime.now().minusHours(1);

        AutoBid autoBid = new AutoBid(
                "auto-1",
                "bidder-1",
                "auction-1",
                5000.0,
                200.0,
                false,
                createdAt
        );

        assertEquals("auto-1", autoBid.getId());
        assertEquals("bidder-1", autoBid.getUserId());
        assertEquals("auction-1", autoBid.getAuctionId());
        assertEquals(5000.0, autoBid.getMaxBid());
        assertEquals(200.0, autoBid.getIncrement());
        assertFalse(autoBid.isActive());
        assertSame(createdAt, autoBid.getCreatedAt());
    }

    // Setter phải cập nhật đúng dữ liệu
    @Test
    void settersShouldUpdateFields() {
        AutoBid autoBid = new AutoBid(
                "bidder-1",
                "auction-1",
                5000.0,
                200.0
        );

        LocalDateTime newCreatedAt = LocalDateTime.now().minusDays(1);

        autoBid.setUserId("bidder-2");
        autoBid.setAuctionId("auction-2");
        autoBid.setMaxBid(10000.0);
        autoBid.setIncrement(500.0);
        autoBid.setActive(false);
        autoBid.setCreatedAt(newCreatedAt);

        assertEquals("bidder-2", autoBid.getUserId());
        assertEquals("auction-2", autoBid.getAuctionId());
        assertEquals(10000.0, autoBid.getMaxBid());
        assertEquals(500.0, autoBid.getIncrement());
        assertFalse(autoBid.isActive());
        assertSame(newCreatedAt, autoBid.getCreatedAt());
    }

    // canCover true nếu active và maxBid >= minimumBidRequired
    @Test
    void canCoverShouldReturnTrueWhenActiveAndMaxBidEnough() {
        AutoBid autoBid = new AutoBid(
                "bidder-1",
                "auction-1",
                5000.0,
                200.0
        );

        assertTrue(autoBid.canCover(5000.0));
        assertTrue(autoBid.canCover(4000.0));
    }

    // canCover false nếu maxBid nhỏ hơn minimumBidRequired
    @Test
    void canCoverShouldReturnFalseWhenMaxBidNotEnough() {
        AutoBid autoBid = new AutoBid(
                "bidder-1",
                "auction-1",
                5000.0,
                200.0
        );

        assertFalse(autoBid.canCover(5000.1));
        assertFalse(autoBid.canCover(6000.0));
    }

    // canCover false nếu AutoBid inactive dù maxBid đủ
    @Test
    void canCoverShouldReturnFalseWhenInactive() {
        AutoBid autoBid = new AutoBid(
                "bidder-1",
                "auction-1",
                5000.0,
                200.0
        );

        autoBid.setActive(false);

        assertFalse(autoBid.canCover(4000.0));
        assertFalse(autoBid.canCover(5000.0));
    }

    // calculateNextBidAmount lấy currentPrice + increment nếu đủ lớn và chưa vượt maxBid
    @Test
    void calculateNextBidAmountShouldUseCurrentPricePlusIncrementWhenValid() {
        AutoBid autoBid = new AutoBid(
                "bidder-1",
                "auction-1",
                5000.0,
                200.0
        );

        double result = autoBid.calculateNextBidAmount(
                1000.0,
                1100.0
        );

        assertEquals(1200.0, result);
    }

    // calculateNextBidAmount phải ít nhất bằng minimumBidRequired
    @Test
    void calculateNextBidAmountShouldRespectMinimumBidRequired() {
        AutoBid autoBid = new AutoBid(
                "bidder-1",
                "auction-1",
                5000.0,
                100.0
        );

        double result = autoBid.calculateNextBidAmount(
                1000.0,
                1500.0
        );

        assertEquals(1500.0, result);
    }

    // calculateNextBidAmount không được vượt maxBid
    @Test
    void calculateNextBidAmountShouldNotExceedMaxBid() {
        AutoBid autoBid = new AutoBid(
                "bidder-1",
                "auction-1",
                1300.0,
                500.0
        );

        double result = autoBid.calculateNextBidAmount(
                1000.0,
                1100.0
        );

        assertEquals(1300.0, result);
    }

    // calculateNextBidAmount nếu minimumBidRequired lớn hơn maxBid thì trả maxBid
    @Test
    void calculateNextBidAmountShouldReturnMaxBidWhenMinimumBidIsHigherThanMaxBid() {
        AutoBid autoBid = new AutoBid(
                "bidder-1",
                "auction-1",
                1300.0,
                500.0
        );

        double result = autoBid.calculateNextBidAmount(
                1000.0,
                1500.0
        );

        assertEquals(1300.0, result);
    }

    // increment bằng 0 vẫn tính theo minimumBidRequired nếu minimum cao hơn currentPrice
    @Test
    void calculateNextBidAmountShouldWorkWhenIncrementIsZero() {
        AutoBid autoBid = new AutoBid(
                "bidder-1",
                "auction-1",
                5000.0,
                0.0
        );

        double result = autoBid.calculateNextBidAmount(
                1000.0,
                1200.0
        );

        assertEquals(1200.0, result);
    }

    // increment âm hiện tại không bị chặn ở model, minimumBidRequired vẫn bảo vệ giá tối thiểu
    @Test
    void calculateNextBidAmountShouldUseMinimumBidWhenIncrementIsNegative() {
        AutoBid autoBid = new AutoBid(
                "bidder-1",
                "auction-1",
                5000.0,
                -100.0
        );

        double result = autoBid.calculateNextBidAmount(
                1000.0,
                1200.0
        );

        assertEquals(1200.0, result);
    }

    // userId null hiện tại vẫn được lưu, validate nằm ở service/controller
    @Test
    void constructorShouldAllowNullUserId() {
        AutoBid autoBid = new AutoBid(
                null,
                "auction-1",
                5000.0,
                200.0
        );

        assertNull(autoBid.getUserId());
        assertEquals("auction-1", autoBid.getAuctionId());
    }

    // auctionId null hiện tại vẫn được lưu, validate nằm ở service/controller
    @Test
    void constructorShouldAllowNullAuctionId() {
        AutoBid autoBid = new AutoBid(
                "bidder-1",
                null,
                5000.0,
                200.0
        );

        assertEquals("bidder-1", autoBid.getUserId());
        assertNull(autoBid.getAuctionId());
    }
}