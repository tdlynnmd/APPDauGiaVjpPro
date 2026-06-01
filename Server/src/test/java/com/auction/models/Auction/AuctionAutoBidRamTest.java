package com.auction.models.Auction;

import com.auction.models.Entity.Entity;
import com.auction.models.Item.Electronics;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.*;

class AuctionAutoBidRamTest {

    // Set id cho Entity vì constructor tạo mới tự sinh id random
    private void setEntityId(Entity entity, String id) throws Exception {
        Field idField = Entity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    // Tạo item mẫu
    private Electronics sampleItem(String itemId, String sellerId) throws Exception {
        Electronics item = new Electronics(
                "Laptop Dell",
                1000.0,
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

    // Tạo auction mẫu
    private Auction sampleAuction() throws Exception {
        Electronics item = sampleItem("item-1", "seller-1");

        Auction auction = new Auction(
                item,
                "seller-1",
                100.0,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusHours(1)
        );

        setEntityId(auction, "auction-1");
        return auction;
    }

    // Tạo AutoBid mẫu
    private AutoBid autoBid(String id, String userId, String auctionId,
                            double maxBid, double increment,
                            boolean active, LocalDateTime createdAt) {
        return new AutoBid(
                id,
                userId,
                auctionId,
                maxBid,
                increment,
                active,
                createdAt
        );
    }

    // getAutoBidsQueue ban đầu phải trả queue không null và rỗng
    @Test
    void getAutoBidsQueueShouldReturnEmptyQueueInitially() throws Exception {
        Auction auction = sampleAuction();

        PriorityQueue<AutoBid> queue = auction.getAutoBidsQueue();

        assertNotNull(queue);
        assertTrue(queue.isEmpty());
    }

    // addOrUpdateAutoBidInRam phải thêm auto bid active vào queue
    @Test
    void addOrUpdateAutoBidInRamShouldAddActiveAutoBid() throws Exception {
        Auction auction = sampleAuction();

        AutoBid autoBid = autoBid(
                "auto-1",
                "bidder-1",
                "auction-1",
                5000.0,
                200.0,
                true,
                LocalDateTime.now()
        );

        auction.addOrUpdateAutoBidInRam(autoBid);

        assertEquals(1, auction.getAutoBidsQueue().size());
        assertSame(autoBid, auction.getAutoBidsQueue().peek());
    }

    // addOrUpdateAutoBidInRam không thêm auto bid inactive
    @Test
    void addOrUpdateAutoBidInRamShouldNotAddInactiveAutoBid() throws Exception {
        Auction auction = sampleAuction();

        AutoBid autoBid = autoBid(
                "auto-1",
                "bidder-1",
                "auction-1",
                5000.0,
                200.0,
                false,
                LocalDateTime.now()
        );

        auction.addOrUpdateAutoBidInRam(autoBid);

        assertTrue(auction.getAutoBidsQueue().isEmpty());
    }

    // addOrUpdateAutoBidInRam cùng userId phải replace auto bid cũ
    @Test
    void addOrUpdateAutoBidInRamShouldReplaceAutoBidOfSameUser() throws Exception {
        Auction auction = sampleAuction();

        AutoBid oldAutoBid = autoBid(
                "auto-old",
                "bidder-1",
                "auction-1",
                3000.0,
                100.0,
                true,
                LocalDateTime.now().minusMinutes(5)
        );

        AutoBid newAutoBid = autoBid(
                "auto-new",
                "bidder-1",
                "auction-1",
                6000.0,
                200.0,
                true,
                LocalDateTime.now()
        );

        auction.addOrUpdateAutoBidInRam(oldAutoBid);
        auction.addOrUpdateAutoBidInRam(newAutoBid);

        assertEquals(1, auction.getAutoBidsQueue().size());

        AutoBid saved = auction.getAutoBidsQueue().peek();

        assertSame(newAutoBid, saved);
        assertEquals("auto-new", saved.getId());
        assertEquals(6000.0, saved.getMaxBid());
    }

    // addOrUpdateAutoBidInRam inactive cùng userId phải xóa auto bid cũ
    @Test
    void addOrUpdateAutoBidInRamShouldRemoveOldAutoBidWhenNewOneIsInactive() throws Exception {
        Auction auction = sampleAuction();

        AutoBid oldAutoBid = autoBid(
                "auto-old",
                "bidder-1",
                "auction-1",
                3000.0,
                100.0,
                true,
                LocalDateTime.now().minusMinutes(5)
        );

        AutoBid inactiveAutoBid = autoBid(
                "auto-inactive",
                "bidder-1",
                "auction-1",
                6000.0,
                200.0,
                false,
                LocalDateTime.now()
        );

        auction.addOrUpdateAutoBidInRam(oldAutoBid);
        auction.addOrUpdateAutoBidInRam(inactiveAutoBid);

        assertTrue(auction.getAutoBidsQueue().isEmpty());
    }

    // removeAutoBidInRam phải xóa đúng userId
    @Test
    void removeAutoBidInRamShouldRemoveAutoBidByUserId() throws Exception {
        Auction auction = sampleAuction();

        AutoBid autoBid1 = autoBid(
                "auto-1",
                "bidder-1",
                "auction-1",
                5000.0,
                200.0,
                true,
                LocalDateTime.now()
        );

        AutoBid autoBid2 = autoBid(
                "auto-2",
                "bidder-2",
                "auction-1",
                6000.0,
                200.0,
                true,
                LocalDateTime.now()
        );

        auction.addOrUpdateAutoBidInRam(autoBid1);
        auction.addOrUpdateAutoBidInRam(autoBid2);

        auction.removeAutoBidInRam("bidder-1");

        assertEquals(1, auction.getAutoBidsQueue().size());
        assertEquals("bidder-2", auction.getAutoBidsQueue().peek().getUserId());
    }

    // removeAutoBidInRam userId không tồn tại thì không ảnh hưởng queue
    @Test
    void removeAutoBidInRamShouldDoNothingWhenUserIdDoesNotExist() throws Exception {
        Auction auction = sampleAuction();

        AutoBid autoBid1 = autoBid(
                "auto-1",
                "bidder-1",
                "auction-1",
                5000.0,
                200.0,
                true,
                LocalDateTime.now()
        );

        auction.addOrUpdateAutoBidInRam(autoBid1);
        auction.removeAutoBidInRam("missing-user");

        assertEquals(1, auction.getAutoBidsQueue().size());
        assertSame(autoBid1, auction.getAutoBidsQueue().peek());
    }

    // PriorityQueue phải ưu tiên maxBid cao hơn
    @Test
    void autoBidPriorityShouldPreferHigherMaxBid() throws Exception {
        Auction auction = sampleAuction();
        LocalDateTime now = LocalDateTime.now();

        AutoBid low = autoBid(
                "auto-low",
                "bidder-low",
                "auction-1",
                3000.0,
                100.0,
                true,
                now.minusSeconds(1)
        );

        AutoBid high = autoBid(
                "auto-high",
                "bidder-high",
                "auction-1",
                6000.0,
                100.0,
                true,
                now
        );

        auction.addOrUpdateAutoBidInRam(low);
        auction.addOrUpdateAutoBidInRam(high);

        assertEquals("bidder-high", auction.getAutoBidsQueue().peek().getUserId());
    }

    // Nếu maxBid bằng nhau thì createdAt sớm hơn được ưu tiên
    @Test
    void autoBidPriorityShouldPreferEarlierCreatedAtWhenMaxBidEqual() throws Exception {
        Auction auction = sampleAuction();

        AutoBid early = autoBid(
                "auto-early",
                "bidder-early",
                "auction-1",
                5000.0,
                100.0,
                true,
                LocalDateTime.now().minusMinutes(10)
        );

        AutoBid late = autoBid(
                "auto-late",
                "bidder-late",
                "auction-1",
                5000.0,
                100.0,
                true,
                LocalDateTime.now()
        );

        auction.addOrUpdateAutoBidInRam(late);
        auction.addOrUpdateAutoBidInRam(early);

        assertEquals("bidder-early", auction.getAutoBidsQueue().peek().getUserId());
    }

    // Queue poll ra phải theo thứ tự maxBid giảm dần
    @Test
    void autoBidQueueShouldPollByPriorityOrder() throws Exception {
        Auction auction = sampleAuction();
        LocalDateTime now = LocalDateTime.now();

        AutoBid low = autoBid(
                "auto-low",
                "bidder-low",
                "auction-1",
                3000.0,
                100.0,
                true,
                now
        );

        AutoBid medium = autoBid(
                "auto-medium",
                "bidder-medium",
                "auction-1",
                5000.0,
                100.0,
                true,
                now
        );

        AutoBid high = autoBid(
                "auto-high",
                "bidder-high",
                "auction-1",
                7000.0,
                100.0,
                true,
                now
        );

        auction.addOrUpdateAutoBidInRam(low);
        auction.addOrUpdateAutoBidInRam(high);
        auction.addOrUpdateAutoBidInRam(medium);

        PriorityQueue<AutoBid> queue = auction.getAutoBidsQueue();

        assertEquals("bidder-high", queue.poll().getUserId());
        assertEquals("bidder-medium", queue.poll().getUserId());
        assertEquals("bidder-low", queue.poll().getUserId());
        assertTrue(queue.isEmpty());
    }

    // addOrUpdateAutoBidInRam autoBid null hiện tại sẽ NullPointerException
    @Test
    void addOrUpdateAutoBidInRamShouldThrowWhenAutoBidIsNull() throws Exception {
        Auction auction = sampleAuction();

        assertThrows(NullPointerException.class, () -> {
            auction.addOrUpdateAutoBidInRam(null);
        });
    }

    // addOrUpdateAutoBidInRam userId null hiện tại vẫn được thêm vào queue nếu autoBid active
    @Test
    void addOrUpdateAutoBidInRamShouldAllowNullUserIdWhenExistingUserIdIsNotNull() throws Exception {
        Auction auction = sampleAuction();

        AutoBid existing = autoBid(
                "auto-existing",
                "bidder-1",
                "auction-1",
                3000.0,
                100.0,
                true,
                LocalDateTime.now()
        );

        AutoBid nullUserAutoBid = autoBid(
                "auto-null-user",
                null,
                "auction-1",
                5000.0,
                100.0,
                true,
                LocalDateTime.now()
        );

        auction.addOrUpdateAutoBidInRam(existing);

        assertDoesNotThrow(() -> {
            auction.addOrUpdateAutoBidInRam(nullUserAutoBid);
        });

        assertEquals(2, auction.getAutoBidsQueue().size());

        // AutoBid userId null có maxBid cao hơn nên đang đứng đầu queue
        assertNull(auction.getAutoBidsQueue().peek().getUserId());
        assertEquals(5000.0, auction.getAutoBidsQueue().peek().getMaxBid());
    }

    // removeAutoBidInRam null hiện tại không xóa gì nếu autoBid userId khác null
    @Test
    void removeAutoBidInRamShouldNotRemoveWhenUserIdIsNull() throws Exception {
        Auction auction = sampleAuction();

        AutoBid existing = autoBid(
                "auto-existing",
                "bidder-1",
                "auction-1",
                3000.0,
                100.0,
                true,
                LocalDateTime.now()
        );

        auction.addOrUpdateAutoBidInRam(existing);
        auction.removeAutoBidInRam(null);

        assertEquals(1, auction.getAutoBidsQueue().size());
        assertEquals("bidder-1", auction.getAutoBidsQueue().peek().getUserId());
    }
}