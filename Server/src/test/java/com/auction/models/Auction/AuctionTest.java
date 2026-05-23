package com.auction.models.Auction;

import com.auction.enums.AuctionStatus;
import com.auction.enums.BidStatus;
import com.auction.enums.UserRole;
import com.auction.exception.AuctionException;
import com.auction.models.User.Bidder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionTest {

    private Auction auction;
    private Bidder validBidder;
    private Bidder sellerActingAsBidder;

    @BeforeEach
    void setUp() {
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusHours(1);

        // Su dung ham khoi tao so 2 (Hydration) de khong can phu thuoc vao doi tuong Item
        auction = new Auction(
                "auction_test_001",
                "item_001",
                "seller_999",
                null,
                null,
                100.0,
                10.0,
                startTime,
                endTime,
                startTime,
                startTime,
                AuctionStatus.OPEN
        );

        validBidder = new Bidder("bidder_123", "khach", "khach@gmail.com", "Pass123@",
                UserRole.BIDDER, 5000.0, 0.0, null, null, null);

        sellerActingAsBidder = new Bidder("seller_999", "gian_thuong", "ban@gmail.com", "Pass123@",
                UserRole.BIDDER, 5000.0, 0.0, null, null, null);
    }

    @Test
    @DisplayName("Test cap nhat trang thai: Dang chay sang Ket thuc")
    void testRefreshStatus_TimeExpired() {
        LocalDateTime expiredTime = LocalDateTime.now().plusHours(2);
        auction.refreshStatus(expiredTime);
        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
    }

    @Test
    @DisplayName("Test tu choi: Nguoi ban tu dat gia")
    void testPlaceBid_RejectSeller() {
        AuctionException exception = assertThrows(AuctionException.class, () -> {
            BidTransaction result = auction.placeBid(sellerActingAsBidder, 150.0, "bid_01");
        });

        // Kiểm tra xem mã lỗi ném ra có đúng là do người bán không được phép đặt giá hay không
        assertEquals("AUC_ROOM_008", exception.getErrorCode());
    }

    @Test
    @DisplayName("Test tu choi: Gia dat thap hon buoc gia")
    void testPlaceBid_RejectLowAmount() {
        AuctionException exception = assertThrows(AuctionException.class, () -> {
            BidTransaction result = auction.placeBid(validBidder, 105.0, "bid_02");
        });

        // Kiểm tra xem mã lỗi ném ra có đúng là do người bán không được phép đặt giá hay không
        assertEquals("AUC_ROOM_004", exception.getErrorCode());
    }

    @Test
    @DisplayName("Test dat gia dung va cong gio chong ban tia")
    void testPlaceBid_SuccessAndAntiSniping() {
        // Khoi tao lai phong dau gia sap dong cua
        auction = new Auction(
                "auction_test_002",
                "item_001",
                "seller_999",
                null,
                null,
                100.0,
                10.0,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusSeconds(10),
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now(),
                AuctionStatus.OPEN
        );

        LocalDateTime tightEndTime = auction.getEndTime();
        BidTransaction result = auction.placeBid(validBidder, 200.0, "bid_03");

        assertEquals(BidStatus.ACCEPTED, result.getStatus());
        assertEquals(200.0, auction.getCurrentPrice());
        assertEquals("bidder_123", auction.getHighestBidderId());
        assertTrue(auction.getEndTime().isAfter(tightEndTime));
    }
}