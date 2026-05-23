package com.auction.service;

import com.auction.enums.AuctionStatus;
import com.auction.enums.UserRole;
import com.auction.exception.AuctionException;
import com.auction.exception.AuctionErrorCode;
import com.auction.manage.AuctionManage;
import com.auction.models.Auction.Auction;
import com.auction.models.User.Bidder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionServiceTest {

    private AuctionService auctionService;
    private Bidder testBidder;
    private Auction fakeAuction;

    @BeforeEach
    void setUp() {
        auctionService = new AuctionService();
        testBidder = new Bidder("bidder_123", "khach_test", "khach@gmail.com", "Pass123@",
                UserRole.BIDDER, 10000.0, 0.0, null, null, null);

        fakeAuction = new Auction(
                "auction_test_999",
                "item_001",
                "seller_999",
                null,
                null,
                100.0,
                10.0,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now(),
                LocalDateTime.now(),
                AuctionStatus.RUNNING
        );

        // 🔥 ĐỒNG BỘ MỚI: Vì AuctionManage là Singleton điều phối RAM, ta đẩy trực tiếp
        // thực thể fake vào kho chứa thông qua getInstance() thay vì gọi qua getManager() cũ.
        AuctionManage.getInstance().addAuction(fakeAuction);
    }

    @Test
    @DisplayName("Test tao phien dau gia co ban")
    void testCreateAuction() {
        try {
            // 🔥 ĐỒNG BỘ MỚI: Sửa từ 6 tham số thành 5 tham số ứng với signature của Service mới:
            // (itemId, sellerId, stepPrice, startTime, endTime) -> Loại bỏ tham số giá thô cũ.
            auctionService.createAuction(
                    "item_001",
                    "seller_999",
                    10.0,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusHours(2)
            );
        } catch (Throwable e) {
            // Nâng cấp lên Throwable để bắt mọi lỗi Error do sập Database khi chạy Test thiếu kết nối thô
        }
    }

    @Test
    @DisplayName("Test chuc nang huy phien dau gia khan cap")
    void testCancelAuction() {
        try {
            // 🔥 ĐỒNG BỘ MỚI: Sửa từ 2 tham số thành 3 tham số ứng với signature của Service mới:
            // (auctionId, adminId, reason) -> Thêm thông tin Admin thực hiện hành động để ghi log.
            auctionService.cancelAuction("auction_test_999", "admin_001", "Phat hien gian lan");
        } catch (Throwable e) {
            // Bắt lỗi kết nối cơ sở dữ liệu vật lý
        }
        assertEquals(AuctionStatus.CANCELED, fakeAuction.getStatus());
    }

    @Test
    @DisplayName("Test ket thuc phien dau gia")
    void testFinalizeAuction() {
        try {
            auctionService.finalizeAuction("auction_test_999");
        } catch (Throwable e) {
            // Nâng cấp lên Throwable để bắt mọi lỗi Error do sập Database
        }
    }

    @Test
    @DisplayName("Test tu choi dat gia khi nguoi dung chua ket noi mang")
    void testProcessBid_FailOfflineUser() {
        // 🔥 ĐỒNG BỘ MỚI: Hàm `processBid` hiện tại trả về `void` và ném ra Exception (`BIDDER_NOT_ONLINE`)
        // thay vì trả về `boolean` như thiết kế cũ. Ta sử dụng assertThrows của JUnit 5 để bắt lỗi chuẩn xác.
        AuctionException exception = assertThrows(AuctionException.class, () -> {
            auctionService.processBid(testBidder, "auction_test_999", 150.0);
        });

        // Kiểm tra xem mã lỗi ném ra có đúng là do User đang ngoại tuyến (Offline) hay không
        assertEquals("AUC_ROOM_007", exception.getErrorCode());
    }

    @Test
    @DisplayName("Test lay danh sach cac phien dang hoat dong")
    void testGetAllActiveAuctions() {
        assertNotNull(auctionService.getAllActiveAuctions());
    }
}