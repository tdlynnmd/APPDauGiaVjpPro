package com.auction.controller;

import com.auction.dto.AuctionDetailDTO;
import com.auction.dto.AuctionSubscriptionRequest;
import com.auction.dto.AuctionSummaryDTO;
import com.auction.dto.BidTransactionDTO;
import com.auction.dto.CancelAuctionRequest;
import com.auction.dto.CancelAutoBidRequest;
import com.auction.dto.CreateAuctionRequest;
import com.auction.dto.GetAuctionBidsRequest;
import com.auction.dto.GetAuctionDetailRequest;
import com.auction.dto.PageDTO;
import com.auction.dto.PlaceBidRequest;
import com.auction.dto.SetupAutoBidRequest;
import com.auction.enums.UserRole;
import com.auction.exception.ValidationErrorCode;
import com.auction.exception.ValidationException;
import com.auction.network.ClientSession;
import com.auction.service.AuctionService;
import com.auction.service.BidTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuctionControllerTest {

    private AuctionController auctionController;
    private FakeAuctionService auctionService;
    private FakeBidTransactionService bidTransactionService;

    @BeforeEach
    void setUp() throws Exception {
        auctionController = new AuctionController();

        auctionService = new FakeAuctionService();
        bidTransactionService = new FakeBidTransactionService();

        injectField(auctionController, "auctionService", auctionService);
        injectField(auctionController, "bidTransactionService", bidTransactionService);
    }

    // Inject fake service vào field private final của controller
    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // Check đúng mã lỗi ValidationException
    private void assertValidationError(ValidationException exception, ValidationErrorCode expectedError) {
        assertEquals(expectedError.getCode(), exception.getErrorCode());
    }

    // Tạo ClientSession fake
    private ClientSession fakeSession() {
        return new ClientSession((Socket) null, new PrintWriter(System.out));
    }

    // Tạo AuctionDetailDTO mẫu
    private AuctionDetailDTO sampleAuctionDetail() {
        return new AuctionDetailDTO(
                "auction-1",
                1000.0,
                100.0,
                LocalDateTime.now().plusHours(1),
                "RUNNING",
                "Laptop Dell",
                "Laptop văn phòng",
                "image.png",
                "seller01",
                List.of()
        );
    }

    // Tạo BidTransactionDTO mẫu
    private BidTransactionDTO sampleBidDTO() {
        return new BidTransactionDTO(
                "bidder01",
                1500.0,
                LocalDateTime.now(),
                "ACCEPTED"
        );
    }

    // Fake AuctionService để không gọi DB/service thật
    private static class FakeAuctionService extends AuctionService {
        List<AuctionSummaryDTO> activeAuctionsToReturn = List.of();
        AuctionDetailDTO auctionDetailToReturn;

        String lastCreateItemId;
        String lastCreateSellerId;
        double lastCreateStepPrice;
        LocalDateTime lastCreateStartTime;
        LocalDateTime lastCreateEndTime;

        String lastProcessBidderId;
        String lastProcessAuctionId;
        double lastProcessAmount;

        String lastJoinLiveBidderId;
        String lastJoinLiveAuctionId;
        ClientSession lastJoinLiveSession;

        String lastLeaveLiveBidderId;
        String lastLeaveLiveAuctionId;
        ClientSession lastLeaveLiveSession;

        String lastJoinAuctionBidderId;
        String lastJoinAuctionId;

        String lastLeaveAuctionBidderId;
        String lastLeaveAuctionId;
        ClientSession lastLeaveAuctionSession;

        String lastCancelAuctionId;
        String lastCancelOperatorId;
        UserRole lastCancelOperatorRole;
        String lastCancelReason;

        String lastSetupAutoBidderId;
        String lastSetupAutoAuctionId;
        double lastSetupAutoMaxBid;
        double lastSetupAutoIncrement;

        String lastCancelAutoBidderId;
        String lastCancelAutoAuctionId;

        @Override
        public List<AuctionSummaryDTO> getAllActiveAuctions(String currentUserId) {
            return activeAuctionsToReturn;
        }

        @Override
        public AuctionDetailDTO getAuctionDetail(String auctionId) {
            lastProcessAuctionId = auctionId;
            return auctionDetailToReturn;
        }

        @Override
        public void createAuction(String itemId, String sellerId, double stepPrice,
                                  LocalDateTime startTime, LocalDateTime endTime) {
            lastCreateItemId = itemId;
            lastCreateSellerId = sellerId;
            lastCreateStepPrice = stepPrice;
            lastCreateStartTime = startTime;
            lastCreateEndTime = endTime;
        }

        @Override
        public void processBid(String bidderId, String auctionId, double amount) {
            lastProcessBidderId = bidderId;
            lastProcessAuctionId = auctionId;
            lastProcessAmount = amount;
        }

        @Override
        public void joinLiveRoom(String bidderId, String auctionId, ClientSession session) {
            lastJoinLiveBidderId = bidderId;
            lastJoinLiveAuctionId = auctionId;
            lastJoinLiveSession = session;
        }

        @Override
        public void leaveLiveRoom(String bidderId, String auctionId, ClientSession session) {
            lastLeaveLiveBidderId = bidderId;
            lastLeaveLiveAuctionId = auctionId;
            lastLeaveLiveSession = session;
        }

        @Override
        public void joinAuction(String bidderId, String auctionId) {
            lastJoinAuctionBidderId = bidderId;
            lastJoinAuctionId = auctionId;
        }

        @Override
        public void leaveAuction(String bidderId, String auctionId, ClientSession session) {
            lastLeaveAuctionBidderId = bidderId;
            lastLeaveAuctionId = auctionId;
            lastLeaveAuctionSession = session;
        }

        @Override
        public void cancelAuction(String auctionId, String operatorId, UserRole operatorRole, String reason) {
            lastCancelAuctionId = auctionId;
            lastCancelOperatorId = operatorId;
            lastCancelOperatorRole = operatorRole;
            lastCancelReason = reason;
        }

        @Override
        public void setupAutoBid(String bidderId, String auctionId, double maxBid, double increment) {
            lastSetupAutoBidderId = bidderId;
            lastSetupAutoAuctionId = auctionId;
            lastSetupAutoMaxBid = maxBid;
            lastSetupAutoIncrement = increment;
        }

        @Override
        public void cancelAutoBid(String bidderId, String auctionId) {
            lastCancelAutoBidderId = bidderId;
            lastCancelAutoAuctionId = auctionId;
        }
    }

    // Fake BidTransactionService để không gọi DB thật
    private static class FakeBidTransactionService extends BidTransactionService {
        PageDTO<BidTransactionDTO> auctionBidsToReturn;

        String lastAuctionId;
        int lastPage;
        int lastPageSize;

        @Override
        public PageDTO<BidTransactionDTO> getAuctionBidsPaged(String auctionId, int page, int pageSize) {
            lastAuctionId = auctionId;
            lastPage = page;
            lastPageSize = pageSize;
            return auctionBidsToReturn;
        }
    }

    // =========================================================
    // getActiveAuctions()
    // =========================================================

    // getActiveAuctions phải trả danh sách từ AuctionService
    @Test
    void getActiveAuctionsShouldReturnListFromService() {
        List<AuctionSummaryDTO> auctions = List.of(
                new AuctionSummaryDTO(
                        "auction-1",
                        "Laptop Dell",
                        1000.0,
                        "RUNNING",
                        LocalDateTime.now().plusHours(1)
                )
        );

        auctionService.activeAuctionsToReturn = auctions;

        List<AuctionSummaryDTO> result = auctionController.getActiveAuctions("user-1");

        assertSame(auctions, result);
    }

    // =========================================================
    // getAuctionDetail()
    // =========================================================

    // getAuctionDetail request null phải ném BAD_REQUEST
    @Test
    void getAuctionDetailShouldThrowWhenRequestIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            auctionController.getAuctionDetail(null);
        });

        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    // getAuctionDetail hợp lệ phải gọi AuctionService.getAuctionDetail
    @Test
    void getAuctionDetailShouldReturnDetailFromService() {
        AuctionDetailDTO detail = sampleAuctionDetail();
        auctionService.auctionDetailToReturn = detail;

        GetAuctionDetailRequest request = new GetAuctionDetailRequest("auction-1");

        AuctionDetailDTO result = auctionController.getAuctionDetail(request);

        assertSame(detail, result);
        assertEquals("auction-1", auctionService.lastProcessAuctionId);
    }

    // =========================================================
    // createAuction()
    // =========================================================

    // createAuction request null phải ném BAD_REQUEST
    @Test
    void createAuctionShouldThrowWhenRequestIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            auctionController.createAuction("seller-1", null);
        });

        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    // createAuction stepPrice <= 0 phải ném INVALID_PARAMETER
    @Test
    void createAuctionShouldThrowWhenStepPriceIsZero() {
        CreateAuctionRequest request = new CreateAuctionRequest(
                LocalDateTime.now().plusHours(2).toString(),
                "item-1",
                LocalDateTime.now().plusHours(1).toString(),
                0.0
        );

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            auctionController.createAuction("seller-1", request);
        });

        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    // createAuction endTime <= startTime phải ném INVALID_PARAMETER
    @Test
    void createAuctionShouldThrowWhenEndTimeIsNotAfterStartTime() {
        LocalDateTime start = LocalDateTime.now().plusHours(2);
        LocalDateTime end = start.minusMinutes(1);

        CreateAuctionRequest request = new CreateAuctionRequest(
                end.toString(),
                "item-1",
                start.toString(),
                100.0
        );

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            auctionController.createAuction("seller-1", request);
        });

        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    // createAuction thời gian không parse được hiện tại ném DateTimeParseException
    @Test
    void createAuctionShouldThrowWhenDateFormatInvalid() {
        CreateAuctionRequest request = new CreateAuctionRequest(
                "not-a-date",
                "item-1",
                "also-not-a-date",
                100.0
        );

        assertThrows(RuntimeException.class, () -> {
            auctionController.createAuction("seller-1", request);
        });
    }

    // createAuction hợp lệ phải parse thời gian và gọi AuctionService.createAuction
    @Test
    void createAuctionShouldCallServiceWhenValid() {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = start.plusHours(2);

        CreateAuctionRequest request = new CreateAuctionRequest(
                end.toString(),
                "item-1",
                start.toString(),
                100.0
        );

        auctionController.createAuction("seller-1", request);

        assertEquals("item-1", auctionService.lastCreateItemId);
        assertEquals("seller-1", auctionService.lastCreateSellerId);
        assertEquals(100.0, auctionService.lastCreateStepPrice);
        assertEquals(start, auctionService.lastCreateStartTime);
        assertEquals(end, auctionService.lastCreateEndTime);
    }

    // =========================================================
    // placeBid()
    // =========================================================

    // placeBid request null phải ném BAD_REQUEST
    @Test
    void placeBidShouldThrowWhenRequestIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            auctionController.placeBid("bidder-1", null);
        });

        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    // placeBid amount <= 0 phải ném INVALID_PARAMETER
    @Test
    void placeBidShouldThrowWhenAmountIsZero() {
        PlaceBidRequest request = new PlaceBidRequest("auction-1", 0.0);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            auctionController.placeBid("bidder-1", request);
        });

        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    // placeBid hợp lệ phải gọi AuctionService.processBid
    @Test
    void placeBidShouldCallServiceWhenValid() {
        PlaceBidRequest request = new PlaceBidRequest("auction-1", 1500.0);

        auctionController.placeBid("bidder-1", request);

        assertEquals("bidder-1", auctionService.lastProcessBidderId);
        assertEquals("auction-1", auctionService.lastProcessAuctionId);
        assertEquals(1500.0, auctionService.lastProcessAmount);
    }

    // =========================================================
    // live room
    // =========================================================

    // joinLiveRoom request null phải ném BAD_REQUEST
    @Test
    void joinLiveRoomShouldThrowWhenRequestIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            auctionController.joinLiveRoom("bidder-1", null, fakeSession());
        });

        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    // joinLiveRoom hợp lệ phải gọi service và trả AuctionDetailDTO
    @Test
    void joinLiveRoomShouldCallServiceAndReturnAuctionDetail() {
        ClientSession session = fakeSession();
        AuctionDetailDTO detail = sampleAuctionDetail();
        auctionService.auctionDetailToReturn = detail;

        AuctionSubscriptionRequest request = new AuctionSubscriptionRequest("auction-1");

        AuctionDetailDTO result = auctionController.joinLiveRoom("bidder-1", request, session);

        assertSame(detail, result);
        assertEquals("bidder-1", auctionService.lastJoinLiveBidderId);
        assertEquals("auction-1", auctionService.lastJoinLiveAuctionId);
        assertSame(session, auctionService.lastJoinLiveSession);

        // joinLiveRoom sau đó gọi getAuctionDetail nên lastProcessAuctionId cũng là auction-1
        assertEquals("auction-1", auctionService.lastProcessAuctionId);
    }

    // leaveLiveRoom request null phải ném BAD_REQUEST
    @Test
    void leaveLiveRoomShouldThrowWhenRequestIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            auctionController.leaveLiveRoom("bidder-1", null, fakeSession());
        });

        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    // leaveLiveRoom hợp lệ phải gọi AuctionService.leaveLiveRoom
    @Test
    void leaveLiveRoomShouldCallServiceWhenValid() {
        ClientSession session = fakeSession();
        AuctionSubscriptionRequest request = new AuctionSubscriptionRequest("auction-1");

        auctionController.leaveLiveRoom("bidder-1", request, session);

        assertEquals("bidder-1", auctionService.lastLeaveLiveBidderId);
        assertEquals("auction-1", auctionService.lastLeaveLiveAuctionId);
        assertSame(session, auctionService.lastLeaveLiveSession);
    }

    // =========================================================
    // joinAuction / leaveAuction
    // =========================================================

    // joinAuction request null phải ném BAD_REQUEST
    @Test
    void joinAuctionShouldThrowWhenRequestIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            auctionController.joinAuction("bidder-1", null);
        });

        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    // joinAuction hợp lệ phải gọi AuctionService.joinAuction
    @Test
    void joinAuctionShouldCallServiceWhenValid() {
        AuctionSubscriptionRequest request = new AuctionSubscriptionRequest("auction-1");

        auctionController.joinAuction("bidder-1", request);

        assertEquals("bidder-1", auctionService.lastJoinAuctionBidderId);
        assertEquals("auction-1", auctionService.lastJoinAuctionId);
    }

    // leaveAuction request null phải ném BAD_REQUEST
    @Test
    void leaveAuctionShouldThrowWhenRequestIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            auctionController.leaveAuction("bidder-1", null, fakeSession());
        });

        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    // leaveAuction hợp lệ phải gọi AuctionService.leaveAuction
    @Test
    void leaveAuctionShouldCallServiceWhenValid() {
        ClientSession session = fakeSession();
        AuctionSubscriptionRequest request = new AuctionSubscriptionRequest("auction-1");

        auctionController.leaveAuction("bidder-1", request, session);

        assertEquals("bidder-1", auctionService.lastLeaveAuctionBidderId);
        assertEquals("auction-1", auctionService.lastLeaveAuctionId);
        assertSame(session, auctionService.lastLeaveAuctionSession);
    }

    // =========================================================
    // cancelAuctionBySeller()
    // =========================================================

    // cancelAuctionBySeller request null phải ném BAD_REQUEST
    @Test
    void cancelAuctionBySellerShouldThrowWhenRequestIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            auctionController.cancelAuctionBySeller("seller-1", null);
        });

        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    // cancelAuctionBySeller reason rỗng thì dùng reason mặc định
    @Test
    void cancelAuctionBySellerShouldUseDefaultReasonWhenReasonIsBlank() {
        CancelAuctionRequest request = new CancelAuctionRequest("auction-1", "   ", null);

        auctionController.cancelAuctionBySeller("seller-1", request);

        assertEquals("auction-1", auctionService.lastCancelAuctionId);
        assertEquals("seller-1", auctionService.lastCancelOperatorId);
        assertEquals(UserRole.SELLER, auctionService.lastCancelOperatorRole);
        assertEquals("Chủ phòng tự nguyện hủy.", auctionService.lastCancelReason);
    }

    // cancelAuctionBySeller reason hợp lệ thì truyền reason đó xuống service
    @Test
    void cancelAuctionBySellerShouldPassReasonWhenReasonValid() {
        CancelAuctionRequest request = new CancelAuctionRequest("auction-1", "item lỗi", null);

        auctionController.cancelAuctionBySeller("seller-1", request);

        assertEquals("auction-1", auctionService.lastCancelAuctionId);
        assertEquals("seller-1", auctionService.lastCancelOperatorId);
        assertEquals(UserRole.SELLER, auctionService.lastCancelOperatorRole);
        assertEquals("item lỗi", auctionService.lastCancelReason);
    }

    // =========================================================
    // getAuctionBidHistory()
    // =========================================================

    // getAuctionBidHistory request null phải ném BAD_REQUEST
    @Test
    void getAuctionBidHistoryShouldThrowWhenRequestIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            auctionController.getAuctionBidHistory(null);
        });

        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    // getAuctionBidHistory auctionId rỗng phải ném MISSING_REQUIRED_FIELD
    @Test
    void getAuctionBidHistoryShouldThrowWhenAuctionIdIsBlank() {
        GetAuctionBidsRequest request = new GetAuctionBidsRequest("   ", 1, 10);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            auctionController.getAuctionBidHistory(request);
        });

        assertValidationError(exception, ValidationErrorCode.MISSING_REQUIRED_FIELD);
    }

    // getAuctionBidHistory page <= 0 phải ném INVALID_PARAMETER
    @Test
    void getAuctionBidHistoryShouldThrowWhenPageInvalid() {
        GetAuctionBidsRequest request = new GetAuctionBidsRequest("auction-1", 0, 10);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            auctionController.getAuctionBidHistory(request);
        });

        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    // getAuctionBidHistory pageSize <= 0 phải ném INVALID_PARAMETER
    @Test
    void getAuctionBidHistoryShouldThrowWhenPageSizeInvalid() {
        GetAuctionBidsRequest request = new GetAuctionBidsRequest("auction-1", 1, 0);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            auctionController.getAuctionBidHistory(request);
        });

        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    // getAuctionBidHistory hợp lệ phải gọi BidTransactionService
    @Test
    void getAuctionBidHistoryShouldReturnPageFromService() {
        PageDTO<BidTransactionDTO> page = new PageDTO<>(
                List.of(sampleBidDTO()),
                1,
                1,
                1
        );

        bidTransactionService.auctionBidsToReturn = page;

        GetAuctionBidsRequest request = new GetAuctionBidsRequest("auction-1", 1, 10);

        PageDTO<BidTransactionDTO> result = auctionController.getAuctionBidHistory(request);

        assertSame(page, result);
        assertEquals("auction-1", bidTransactionService.lastAuctionId);
        assertEquals(1, bidTransactionService.lastPage);
        assertEquals(10, bidTransactionService.lastPageSize);
    }

    // =========================================================
    // auto bid
    // =========================================================

    // setupAutoBid request null phải ném BAD_REQUEST
    @Test
    void setupAutoBidShouldThrowWhenRequestIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            auctionController.setupAutoBid("bidder-1", null);
        });

        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    // setupAutoBid maxBid <= 0 phải ném INVALID_PARAMETER
    @Test
    void setupAutoBidShouldThrowWhenMaxBidInvalid() {
        SetupAutoBidRequest request = new SetupAutoBidRequest("auction-1", 0.0, 100.0);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            auctionController.setupAutoBid("bidder-1", request);
        });

        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    // setupAutoBid increment <= 0 phải ném INVALID_PARAMETER
    @Test
    void setupAutoBidShouldThrowWhenIncrementInvalid() {
        SetupAutoBidRequest request = new SetupAutoBidRequest("auction-1", 1000.0, 0.0);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            auctionController.setupAutoBid("bidder-1", request);
        });

        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    // setupAutoBid hợp lệ phải gọi AuctionService.setupAutoBid
    @Test
    void setupAutoBidShouldCallServiceWhenValid() {
        SetupAutoBidRequest request = new SetupAutoBidRequest("auction-1", 5000.0, 200.0);

        auctionController.setupAutoBid("bidder-1", request);

        assertEquals("bidder-1", auctionService.lastSetupAutoBidderId);
        assertEquals("auction-1", auctionService.lastSetupAutoAuctionId);
        assertEquals(5000.0, auctionService.lastSetupAutoMaxBid);
        assertEquals(200.0, auctionService.lastSetupAutoIncrement);
    }

    // cancelAutoBid request null phải ném BAD_REQUEST
    @Test
    void cancelAutoBidShouldThrowWhenRequestIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            auctionController.cancelAutoBid("bidder-1", null);
        });

        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    // cancelAutoBid hợp lệ phải gọi AuctionService.cancelAutoBid
    @Test
    void cancelAutoBidShouldCallServiceWhenValid() {
        CancelAutoBidRequest request = new CancelAutoBidRequest("auction-1");

        auctionController.cancelAutoBid("bidder-1", request);

        assertEquals("bidder-1", auctionService.lastCancelAutoBidderId);
        assertEquals("auction-1", auctionService.lastCancelAutoAuctionId);
    }
}