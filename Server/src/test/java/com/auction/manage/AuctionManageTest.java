package com.auction.manage;

import com.auction.enums.AuctionStatus;
import com.auction.models.Auction.Auction;
import com.auction.models.Item.Electronics;
import com.auction.models.Item.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuctionManageTest {

    private AuctionManage auctionManage;

    @BeforeEach
    void setUp() throws Exception {
        auctionManage = AuctionManage.getInstance();
        clearAuctionManage();
    }

    // Clear singleton RAM để test không bị ảnh hưởng bởi test trước
    private void clearAuctionManage() throws Exception {
        clearMapField("activeAuctions");
        clearMapField("lastAccessedTime");
    }

    // Clear map private bằng reflection
    private void clearMapField(String fieldName) throws Exception {
        Field field = AuctionManage.class.getDeclaredField(fieldName);
        field.setAccessible(true);

        Map<?, ?> map = (Map<?, ?>) field.get(auctionManage);
        map.clear();
    }

    // Lấy map lastAccessedTime để kiểm tra cache time
    @SuppressWarnings("unchecked")
    private Map<String, LocalDateTime> getLastAccessedTimeMap() throws Exception {
        Field field = AuctionManage.class.getDeclaredField("lastAccessedTime");
        field.setAccessible(true);

        return (Map<String, LocalDateTime>) field.get(auctionManage);
    }

    // Tạo item mẫu cho auction
    private Item sampleItem(String itemId) {
        Electronics item = new Electronics(
                "Laptop Dell",
                12000000,
                "Laptop văn phòng",
                2022,
                "seller-1",
                "dell.png",
                "Dell",
                24
        );

        item.setId(itemId);
        return item;
    }

    // Tạo auction mẫu với id cố định
    private Auction sampleAuction(String auctionId) {
        Auction auction = new Auction(
                sampleItem("item-" + auctionId),
                "seller-1",
                100000,
                LocalDateTime.now().plusMinutes(5),
                LocalDateTime.now().plusHours(1)
        );

        auction.setId(auctionId);
        return auction;
    }

    // Tạo auction với thời gian tùy chỉnh để ép status
    private Auction sampleAuctionWithTime(String auctionId, LocalDateTime startTime, LocalDateTime endTime) {
        Auction auction = new Auction(
                sampleItem("item-" + auctionId),
                "seller-1",
                100000,
                startTime,
                endTime
        );

        auction.setId(auctionId);
        return auction;
    }

    // =========================================================
    // NHÓM A - TEST CHỨC NĂNG CƠ BẢN
    // =========================================================

    // Singleton phải luôn trả về cùng một instance
    @Test
    void getInstanceShouldReturnSameObject() {
        AuctionManage first = AuctionManage.getInstance();
        AuctionManage second = AuctionManage.getInstance();

        assertSame(first, second);
    }

    // addAuction phải lưu auction vào RAM
    @Test
    void addAuctionShouldStoreAuctionInMemory() {
        Auction auction = sampleAuction("auction-1");

        auctionManage.addAuction(auction);

        assertSame(auction, auctionManage.getAuctionById("auction-1"));
        assertEquals(1, auctionManage.getAllActive().size());
    }

    // addAuction phải tạo lastAccessedTime cho auction
    @Test
    void addAuctionShouldCreateLastAccessedTime() throws Exception {
        Auction auction = sampleAuction("auction-1");

        auctionManage.addAuction(auction);

        Map<String, LocalDateTime> lastAccessedTime = getLastAccessedTimeMap();

        assertTrue(lastAccessedTime.containsKey("auction-1"));
        assertNotNull(lastAccessedTime.get("auction-1"));
    }

    // getAuctionById phải lấy đúng auction theo id
    @Test
    void getAuctionByIdShouldReturnAuctionWhenExists() {
        Auction auction = sampleAuction("auction-1");
        auctionManage.addAuction(auction);

        Auction result = auctionManage.getAuctionById("auction-1");

        assertSame(auction, result);
    }

    // getAuctionById phải trả null khi không có auction
    @Test
    void getAuctionByIdShouldReturnNullWhenNotFound() {
        assertNull(auctionManage.getAuctionById("missing-auction"));
    }

    // getAuctionById phải cập nhật lastAccessedTime khi auction tồn tại
    @Test
    void getAuctionByIdShouldUpdateLastAccessedTime() throws Exception {
        Auction auction = sampleAuction("auction-1");
        auctionManage.addAuction(auction);

        Map<String, LocalDateTime> lastAccessedTime = getLastAccessedTimeMap();

        LocalDateTime oldTime = LocalDateTime.now().minusHours(1);
        lastAccessedTime.put("auction-1", oldTime);

        auctionManage.getAuctionById("auction-1");

        assertTrue(lastAccessedTime.get("auction-1").isAfter(oldTime));
    }

    // getAllActive phải trả tất cả auction đang nằm trong RAM
    @Test
    void getAllActiveShouldReturnAllAuctionsInMemory() {
        Auction auction1 = sampleAuction("auction-1");
        Auction auction2 = sampleAuction("auction-2");

        auctionManage.addAuction(auction1);
        auctionManage.addAuction(auction2);

        List<Auction> result = auctionManage.getAllActive();

        assertEquals(2, result.size());
        assertTrue(result.contains(auction1));
        assertTrue(result.contains(auction2));
    }

    // removeAuctionById phải xóa auction khỏi RAM
    @Test
    void removeAuctionByIdShouldRemoveAuctionFromMemory() {
        Auction auction = sampleAuction("auction-1");
        auctionManage.addAuction(auction);

        auctionManage.removeAuctionById("auction-1");

        assertNull(auctionManage.getAuctionById("auction-1"));
        assertTrue(auctionManage.getAllActive().isEmpty());
    }

    // removeAuctionById phải xóa cả lastAccessedTime
    @Test
    void removeAuctionByIdShouldRemoveLastAccessedTime() throws Exception {
        Auction auction = sampleAuction("auction-1");
        auctionManage.addAuction(auction);

        auctionManage.removeAuctionById("auction-1");

        Map<String, LocalDateTime> lastAccessedTime = getLastAccessedTimeMap();

        assertFalse(lastAccessedTime.containsKey("auction-1"));
    }

    // =========================================================
    // NHÓM B - TEST CASE CÓ THỂ GÂY LỖI
    // =========================================================

    // addAuction(null) hiện tại sẽ lỗi, test này bắt điểm yếu null-safety
    @Test
    void addAuctionShouldThrowWhenAuctionIsNull() {
        assertThrows(NullPointerException.class, () -> {
            auctionManage.addAuction(null);
        });
    }

    // auction có id null cũng gây lỗi vì ConcurrentHashMap không nhận null key
    @Test
    void addAuctionShouldThrowWhenAuctionIdIsNull() {
        Auction auction = sampleAuction("auction-null-id");
        auction.setId(null);

        assertThrows(NullPointerException.class, () -> {
            auctionManage.addAuction(auction);
        });
    }

    // getAuctionById(null) hiện tại sẽ lỗi vì ConcurrentHashMap không nhận null key
    @Test
    void getAuctionByIdShouldThrowWhenIdIsNull() {
        assertThrows(NullPointerException.class, () -> {
            auctionManage.getAuctionById(null);
        });
    }

    // removeAuctionById(null) hiện tại sẽ lỗi vì ConcurrentHashMap không nhận null key
    @Test
    void removeAuctionByIdShouldThrowWhenIdIsNull() {
        assertThrows(NullPointerException.class, () -> {
            auctionManage.removeAuctionById(null);
        });
    }

    // addAuction cùng id sẽ ghi đè auction cũ, test để phát hiện mất dữ liệu nếu id trùng
    @Test
    void addAuctionShouldReplaceOldAuctionWhenSameId() {
        Auction oldAuction = sampleAuction("auction-1");
        Auction newAuction = sampleAuction("auction-1");

        auctionManage.addAuction(oldAuction);
        auctionManage.addAuction(newAuction);

        assertSame(newAuction, auctionManage.getAuctionById("auction-1"));
        assertEquals(1, auctionManage.getAllActive().size());
    }

    // getAllActive trả bản copy, sửa list result không được làm mất dữ liệu thật trong RAM
    @Test
    void getAllActiveShouldReturnCopyList() {
        Auction auction = sampleAuction("auction-1");
        auctionManage.addAuction(auction);

        List<Auction> result = auctionManage.getAllActive();
        result.clear();

        assertEquals(1, auctionManage.getAllActive().size());
        assertSame(auction, auctionManage.getAuctionById("auction-1"));
    }

    // getAllActive tên dễ hiểu nhầm: hiện tại nó không lọc FINISHED
    @Test
    void getAllActiveShouldReturnFinishedAuctionIfStillInMemory() {
        Auction finishedAuction = sampleAuctionWithTime(
                "auction-finished",
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1)
        );

        finishedAuction.refreshStatus(LocalDateTime.now());
        auctionManage.addAuction(finishedAuction);

        List<Auction> result = auctionManage.getAllActive();

        assertEquals(1, result.size());
        assertSame(finishedAuction, result.get(0));
        assertEquals(AuctionStatus.FINISHED, finishedAuction.getStatus());
    }

    // removeAuctionById với id không tồn tại không được làm ảnh hưởng auction khác
    @Test
    void removeAuctionByIdShouldNotAffectOtherAuctionsWhenIdMissing() {
        Auction auction1 = sampleAuction("auction-1");
        Auction auction2 = sampleAuction("auction-2");

        auctionManage.addAuction(auction1);
        auctionManage.addAuction(auction2);

        auctionManage.removeAuctionById("missing-auction");

        assertEquals(2, auctionManage.getAllActive().size());
        assertSame(auction1, auctionManage.getAuctionById("auction-1"));
        assertSame(auction2, auctionManage.getAuctionById("auction-2"));
    }

    // remove một auction không được xóa nhầm auction khác
    @Test
    void removeAuctionByIdShouldOnlyRemoveTargetAuction() {
        Auction auction1 = sampleAuction("auction-1");
        Auction auction2 = sampleAuction("auction-2");

        auctionManage.addAuction(auction1);
        auctionManage.addAuction(auction2);

        auctionManage.removeAuctionById("auction-1");

        assertNull(auctionManage.getAuctionById("auction-1"));
        assertSame(auction2, auctionManage.getAuctionById("auction-2"));
        assertEquals(1, auctionManage.getAllActive().size());
    }

    // getAuctionById với id thiếu không được tạo lastAccessedTime rác
    @Test
    void getAuctionByIdShouldNotCreateLastAccessedTimeWhenMissing() throws Exception {
        auctionManage.getAuctionById("missing-auction");

        Map<String, LocalDateTime> lastAccessedTime = getLastAccessedTimeMap();

        assertFalse(lastAccessedTime.containsKey("missing-auction"));
    }
}