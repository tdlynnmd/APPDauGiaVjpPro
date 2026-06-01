package com.auction.models.Auction;

import com.auction.enums.AuctionStatus;
import com.auction.enums.BidStatus;
import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;
import com.auction.exception.AuctionErrorCode;
import com.auction.exception.AuctionException;
import com.auction.models.Entity.Entity;
import com.auction.models.Item.Electronics;
import com.auction.models.User.Bidder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuctionCoreTest {

    // Set id cho Entity vì constructor tạo mới tự sinh id random
    private void setEntityId(Entity entity, String id) throws Exception {
        Field idField = Entity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    // Tạo item mẫu
    private Electronics sampleItem(String itemId, String sellerId, double startingPrice) throws Exception {
        Electronics item = new Electronics(
                "Laptop Dell",
                startingPrice,
                "Laptop văn phòng",
                2022,
                sellerId,
                "image.png",
                "Dell",
                24
        );

        setEntityId(item, itemId);
        return item;
    }

    // Tạo bidder mẫu bằng constructor DB để set được id rõ ràng
    private Bidder sampleBidder(String bidderId, String username) {
        return new Bidder(
                bidderId,
                username,
                username + "@example.com",
                "Password@123",
                UserRole.BIDDER,
                10000.0,
                0.0,
                UserStatus.ACTIVE,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now()
        );
    }

    // Tạo auction đang RUNNING
    private Auction runningAuction() throws Exception {
        Electronics item = sampleItem("item-1", "seller-1", 1000.0);

        Auction auction = new Auction(
                item,
                "seller-1",
                100.0,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusHours(1)
        );

        setEntityId(auction, "auction-1");
        auction.refreshStatus(LocalDateTime.now());

        return auction;
    }

    // Tạo auction OPEN
    private Auction openAuction() throws Exception {
        Electronics item = sampleItem("item-1", "seller-1", 1000.0);

        Auction auction = new Auction(
                item,
                "seller-1",
                100.0,
                LocalDateTime.now().plusMinutes(10),
                LocalDateTime.now().plusHours(1)
        );

        setEntityId(auction, "auction-1");
        auction.refreshStatus(LocalDateTime.now());

        return auction;
    }

    // Tạo auction sắp kết thúc để test anti-sniping
    private Auction almostEndingAuction() throws Exception {
        Electronics item = sampleItem("item-1", "seller-1", 1000.0);

        Auction auction = new Auction(
                item,
                "seller-1",
                100.0,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusSeconds(10)
        );

        setEntityId(auction, "auction-1");
        auction.refreshStatus(LocalDateTime.now());

        return auction;
    }

    // Check đúng mã lỗi AuctionException
    private void assertAuctionError(AuctionException exception, AuctionErrorCode expectedError) {
        assertEquals(expectedError.getCode(), exception.getErrorCode());
    }

    // =========================================================
    // constructor / status
    // =========================================================

    // Constructor tạo mới phải lưu đúng dữ liệu cơ bản
    @Test
    void constructorShouldInitializeAuctionCorrectly() throws Exception {
        Electronics item = sampleItem("item-1", "seller-1", 1000.0);

        LocalDateTime start = LocalDateTime.now().plusMinutes(10);
        LocalDateTime end = start.plusHours(1);

        Auction auction = new Auction(
                item,
                "seller-1",
                100.0,
                start,
                end
        );

        setEntityId(auction, "auction-1");

        assertEquals("auction-1", auction.getId());
        assertEquals("item-1", auction.getItemId());
        assertEquals("seller-1", auction.getSellerId());
        assertEquals(1000.0, auction.getCurrentPrice());
        assertEquals(100.0, auction.getStepPrice());
        assertEquals(100.0, auction.getLiveStepPrice());
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
        assertNull(auction.getHighestBidderId());
        assertNull(auction.getCurrentWinningBidId());
        assertSame(item, auction.getItem());
    }

    // refreshStatus trước startTime thì OPEN
    @Test
    void refreshStatusShouldSetOpenBeforeStartTime() throws Exception {
        Auction auction = openAuction();

        assertEquals(AuctionStatus.OPEN, auction.getStatus());
    }

    // refreshStatus trong khoảng start-end thì RUNNING
    @Test
    void refreshStatusShouldSetRunningBetweenStartAndEndTime() throws Exception {
        Auction auction = runningAuction();

        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }

    // refreshStatus sau endTime thì FINISHED
    @Test
    void refreshStatusShouldSetFinishedAfterEndTime() throws Exception {
        Electronics item = sampleItem("item-1", "seller-1", 1000.0);

        Auction auction = new Auction(
                item,
                "seller-1",
                100.0,
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1)
        );

        auction.refreshStatus(LocalDateTime.now());

        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
    }

    // refreshStatus không được đổi CANCELED
    @Test
    void refreshStatusShouldNotChangeCanceledAuction() throws Exception {
        Auction auction = runningAuction();

        auction.setStatus(AuctionStatus.CANCELED);
        auction.refreshStatus(LocalDateTime.now());

        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
    }

    // refreshStatus không được đổi FINISHED
    @Test
    void refreshStatusShouldNotChangeFinishedAuction() throws Exception {
        Auction auction = runningAuction();

        auction.setStatus(AuctionStatus.FINISHED);
        auction.refreshStatus(LocalDateTime.now());

        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
    }

    // =========================================================
    // placeBid success
    // =========================================================

    // placeBid hợp lệ phải cập nhật giá hiện tại, highestBidderId và currentWinningBidId
    @Test
    void placeBidShouldUpdateAuctionStateWhenValid() throws Exception {
        Auction auction = runningAuction();
        Bidder bidder = sampleBidder("bidder-1", "bidder01");

        BidTransaction bid = auction.placeBid(
                bidder,
                1100.0,
                "bid-1"
        );

        assertNotNull(bid);
        assertEquals("bid-1", bid.getId());
        assertEquals("bidder-1", bid.getBidderId());
        assertEquals("auction-1", bid.getAuctionId());
        assertEquals(1100.0, bid.getAmount());
        assertEquals(BidStatus.ACCEPTED, bid.getStatus());

        assertEquals(1100.0, auction.getCurrentPrice());
        assertEquals("bidder-1", auction.getHighestBidderId());
        assertEquals("bid-1", auction.getCurrentWinningBidId());
        assertSame(bidder, auction.getHighestBidder());
    }

    // Bid đúng bằng currentPrice + liveStepPrice phải được chấp nhận
    @Test
    void placeBidShouldAllowBidEqualToMinimumRequired() throws Exception {
        Auction auction = runningAuction();
        Bidder bidder = sampleBidder("bidder-1", "bidder01");

        BidTransaction bid = auction.placeBid(
                bidder,
                1100.0,
                "bid-1"
        );

        assertEquals(1100.0, bid.getAmount());
        assertEquals(1100.0, auction.getCurrentPrice());
    }

    // Bid cao hơn minimumRequired phải được chấp nhận
    @Test
    void placeBidShouldAllowBidHigherThanMinimumRequired() throws Exception {
        Auction auction = runningAuction();
        Bidder bidder = sampleBidder("bidder-1", "bidder01");

        BidTransaction bid = auction.placeBid(
                bidder,
                2000.0,
                "bid-1"
        );

        assertEquals(2000.0, bid.getAmount());
        assertEquals(2000.0, auction.getCurrentPrice());
    }

    // =========================================================
    // placeBid validation
    // =========================================================

    // placeBid khi auction OPEN phải ném AUCTION_NOT_RUNNING
    @Test
    void placeBidShouldThrowWhenAuctionIsOpen() throws Exception {
        Auction auction = openAuction();
        Bidder bidder = sampleBidder("bidder-1", "bidder01");

        AuctionException exception = assertThrows(AuctionException.class, () -> {
            auction.placeBid(bidder, 1100.0, "bid-1");
        });

        assertAuctionError(exception, AuctionErrorCode.AUCTION_NOT_RUNNING);
    }

    // placeBid khi auction FINISHED phải ném AUCTION_NOT_RUNNING
    @Test
    void placeBidShouldThrowWhenAuctionIsFinished() throws Exception {
        Auction auction = runningAuction();
        auction.setStatus(AuctionStatus.FINISHED);

        Bidder bidder = sampleBidder("bidder-1", "bidder01");

        AuctionException exception = assertThrows(AuctionException.class, () -> {
            auction.placeBid(bidder, 1100.0, "bid-1");
        });

        assertAuctionError(exception, AuctionErrorCode.AUCTION_NOT_RUNNING);
    }

    // placeBid bidder null phải ném ITEM_NOT_FOUND theo code hiện tại
    @Test
    void placeBidShouldThrowWhenBidderIsNull() throws Exception {
        Auction auction = runningAuction();

        AuctionException exception = assertThrows(AuctionException.class, () -> {
            auction.placeBid(null, 1100.0, "bid-1");
        });

        assertAuctionError(exception, AuctionErrorCode.ITEM_NOT_FOUND);
    }

    // placeBid bidder có id null phải ném ITEM_NOT_FOUND theo code hiện tại
    @Test
    void placeBidShouldThrowWhenBidderIdIsNull() throws Exception {
        Auction auction = runningAuction();

        Bidder bidder = new Bidder(
                null,
                "bidder01",
                "bidder01@example.com",
                "Password@123",
                UserRole.BIDDER,
                10000.0,
                0.0,
                UserStatus.ACTIVE,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now()
        );

        AuctionException exception = assertThrows(AuctionException.class, () -> {
            auction.placeBid(bidder, 1100.0, "bid-1");
        });

        assertAuctionError(exception, AuctionErrorCode.ITEM_NOT_FOUND);
    }

    // Seller không được bid auction của chính mình
    @Test
    void placeBidShouldThrowWhenBidderIsSeller() throws Exception {
        Auction auction = runningAuction();
        Bidder sellerAsBidder = sampleBidder("seller-1", "seller01");

        AuctionException exception = assertThrows(AuctionException.class, () -> {
            auction.placeBid(sellerAsBidder, 1100.0, "bid-1");
        });

        assertAuctionError(exception, AuctionErrorCode.BIDDER_IS_SELLER);
    }

    // Bid thấp hơn currentPrice + liveStepPrice phải ném BID_AMOUNT_TOO_LOW
    @Test
    void placeBidShouldThrowWhenAmountTooLow() throws Exception {
        Auction auction = runningAuction();
        Bidder bidder = sampleBidder("bidder-1", "bidder01");

        AuctionException exception = assertThrows(AuctionException.class, () -> {
            auction.placeBid(bidder, 1099.0, "bid-1");
        });

        assertAuctionError(exception, AuctionErrorCode.BID_AMOUNT_TOO_LOW);
    }

    // =========================================================
    // anti-sniping
    // =========================================================

    // Bid trong 30 giây cuối phải gia hạn endTime
    @Test
    void placeBidNearEndTimeShouldExtendAuction() throws Exception {
        Auction auction = almostEndingAuction();
        Bidder bidder = sampleBidder("bidder-1", "bidder01");

        LocalDateTime oldEndTime = auction.getEndTime();

        auction.placeBid(bidder, 1100.0, "bid-1");

        assertTrue(auction.getEndTime().isAfter(oldEndTime));
        assertEquals(100.0, auction.getLiveStepPrice());
    }

    // Bid không gần cuối giờ thì không gia hạn endTime
    @Test
    void placeBidFarFromEndTimeShouldNotExtendAuction() throws Exception {
        Auction auction = runningAuction();
        Bidder bidder = sampleBidder("bidder-1", "bidder01");

        LocalDateTime oldEndTime = auction.getEndTime();

        auction.placeBid(bidder, 1100.0, "bid-1");

        assertEquals(oldEndTime, auction.getEndTime());
        assertEquals(100.0, auction.getLiveStepPrice());
    }

    // Ép endTime về gần hiện tại để mỗi lần bid đều kích hoạt anti-sniping
    private void forceEndTimeNearNow(Auction auction) throws Exception {
        Field endTimeField = Auction.class.getDeclaredField("endTime");
        endTimeField.setAccessible(true);
        endTimeField.set(auction, LocalDateTime.now().plusSeconds(10));
    }

    // Sau hơn 3 lần gia hạn thật sự, liveStepPrice phải tăng để chống spam
    @Test
    void repeatedLastSecondBidsShouldIncreaseLiveStepPriceAfterThirdExtension() throws Exception {
        Auction auction = almostEndingAuction();

        Bidder bidder1 = sampleBidder("bidder-1", "bidder01");
        Bidder bidder2 = sampleBidder("bidder-2", "bidder02");
        Bidder bidder3 = sampleBidder("bidder-3", "bidder03");
        Bidder bidder4 = sampleBidder("bidder-4", "bidder04");

        forceEndTimeNearNow(auction);
        auction.placeBid(bidder1, 1100.0, "bid-1");

        forceEndTimeNearNow(auction);
        auction.placeBid(bidder2, 1200.0, "bid-2");

        forceEndTimeNearNow(auction);
        auction.placeBid(bidder3, 1300.0, "bid-3");

        forceEndTimeNearNow(auction);
        auction.placeBid(bidder4, 1400.0, "bid-4");

        assertTrue(auction.getLiveStepPrice() > 100.0);
    }

    // =========================================================
    // rollback
    // =========================================================

    // rollbackBidInRam phải khôi phục giá, highestBidderId, endTime và clear currentWinningBidId
    @Test
    void rollbackBidInRamShouldRestoreOldState() throws Exception {
        Auction auction = runningAuction();
        Bidder bidder = sampleBidder("bidder-1", "bidder01");

        LocalDateTime oldEndTime = auction.getEndTime();

        auction.placeBid(bidder, 1100.0, "bid-1");

        assertEquals(1100.0, auction.getCurrentPrice());
        assertEquals("bidder-1", auction.getHighestBidderId());
        assertEquals("bid-1", auction.getCurrentWinningBidId());

        auction.rollbackBidInRam(
                null,
                1000.0,
                oldEndTime
        );

        assertEquals(1000.0, auction.getCurrentPrice());
        assertNull(auction.getHighestBidderId());
        assertNull(auction.getHighestBidder());
        assertNull(auction.getCurrentWinningBidId());
        assertEquals(oldEndTime, auction.getEndTime());
    }

    // rollbackBidInRam với oldEndTime null thì không đổi endTime hiện tại
    @Test
    void rollbackBidInRamShouldKeepCurrentEndTimeWhenOldEndTimeIsNull() throws Exception {
        Auction auction = runningAuction();
        Bidder bidder = sampleBidder("bidder-1", "bidder01");

        auction.placeBid(bidder, 1100.0, "bid-1");

        LocalDateTime endTimeAfterBid = auction.getEndTime();

        auction.rollbackBidInRam(
                "old-bidder",
                1000.0,
                null
        );

        assertEquals(1000.0, auction.getCurrentPrice());
        assertEquals("old-bidder", auction.getHighestBidderId());
        assertEquals(endTimeAfterBid, auction.getEndTime());
        assertNull(auction.getCurrentWinningBidId());
    }

    // =========================================================
    // hydration constructor
    // =========================================================

    // Constructor load DB phải giữ đúng dữ liệu truyền vào
    @Test
    void hydrationConstructorShouldStoreFields() {
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();

        Auction auction = new Auction(
                "auction-1",
                "item-1",
                "seller-1",
                "bidder-1",
                "bid-1",
                2000.0,
                100.0,
                start,
                end,
                createdAt,
                updatedAt,
                AuctionStatus.RUNNING
        );

        assertEquals("auction-1", auction.getId());
        assertEquals("item-1", auction.getItemId());
        assertEquals("seller-1", auction.getSellerId());
        assertEquals("bidder-1", auction.getHighestBidderId());
        assertEquals("bid-1", auction.getCurrentWinningBidId());
        assertEquals(2000.0, auction.getCurrentPrice());
        assertEquals(100.0, auction.getStepPrice());
        assertEquals(100.0, auction.getLiveStepPrice());
        assertEquals(start, auction.getStartTime());
        assertEquals(end, auction.getEndTime());
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }
}