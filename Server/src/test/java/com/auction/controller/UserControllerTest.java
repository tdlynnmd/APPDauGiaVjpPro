package com.auction.controller;

import com.auction.dto.BidTransactionDTO;
import com.auction.dto.DepositRequest;
import com.auction.dto.GetBidderHistoryRequest;
import com.auction.dto.PageDTO;
import com.auction.dto.UserDTO;
import com.auction.dto.WithdrawRequest;
import com.auction.enums.BidStatus;
import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;
import com.auction.exception.ValidationErrorCode;
import com.auction.exception.ValidationException;
import com.auction.service.BidTransactionService;
import com.auction.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest {

    private UserController userController;
    private FakeUserService userService;
    private FakeBidTransactionService bidTransactionService;

    @BeforeEach
    void setUp() throws Exception {
        userController = new UserController();

        userService = new FakeUserService();
        bidTransactionService = new FakeBidTransactionService();

        injectField(userController, "userService", userService);
        injectField(userController, "bidTransactionService", bidTransactionService);
    }

    // Inject fake service vào field private của controller
    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // Check đúng mã lỗi ValidationException
    private void assertValidationError(ValidationException exception, ValidationErrorCode expectedError) {
        assertEquals(expectedError.getCode(), exception.getErrorCode());
    }

    // Tạo UserDTO mẫu
    private UserDTO sampleUserDTO() {
        return new UserDTO(
                "user-1",
                "bidder01",
                "bidder01@example.com",
                UserRole.BIDDER,
                UserStatus.ACTIVE,
                1000.0,
                0.0
        );
    }

    // Tạo BidTransactionDTO mẫu
    private BidTransactionDTO sampleBidDTO(String bidderName, double amount) {
        return new BidTransactionDTO(
                bidderName,
                amount,
                LocalDateTime.now(),
                BidStatus.ACCEPTED.name()
        );
    }

    // Fake UserService để không gọi service thật/database thật
    private static class FakeUserService extends UserService {
        UserDTO userProfileToReturn;

        String lastGetProfileUserId;
        String lastDepositUserId;
        double lastDepositAmount;
        String lastWithdrawUserId;
        double lastWithdrawAmount;

        boolean depositCalled = false;
        boolean withdrawCalled = false;

        @Override
        public UserDTO getUserProfile(String userId) {
            lastGetProfileUserId = userId;
            return userProfileToReturn;
        }

        @Override
        public void depositMoney(String bidderId, double amount) {
            depositCalled = true;
            lastDepositUserId = bidderId;
            lastDepositAmount = amount;
        }

        @Override
        public void withdrawMoney(String bidderId, double amount) {
            withdrawCalled = true;
            lastWithdrawUserId = bidderId;
            lastWithdrawAmount = amount;
        }
    }

    // Fake BidTransactionService để không gọi database thật
    private static class FakeBidTransactionService extends BidTransactionService {
        PageDTO<BidTransactionDTO> historyToReturn;

        String lastBidderId;
        int lastPage;
        int lastPageSize;

        @Override
        public PageDTO<BidTransactionDTO> getBidderHistoryPaged(String bidderId, int page, int pageSize) {
            lastBidderId = bidderId;
            lastPage = page;
            lastPageSize = pageSize;
            return historyToReturn;
        }
    }

    // =========================================================
    // getUserProfile()
    // =========================================================

    // getUserProfile phải gọi UserService.getUserProfile với đúng userId
    @Test
    void getUserProfileShouldReturnUserProfileFromService() {
        UserDTO userDTO = sampleUserDTO();
        userService.userProfileToReturn = userDTO;

        UserDTO result = userController.getUserProfile("user-1");

        assertSame(userDTO, result);
        assertEquals("user-1", userService.lastGetProfileUserId);
    }

    // getUserProfile với userId null vẫn truyền xuống service để service validate
    @Test
    void getUserProfileShouldPassNullUserIdToService() {
        userService.userProfileToReturn = null;

        UserDTO result = userController.getUserProfile(null);

        assertNull(result);
        assertNull(userService.lastGetProfileUserId);
    }

    // =========================================================
    // depositMoney()
    // =========================================================

    // depositMoney request null phải ném BAD_REQUEST
    @Test
    void depositMoneyShouldThrowWhenRequestIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userController.depositMoney("user-1", null);
        });

        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
        assertFalse(userService.depositCalled);
    }

    // depositMoney amount = 0 phải ném INVALID_PARAMETER
    @Test
    void depositMoneyShouldThrowWhenAmountIsZero() {
        DepositRequest request = new DepositRequest(0.0);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userController.depositMoney("user-1", request);
        });

        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
        assertFalse(userService.depositCalled);
    }

    // depositMoney amount âm phải ném INVALID_PARAMETER
    @Test
    void depositMoneyShouldThrowWhenAmountIsNegative() {
        DepositRequest request = new DepositRequest(-100.0);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userController.depositMoney("user-1", request);
        });

        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
        assertFalse(userService.depositCalled);
    }

    // depositMoney hợp lệ phải gọi UserService.depositMoney rồi lấy profile mới
    @Test
    void depositMoneyShouldDepositAndReturnUpdatedProfile() {
        UserDTO updatedProfile = sampleUserDTO();
        userService.userProfileToReturn = updatedProfile;

        DepositRequest request = new DepositRequest(500.0);

        UserDTO result = userController.depositMoney("user-1", request);

        assertSame(updatedProfile, result);

        assertTrue(userService.depositCalled);
        assertEquals("user-1", userService.lastDepositUserId);
        assertEquals(500.0, userService.lastDepositAmount);

        assertEquals("user-1", userService.lastGetProfileUserId);
    }

    // depositMoney userId null vẫn truyền xuống service để service validate
    @Test
    void depositMoneyShouldPassNullUserIdToServiceWhenAmountValid() {
        userService.userProfileToReturn = sampleUserDTO();

        DepositRequest request = new DepositRequest(500.0);

        UserDTO result = userController.depositMoney(null, request);

        assertNotNull(result);
        assertTrue(userService.depositCalled);
        assertNull(userService.lastDepositUserId);
        assertNull(userService.lastGetProfileUserId);
    }

    // =========================================================
    // withdrawMoney()
    // =========================================================

    // withdrawMoney request null phải ném BAD_REQUEST
    @Test
    void withdrawMoneyShouldThrowWhenRequestIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userController.withdrawMoney("user-1", null);
        });

        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
        assertFalse(userService.withdrawCalled);
    }

    // withdrawMoney amount = 0 phải ném INVALID_PARAMETER
    @Test
    void withdrawMoneyShouldThrowWhenAmountIsZero() {
        WithdrawRequest request = new WithdrawRequest(0.0);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userController.withdrawMoney("user-1", request);
        });

        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
        assertFalse(userService.withdrawCalled);
    }

    // withdrawMoney amount âm phải ném INVALID_PARAMETER
    @Test
    void withdrawMoneyShouldThrowWhenAmountIsNegative() {
        WithdrawRequest request = new WithdrawRequest(-100.0);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userController.withdrawMoney("user-1", request);
        });

        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
        assertFalse(userService.withdrawCalled);
    }

    // withdrawMoney hợp lệ phải gọi UserService.withdrawMoney rồi lấy profile mới
    @Test
    void withdrawMoneyShouldWithdrawAndReturnUpdatedProfile() {
        UserDTO updatedProfile = sampleUserDTO();
        userService.userProfileToReturn = updatedProfile;

        WithdrawRequest request = new WithdrawRequest(200.0);

        UserDTO result = userController.withdrawMoney("user-1", request);

        assertSame(updatedProfile, result);

        assertTrue(userService.withdrawCalled);
        assertEquals("user-1", userService.lastWithdrawUserId);
        assertEquals(200.0, userService.lastWithdrawAmount);

        assertEquals("user-1", userService.lastGetProfileUserId);
    }

    // withdrawMoney userId null vẫn truyền xuống service để service validate
    @Test
    void withdrawMoneyShouldPassNullUserIdToServiceWhenAmountValid() {
        userService.userProfileToReturn = sampleUserDTO();

        WithdrawRequest request = new WithdrawRequest(200.0);

        UserDTO result = userController.withdrawMoney(null, request);

        assertNotNull(result);
        assertTrue(userService.withdrawCalled);
        assertNull(userService.lastWithdrawUserId);
        assertNull(userService.lastGetProfileUserId);
    }

    // =========================================================
    // getMyBidHistory()
    // =========================================================

    // getMyBidHistory page <= 0 phải ném INVALID_PARAMETER
    @Test
    void getMyBidHistoryShouldThrowWhenPageIsInvalid() {
        GetBidderHistoryRequest request = new GetBidderHistoryRequest(0, 10);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userController.getMyBidHistory("bidder-1", request);
        });

        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    // getMyBidHistory pageSize <= 0 phải ném INVALID_PARAMETER
    @Test
    void getMyBidHistoryShouldThrowWhenPageSizeIsInvalid() {
        GetBidderHistoryRequest request = new GetBidderHistoryRequest(1, 0);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userController.getMyBidHistory("bidder-1", request);
        });

        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    // getMyBidHistory hợp lệ phải gọi BidTransactionService.getBidderHistoryPaged
    @Test
    void getMyBidHistoryShouldReturnHistoryFromService() {
        PageDTO<BidTransactionDTO> history = new PageDTO<>(
                List.of(sampleBidDTO("alice", 100.0)),
                2,
                5,
                41
        );

        bidTransactionService.historyToReturn = history;

        GetBidderHistoryRequest request = new GetBidderHistoryRequest(2, 10);

        PageDTO<BidTransactionDTO> result = userController.getMyBidHistory("bidder-1", request);

        assertSame(history, result);

        assertEquals("bidder-1", bidTransactionService.lastBidderId);
        assertEquals(2, bidTransactionService.lastPage);
        assertEquals(10, bidTransactionService.lastPageSize);
    }

    // getMyBidHistory bidderId null vẫn truyền xuống service để service validate
    @Test
    void getMyBidHistoryShouldPassNullBidderIdToService() {
        PageDTO<BidTransactionDTO> history = new PageDTO<>(
                List.of(),
                1,
                0,
                0
        );

        bidTransactionService.historyToReturn = history;

        GetBidderHistoryRequest request = new GetBidderHistoryRequest(1, 10);

        PageDTO<BidTransactionDTO> result = userController.getMyBidHistory(null, request);

        assertSame(history, result);
        assertNull(bidTransactionService.lastBidderId);
        assertEquals(1, bidTransactionService.lastPage);
        assertEquals(10, bidTransactionService.lastPageSize);
    }

    // getMyBidHistory request null hiện tại sẽ NullPointerException vì controller chưa check null
    @Test
    void getMyBidHistoryShouldThrowNullPointerExceptionWhenRequestIsNull() {
        assertThrows(NullPointerException.class, () -> {
            userController.getMyBidHistory("bidder-1", null);
        });
    }
}