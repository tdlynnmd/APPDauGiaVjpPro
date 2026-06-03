package com.auction.service;

import com.auction.dao.BidTransactionDAO;
import com.auction.dao.UserDAO;
import com.auction.dto.BidTransactionDTO;
import com.auction.dto.PageDTO;
import com.auction.enums.BidStatus;
import com.auction.exception.ValidationErrorCode;
import com.auction.exception.ValidationException;
import com.auction.models.Auction.BidTransaction;
import com.auction.models.User.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BidTransactionServiceTest {

    private BidTransactionService bidTransactionService;
    private FakeBidTransactionDAO bidTransactionDAO;
    private FakeUserDAO userDAO;

    // Chuẩn bị service và fake DAO trước mỗi test
    @BeforeEach
    void setUp() throws Exception {
        bidTransactionService = new BidTransactionService();

        bidTransactionDAO = new FakeBidTransactionDAO();
        userDAO = new FakeUserDAO();

        injectField(bidTransactionService, "bidTransactionDAO", bidTransactionDAO);
        injectField(bidTransactionService, "userDAO", userDAO);
    }

    // Gán fake DAO vào field private của service
    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // Kiểm tra exception có đúng mã lỗi validation không
    private void assertValidationError(ValidationException exception, ValidationErrorCode expectedError) {
        assertEquals(expectedError.getCode(), exception.getErrorCode());
    }

    // Tạo một bid transaction mẫu để dùng trong test
    private BidTransaction sampleBid(String id, String bidderId, String auctionId, double amount, BidStatus status) {
        return new BidTransaction(
                id,
                bidderId,
                auctionId,
                amount,
                LocalDateTime.now(),
                status
        );
    }

    // Fake BidTransactionDAO để không gọi database thật
    private static class FakeBidTransactionDAO implements BidTransactionDAO {
        List<BidTransaction> auctionBidsToReturn = new ArrayList<>();
        List<BidTransaction> bidderBidsToReturn = new ArrayList<>();

        long totalBidCountByAuction = 0;
        long totalBidCountByBidder = 0;

        int lastLimit;
        int lastOffset;
        String lastAuctionId;
        String lastBidderId;

        boolean findByAuctionIdPagedCalled = false;
        boolean findByBidderIdPagedCalled = false;
        boolean getTotalBidCountByAuctionCalled = false;
        boolean getTotalBidCountByBidderCalled = false;
        // 🔥 THÊM BIẾN NÀY: Để chủ động cấu hình tổng số user trả về khi test
        long countTotalBid = 3;

        // Giả lập insert bid, không dùng DB thật
        @Override
        public boolean insertBid(Connection conn, BidTransaction bid) throws SQLException {
            return true;
        }

        // Không dùng trong test này
        @Override
        public List<BidTransaction> findTopByAuctionId(String auctionId, int limit) {
            return List.of();
        }

        // Giả lập lấy bid theo auction có phân trang
        @Override
        public List<BidTransaction> findByAuctionIdPaged(String auctionId, int limit, int offset) {
            findByAuctionIdPagedCalled = true;
            lastAuctionId = auctionId;
            lastLimit = limit;
            lastOffset = offset;
            return auctionBidsToReturn;
        }

        // Giả lập lấy lịch sử bid của bidder có phân trang
        @Override
        public List<BidTransaction> findByBidderIdPaged(String bidderId, int limit, int offset) {
            findByBidderIdPagedCalled = true;
            lastBidderId = bidderId;
            lastLimit = limit;
            lastOffset = offset;
            return bidderBidsToReturn;
        }

        // Giả lập đếm tổng số bid của một auction
        @Override
        public long getTotalBidCountByAuction(String auctionId) {
            getTotalBidCountByAuctionCalled = true;
            lastAuctionId = auctionId;
            return totalBidCountByAuction;
        }

        // Giả lập đếm tổng số bid của một bidder
        @Override
        public long getTotalBidCountByBidder(String bidderId) {
            getTotalBidCountByBidderCalled = true;
            lastBidderId = bidderId;
            return totalBidCountByBidder;
        }

        // Không dùng trong test này
        @Override
        public void updateStatusToRefunded(Connection conn, String auctionId, String bidderId) throws SQLException {
        }

        @Override
        public void updateStatusByBidId(Connection conn, String bidId, String status) throws SQLException {
        }
    }

    // Fake UserDAO để trả username theo bidderId
    private static class FakeUserDAO implements UserDAO {
        Map<String, String> usernamesById = new HashMap<>();
        List<String> lastRequestedIds = new ArrayList<>();

        // Giả lập lấy username theo danh sách userId
        @Override
        public Map<String, String> findUsernamesByIds(List<String> ids) {
            lastRequestedIds = ids;
            return usernamesById;
        }

        // Các hàm dưới không dùng trong test này
        @Override
        public boolean insertUser(Connection conn, User user) throws SQLException {
            return false;
        }

        @Override
        public Optional<User> findById(String id) {
            return Optional.empty();
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return Optional.empty();
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return Optional.empty();
        }

        @Override
        public boolean freezeMoney(Connection conn, String userId, double amount) throws SQLException {
            return false;
        }

        @Override
        public void unfreezeMoney(Connection conn, String userId, double amount) throws SQLException {
        }

        @Override
        public boolean deductFrozenMoney(Connection conn, String userId, double amount) throws SQLException {
            return false;
        }

        @Override
        public boolean addAvailableBalance(Connection conn, String userId, double amount) throws SQLException {
            return false;
        }

        @Override
        public boolean withdrawAvailableBalance(Connection conn, String userId, double amount) throws SQLException {
            return false;
        }

        @Override
        public boolean addJoinedAuction(Connection conn, String userId, String auctionId) throws SQLException {
            return false;
        }

        @Override
        public void removeJoinedAuction(Connection conn, String userId, String auctionId) throws SQLException {
        }

        @Override
        public List<User> findPaginated(int limit, int offset) {
            return List.of();
        }

        @Override
        public long countTotalUsers() {
            return 0;
        }

        @Override
        public boolean updateStatus(Connection conn, String userId, String name) throws SQLException {
            return false;
        }

        @Override
        public boolean updateProfile(Connection conn, String userId, String username, String email) throws SQLException {
            return false;
        }

        @Override
        public boolean updatePassword(Connection conn, String userId, String hashedPassword) throws SQLException {
            return false;
        }
    }

    // =========================================================
    // recordNewBid() - validation
    // =========================================================



    // =========================================================
    // getAuctionBidsPaged()
    // =========================================================

    // Không cho lấy lịch sử bid khi auctionId null
    @Test
    void getAuctionBidsPagedShouldThrowWhenAuctionIdIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            bidTransactionService.getAuctionBidsPaged(null, 1, 10);
        });

        assertValidationError(exception, ValidationErrorCode.MISSING_REQUIRED_FIELD);
    }

    // Không cho lấy lịch sử bid khi auctionId rỗng
    @Test
    void getAuctionBidsPagedShouldThrowWhenAuctionIdIsBlank() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            bidTransactionService.getAuctionBidsPaged("   ", 1, 10);
        });

        assertValidationError(exception, ValidationErrorCode.MISSING_REQUIRED_FIELD);
    }

    // Không cho page <= 0
    @Test
    void getAuctionBidsPagedShouldThrowWhenPageIsInvalid() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            bidTransactionService.getAuctionBidsPaged("auction-1", 0, 10);
        });

        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    // Không cho pageSize <= 0
    @Test
    void getAuctionBidsPagedShouldThrowWhenPageSizeIsInvalid() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            bidTransactionService.getAuctionBidsPaged("auction-1", 1, 0);
        });

        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    // DAO trả list rỗng thì service trả PageDTO rỗng
    @Test
    void getAuctionBidsPagedShouldReturnEmptyPageWhenDaoReturnsEmptyList() {
        bidTransactionDAO.auctionBidsToReturn = List.of();

        PageDTO<BidTransactionDTO> result =
                bidTransactionService.getAuctionBidsPaged("auction-1", 1, 10);

        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
        assertEquals(1, result.getCurrentPage());
        assertEquals(0, result.getTotalPages());
        assertEquals(0, result.getTotalElements());

        assertTrue(bidTransactionDAO.findByAuctionIdPagedCalled);
        assertFalse(bidTransactionDAO.getTotalBidCountByAuctionCalled);

        assertEquals("auction-1", bidTransactionDAO.lastAuctionId);
        assertEquals(10, bidTransactionDAO.lastLimit);
        assertEquals(0, bidTransactionDAO.lastOffset);
    }

    // DAO trả null thì service vẫn trả PageDTO rỗng
    @Test
    void getAuctionBidsPagedShouldReturnEmptyPageWhenDaoReturnsNull() {
        bidTransactionDAO.auctionBidsToReturn = null;

        PageDTO<BidTransactionDTO> result =
                bidTransactionService.getAuctionBidsPaged("auction-1", 2, 5);

        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
        assertEquals(2, result.getCurrentPage());
        assertEquals(0, result.getTotalPages());
        assertEquals(0, result.getTotalElements());

        assertEquals(5, bidTransactionDAO.lastLimit);
        assertEquals(5, bidTransactionDAO.lastOffset);
    }

    // Có bid thì convert sang DTO, gắn đúng username và phân trang
    @Test
    void getAuctionBidsPagedShouldConvertBidsToDTOs() {
        BidTransaction bid1 = sampleBid("bid-1", "bidder-1", "auction-1", 100.0, BidStatus.ACCEPTED);
        BidTransaction bid2 = sampleBid("bid-2", "bidder-2", "auction-1", 200.0, BidStatus.REFUNDED);

        bidTransactionDAO.auctionBidsToReturn = List.of(bid1, bid2);
        bidTransactionDAO.totalBidCountByAuction = 25;

        userDAO.usernamesById.put("bidder-1", "alice");
        userDAO.usernamesById.put("bidder-2", "bob");

        PageDTO<BidTransactionDTO> result =
                bidTransactionService.getAuctionBidsPaged("auction-1", 2, 10);

        assertEquals(2, result.getData().size());
        assertEquals(2, result.getCurrentPage());
        assertEquals(3, result.getTotalPages());
        assertEquals(25, result.getTotalElements());

        BidTransactionDTO dto1 = result.getData().get(0);
        assertEquals("alice", dto1.getBidderName());
        assertEquals(100.0, dto1.getAmount());
        assertEquals("ACCEPTED", dto1.getStatus());
        assertEquals(bid1.getTime(), dto1.getTime());

        BidTransactionDTO dto2 = result.getData().get(1);
        assertEquals("bob", dto2.getBidderName());
        assertEquals(200.0, dto2.getAmount());
        assertEquals("REFUNDED", dto2.getStatus());

        assertTrue(bidTransactionDAO.getTotalBidCountByAuctionCalled);
        assertEquals(List.of("bidder-1", "bidder-2"), userDAO.lastRequestedIds);
        assertEquals(10, bidTransactionDAO.lastLimit);
        assertEquals(10, bidTransactionDAO.lastOffset);
    }

    // Nếu không tìm thấy username thì dùng tên ẩn danh
    @Test
    void getAuctionBidsPagedShouldUseAnonymousNameWhenUserNotFound() {
        BidTransaction bid = sampleBid(
                "bid-1",
                "unknown-bidder",
                "auction-1",
                100.0,
                BidStatus.ACCEPTED
        );

        bidTransactionDAO.auctionBidsToReturn = List.of(bid);
        bidTransactionDAO.totalBidCountByAuction = 1;

        PageDTO<BidTransactionDTO> result =
                bidTransactionService.getAuctionBidsPaged("auction-1", 1, 10);

        assertEquals(1, result.getData().size());
        assertEquals("Người dùng ẩn danh", result.getData().get(0).getBidderName());
    }

    // =========================================================
    // getBidderHistoryPaged()
    // =========================================================

    // Không cho lấy lịch sử bidder khi bidderId null
    @Test
    void getBidderHistoryPagedShouldThrowWhenBidderIdIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            bidTransactionService.getBidderHistoryPaged(null, 1, 10);
        });

        assertValidationError(exception, ValidationErrorCode.MISSING_REQUIRED_FIELD);
    }

    // Không cho lấy lịch sử bidder khi bidderId rỗng
    @Test
    void getBidderHistoryPagedShouldThrowWhenBidderIdIsBlank() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            bidTransactionService.getBidderHistoryPaged("   ", 1, 10);
        });

        assertValidationError(exception, ValidationErrorCode.MISSING_REQUIRED_FIELD);
    }

    // Không cho page <= 0
    @Test
    void getBidderHistoryPagedShouldThrowWhenPageIsInvalid() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            bidTransactionService.getBidderHistoryPaged("bidder-1", 0, 10);
        });

        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    // Không cho pageSize <= 0
    @Test
    void getBidderHistoryPagedShouldThrowWhenPageSizeIsInvalid() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            bidTransactionService.getBidderHistoryPaged("bidder-1", 1, 0);
        });

        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    // DAO trả rỗng thì service trả PageDTO rỗng
    @Test
    void getBidderHistoryPagedShouldReturnEmptyPageWhenDaoReturnsEmptyList() {
        bidTransactionDAO.bidderBidsToReturn = List.of();

        PageDTO<BidTransactionDTO> result =
                bidTransactionService.getBidderHistoryPaged("bidder-1", 1, 10);

        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
        assertEquals(1, result.getCurrentPage());
        assertEquals(0, result.getTotalPages());
        assertEquals(0, result.getTotalElements());

        assertTrue(bidTransactionDAO.findByBidderIdPagedCalled);
        assertFalse(bidTransactionDAO.getTotalBidCountByBidderCalled);

        assertEquals("bidder-1", bidTransactionDAO.lastBidderId);
        assertEquals(10, bidTransactionDAO.lastLimit);
        assertEquals(0, bidTransactionDAO.lastOffset);
    }

    // Có lịch sử bid thì convert sang DTO và tính phân trang đúng
    @Test
    void getBidderHistoryPagedShouldConvertBidsToDTOs() {
        BidTransaction bid1 = sampleBid("bid-1", "bidder-1", "auction-1", 100.0, BidStatus.ACCEPTED);
        BidTransaction bid2 = sampleBid("bid-2", "bidder-1", "auction-2", 150.0, BidStatus.REFUNDED);

        bidTransactionDAO.bidderBidsToReturn = List.of(bid1, bid2);
        bidTransactionDAO.totalBidCountByBidder = 12;

        userDAO.usernamesById.put("bidder-1", "alice");

        PageDTO<BidTransactionDTO> result =
                bidTransactionService.getBidderHistoryPaged("bidder-1", 2, 5);

        assertEquals(2, result.getData().size());
        assertEquals(2, result.getCurrentPage());
        assertEquals(3, result.getTotalPages());
        assertEquals(12, result.getTotalElements());

        assertEquals("alice", result.getData().get(0).getBidderName());
        assertEquals(100.0, result.getData().get(0).getAmount());
        assertEquals("ACCEPTED", result.getData().get(0).getStatus());

        assertEquals("alice", result.getData().get(1).getBidderName());
        assertEquals(150.0, result.getData().get(1).getAmount());
        assertEquals("REFUNDED", result.getData().get(1).getStatus());

        assertTrue(bidTransactionDAO.getTotalBidCountByBidderCalled);
        assertEquals(5, bidTransactionDAO.lastLimit);
        assertEquals(5, bidTransactionDAO.lastOffset);
    }
}