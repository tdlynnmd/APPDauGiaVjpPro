package com.auction.service;

import com.auction.config.DatabaseConnection;
import com.auction.dao.AuctionDAO;
import com.auction.dao.BidTransactionDAO;
import com.auction.dao.ItemDAO;
import com.auction.dao.LogDAO;
import com.auction.dao.UserDAO;
import com.auction.dao.AutoBidDAO;
import com.auction.models.Auction.AutoBid;
import com.auction.dto.AuctionSummaryDTO;
import com.auction.enums.AuctionStatus;
import com.auction.enums.ItemStatus;
import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;
import com.auction.exception.AuctionErrorCode;
import com.auction.exception.AuctionException;
import com.auction.exception.ValidationErrorCode;
import com.auction.exception.ValidationException;
import com.auction.manage.AuctionManage;
import com.auction.manage.ConnectionManage;
import com.auction.manage.ProductManage;
import com.auction.models.Auction.Auction;
import com.auction.models.Item.Electronics;
import com.auction.models.Item.Item;
import com.auction.models.User.Bidder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuctionServiceTest {

    private AuctionService auctionService;

    private UserDAO userDAO;
    private AuctionDAO auctionDAO;
    private ItemDAO itemDAO;
    private BidTransactionDAO bidTransactionDAO;
    private LogDAO logDAO;

    private AuctionManage auctionManage;
    private ProductManage productManage;
    private ConnectionManage connectionManage; // Sử dụng thực thể thật để bypass lỗi Mockito trên JDK 25

    private MockedStatic<DatabaseConnection> mockedDbConnection;
    private Connection fakeConnection;

    @BeforeEach
    void setUp() throws Exception {
        auctionService = new AuctionService();

        userDAO = mock(UserDAO.class);
        auctionDAO = mock(AuctionDAO.class);
        itemDAO = mock(ItemDAO.class);
        bidTransactionDAO = mock(BidTransactionDAO.class);
        logDAO = mock(LogDAO.class);

        injectField(auctionService, "userDAO", userDAO);
        injectField(auctionService, "auctionDAO", auctionDAO);
        injectField(auctionService, "itemDAO", itemDAO);
        injectField(auctionService, "bidTransactionDAO", bidTransactionDAO);
        injectField(auctionService, "logDAO", logDAO);

        auctionManage = AuctionManage.getInstance();
        productManage = ProductManage.getInstance();

        // 🔥 ĐÃ FIX LỖI JDK 25: Bốc trực tiếp thực thể Singleton thật, loại bỏ hoàn toàn mock(ConnectionManage.class)
        connectionManage = ConnectionManage.getInstance();
        injectField(auctionService, "connectionManage", connectionManage);

        fakeConnection = new FakeDbConnection();

        mockedDbConnection = mockStatic(DatabaseConnection.class);
        mockedDbConnection.when(DatabaseConnection::getConnection).thenReturn(fakeConnection);

        clearRamCaches();
    }

    @AfterEach
    void tearDown() {
        if (mockedDbConnection != null) {
            mockedDbConnection.close();
        }
    }

    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void clearRamCaches() throws Exception {
        Field activeAuctionsField = AuctionManage.class.getDeclaredField("activeAuctions");
        activeAuctionsField.setAccessible(true);
        java.util.Map<?, ?> activeAuctions = (java.util.Map<?, ?>) activeAuctionsField.get(auctionManage);
        activeAuctions.clear();

        Field itemsField = ProductManage.class.getDeclaredField("items");
        itemsField.setAccessible(true);
        java.util.Map<?, ?> itemsMap = (java.util.Map<?, ?>) itemsField.get(productManage);
        itemsMap.clear();

        Field timeField = ProductManage.class.getDeclaredField("lastAccessedTime");
        timeField.setAccessible(true);
        java.util.Map<?, ?> timeMap = (java.util.Map<?, ?>) timeField.get(productManage);
        timeMap.clear();

        // Tự động quét sạch bộ đệm mạng kết nối để cô lập dữ liệu giữa các ca test độc lập
        for (Field field : ConnectionManage.class.getDeclaredFields()) {
            if (java.util.Map.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) field.get(connectionManage);
                if (map != null) map.clear();
            } else if (java.util.Collection.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                java.util.Collection<?> col = (java.util.Collection<?>) field.get(connectionManage);
                if (col != null) col.clear();
            }
        }
    }

    /**
     * 🔥 KỸ THUẬT PHÒNG THỬ NGHIỆM: Tự động nạp/xóa trạng thái Online của người dùng
     * vào ConnectionManage bằng Reflection để satisfies hàm isUserOnline() thuần Java.
     */
    @SuppressWarnings("unchecked")
    private void setBidderOnline(String userId, boolean online) throws Exception {
        for (Field field : ConnectionManage.class.getDeclaredFields()) {
            if (java.util.Map.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                java.util.Map<Object, Object> map = (java.util.Map<Object, Object>) field.get(connectionManage);
                if (map != null) {
                    if (online) {
                        map.put(userId, "ONLINE_TOKEN_PROXY"); // Nạp token giả lập qua mặt containsKey
                    } else {
                        map.remove(userId);
                    }
                }
            } else if (java.util.Collection.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                java.util.Collection<Object> col = (java.util.Collection<Object>) field.get(connectionManage);
                if (col != null) {
                    if (online) col.add(userId);
                    else col.remove(userId);
                }
            }
        }
    }

    private void assertAuctionError(AuctionException exception, AuctionErrorCode expectedError) {
        assertEquals(expectedError.getCode(), exception.getErrorCode());
    }

    private void assertValidationError(ValidationException exception, ValidationErrorCode expectedError) {
        assertEquals(expectedError.getCode(), exception.getErrorCode());
    }

    private Electronics sampleItem(String itemId) {
        Electronics item = new Electronics(
                "Laptop Dell", 12000000, "Laptop văn phòng", 2022, "seller-1", "dell.png", "Dell", 24
        );
        item.setId(itemId);
        return item;
    }

    private Auction sampleAuction(String auctionId, Item item) {
        Auction auction = new Auction(
                item, "seller-1", 100000, LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusHours(1)
        );
        auction.setId(auctionId);
        return auction;
    }

    private LocalDateTime startTime() {
        return LocalDateTime.now().plusMinutes(1);
    }

    private LocalDateTime endTime() {
        return LocalDateTime.now().plusHours(1);
    }

    // =========================================================
    // KHỐI KIỂM THỬ ĐẦU VÀO INPUT VALIDATION
    // =========================================================

    @Test
    void createAuctionShouldThrowWhenStepPriceIsZeroOrNegative() {
        String itemId = "item-1";
        ValidationException exception = assertThrows(ValidationException.class, () -> auctionService.createAuction(itemId, "seller-1", 0, startTime(), endTime()));
        assertValidationError(exception, ValidationErrorCode.INVALID_STEP_PRICE);
    }

    @Test
    void createAuctionShouldThrowWhenStartTimeIsInThePast() {
        String itemId = "item-1";
        LocalDateTime pastStartTime = LocalDateTime.now().minusMinutes(10);
        ValidationException exception = assertThrows(ValidationException.class, () -> auctionService.createAuction(itemId, "seller-1", 100000, pastStartTime, endTime()));
        assertValidationError(exception, ValidationErrorCode.START_TIME_IN_PAST);
    }

    // =========================================================
    // KHỐI KIỂM THỬ LOGIC NGHIỆP VỤ & DATABASE LINK
    // =========================================================

    @Test
    void createAuctionShouldThrowWhenItemNotFound() throws SQLException {
        String itemId = "missing-item";
        when(itemDAO.findById(itemId)).thenReturn(Optional.empty());

        AuctionException exception = assertThrows(AuctionException.class, () -> auctionService.createAuction(
                itemId, "seller-1", 100000, startTime(), endTime()
        ));

        assertAuctionError(exception, AuctionErrorCode.ITEM_NOT_FOUND);
        verify(auctionDAO, never()).insertAuction(any(Connection.class), any(Auction.class));
    }

    @Test
    void createAuctionShouldThrowWhenItemIsNotActive() throws SQLException {
        String itemId = "item-locked";
        Item item = new Electronics(
                itemId, "Laptop Dell", 12000000, "Laptop văn phòng", 2022, "seller-1", "dell.png",
                ItemStatus.INACTIVE, LocalDateTime.now(), "Dell", 24
        );

        when(itemDAO.findById(itemId)).thenReturn(Optional.of(item));

        AuctionException exception = assertThrows(AuctionException.class, () -> auctionService.createAuction(
                itemId, "seller-1", 100000, startTime(), endTime()
        ));

        assertAuctionError(exception, AuctionErrorCode.ITEM_IS_LOCKED);
        verify(auctionDAO, never()).insertAuction(any(Connection.class), any(Auction.class));
    }

    @Test
    void createAuctionShouldThrowWhenInsertAuctionFails() throws SQLException {
        String itemId = "item-insert-fail";
        Item item = sampleItem(itemId);

        when(itemDAO.findById(itemId)).thenReturn(Optional.of(item));
        when(auctionDAO.insertAuction(any(Connection.class), any(Auction.class))).thenReturn(false);

        AuctionException exception = assertThrows(AuctionException.class, () -> auctionService.createAuction(
                itemId, "seller-1", 100000, startTime(), endTime()
        ));

        assertAuctionError(exception, AuctionErrorCode.AUCTION_SAVE_FAILED);
        verify(auctionDAO).insertAuction(any(Connection.class), any(Auction.class));
        verify(itemDAO, never()).updateStatus(any(Connection.class), eq(itemId), anyString());
    }

    @Test
    void createAuctionShouldCreateAuctionAndLockItemWhenValid() throws SQLException {
        String itemId = "item-create-success";
        Item item = sampleItem(itemId);

        when(itemDAO.findById(itemId)).thenReturn(Optional.of(item));
        when(auctionDAO.insertAuction(any(Connection.class), any(Auction.class))).thenReturn(true);
        when(itemDAO.updateStatus(any(Connection.class), eq(itemId), eq(ItemStatus.INACTIVE.name()))).thenReturn(true);

        auctionService.createAuction(itemId, "seller-1", 100000, startTime(), endTime());

        verify(auctionDAO).insertAuction(any(Connection.class), any(Auction.class));
        verify(itemDAO).updateStatus(any(Connection.class), eq(itemId), eq(ItemStatus.INACTIVE.name()));
        assertEquals(ItemStatus.INACTIVE, item.getStatus());
    }

    // =========================================================================
    // 🔥 CÁC TEST CASE THÊM MỚI (Đã gỡ bỏ when().thenReturn() của hàm void gây lỗi)
    // =========================================================================

    @Test
    @org.junit.jupiter.api.DisplayName("Đặt giá thất bại khi người dùng Offline Socket")
    void processBidShouldThrowWhenBidderNotOnline() throws Exception {
        // 🔥 GIẢI PHÁP: Dựng một phòng đấu giá ảo mang ID "auction-1" đưa lên RAM
        // để vượt qua cửa ải kiểm tra getAuctionContext sạch sẽ trước.
        Item item = sampleItem("item-online-test");
        Auction auction = sampleAuction("auction-1", item);
        auctionManage.addAuction(auction);

        Bidder bidder = new Bidder("bidder-offline", "khach", "k@gmail.com", "P@ss123", UserRole.BIDDER, 5000.0, 0.0, UserStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        // Mồi Mock bypass hàng rào trích xuất ngữ cảnh bảo mật
        when(userDAO.findById(bidder.getId())).thenReturn(Optional.of(bidder));
        setBidderOnline("bidder-offline", false);

        AuctionException exception = assertThrows(AuctionException.class, () -> auctionService.processBid(bidder.getId(), "auction-1", 200000.0));
        assertAuctionError(exception, AuctionErrorCode.BIDDER_NOT_ONLINE);
    }

    @Test
    void processBidShouldThrowWhenBidAmountIsTooLow() throws Exception {
        Item item = sampleItem("item-bid");
        Auction auction = sampleAuction("auction-live", item);
        auction.setStatus(AuctionStatus.RUNNING);
        auctionManage.addAuction(auction);

        Bidder bidder = new Bidder("bidder-1", "khach", "k@gmail.com", "P@ss123", UserRole.BIDDER, 50000000.0, 0.0, UserStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        // Mồi Mock bypass hàng rào trích xuất ngữ cảnh bảo mật
        // Mồi Mock bypass hàng rào trích xuất ngữ cảnh bảo mật
        when(userDAO.findById(bidder.getId())).thenReturn(Optional.of(bidder));
        setBidderOnline("bidder-1", true);

        AuctionException exception = assertThrows(AuctionException.class, () -> auctionService.processBid(bidder.getId(), "auction-live", 12050000.0));
        assertAuctionError(exception, AuctionErrorCode.BID_AMOUNT_TOO_LOW);
    }

    @Test
    void processBidShouldRollbackInRamWhenDatabaseInsertFails() throws Exception {
        Item item = sampleItem("item-rollback-test");
        Auction auction = sampleAuction("auction-rollback", item);
        auction.setStatus(AuctionStatus.RUNNING);
        auctionManage.addAuction(auction);

        Bidder bidder = new Bidder("bidder-fail-db", "khach", "k@gmail.com", "P@ss123", UserRole.BIDDER, 50000000.0, 0.0, UserStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        when(userDAO.findById(bidder.getId())).thenReturn(Optional.of(bidder));
        setBidderOnline("bidder-fail-db", true); // Ép trạng thái Online trên RAM thật

        when(userDAO.freezeMoney(any(Connection.class), eq("bidder-fail-db"), anyDouble())).thenReturn(true);
        when(auctionDAO.updatePriceAndWinner(any(Connection.class), eq("auction-rollback"), anyDouble(), eq("bidder-fail-db"), anyString(), any(), anyDouble())).thenReturn(true);
        when(bidTransactionDAO.insertBid(any(Connection.class), any())).thenThrow(new SQLException("Crash Disk I/O"));

        assertThrows(AuctionException.class, () -> auctionService.processBid(bidder.getId(), "auction-rollback", 13000000.0));

        assertNull(auction.getHighestBidderId());
        assertEquals(12000000.0, auction.getCurrentPrice());
    }

    @Test
    void finalizeAuctionShouldDeductWinnerAndPaySellerWhenWinnerExists() throws Exception {
        Item item = sampleItem("item-final-1");
        Auction auction = sampleAuction("auction-final", item);
        auction.setHighestBidderId("winner-1");
        auctionManage.addAuction(auction);

        when(auctionDAO.findById("auction-final")).thenReturn(Optional.of(auction));
        when(userDAO.deductFrozenMoney(any(Connection.class), eq("winner-1"), anyDouble())).thenReturn(true);
        when(userDAO.addAvailableBalance(any(Connection.class), eq("seller-1"), anyDouble())).thenReturn(true);
        when(itemDAO.updateStatus(any(Connection.class), eq("item-final-1"), eq(ItemStatus.SOLD.name()))).thenReturn(true);

        assertDoesNotThrow(() -> auctionService.finalizeAuction("auction-final"));

        verify(userDAO).deductFrozenMoney(any(Connection.class), eq("winner-1"), anyDouble());
        verify(userDAO).addAvailableBalance(any(Connection.class), eq("seller-1"), anyDouble());
        verify(itemDAO).updateStatus(any(Connection.class), eq("item-final-1"), eq(ItemStatus.SOLD.name()));
        verify(auctionDAO).updateStatus(any(Connection.class), eq("auction-final"), eq(AuctionStatus.FINISHED.name()));

        assertNull(auctionManage.getAuctionById("auction-final"));
    }

    @Test
    void finalizeAuctionShouldReleaseItemWhenNoWinner() throws Exception {
        Item item = sampleItem("item-final-2");
        Auction auction = sampleAuction("auction-no-winner", item);
        auction.setHighestBidderId(null);
        auctionManage.addAuction(auction);

        when(auctionDAO.findById("auction-no-winner")).thenReturn(Optional.of(auction));
        when(itemDAO.updateStatus(any(Connection.class), eq("item-final-2"), eq(ItemStatus.ACTIVE.name()))).thenReturn(true);

        assertDoesNotThrow(() -> auctionService.finalizeAuction("auction-no-winner"));

        verify(itemDAO).updateStatus(any(Connection.class), eq("item-final-2"), eq(ItemStatus.ACTIVE.name()));
        verify(auctionDAO).updateStatus(any(Connection.class), eq("auction-no-winner"), eq(AuctionStatus.FINISHED.name()));
        assertNull(auctionManage.getAuctionById("auction-no-winner"));
    }

    @Test
    void leaveAuctionShouldThrowWhenLeadingBidderTriesToLeave() {
        Item item = sampleItem("item-leave-test");
        Auction auction = sampleAuction("auction-leave", item);
        auction.setStatus(AuctionStatus.RUNNING);
        auction.setHighestBidderId("bidder-leading-123");
        auctionManage.addAuction(auction);

        Bidder bidder = new Bidder("bidder-leading-123", "khach", "k@gmail.com", "P@ss123", UserRole.BIDDER, 5000.0, 0.0, UserStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        // Mồi Mock bypass hàng rào trích xuất ngữ cảnh bảo mật
        when(userDAO.findById(bidder.getId())).thenReturn(Optional.of(bidder));
        AuctionException exception = assertThrows(AuctionException.class, () -> auctionService.leaveAuction(bidder.getId(), "auction-leave", null));
        assertAuctionError(exception, AuctionErrorCode.CANNOT_UNWATCH_LEADING_AUCTION);
    }

    @Test
    void cancelAuctionShouldRefundHighestBidderAndInsertAuditLog() throws Exception {
        Item item = sampleItem("item-cancel-test");
        Auction auction = sampleAuction("auction-cancel", item);
        auction.setStatus(AuctionStatus.OPEN);
        auction.setHighestBidderId("winner-to-refund");
        auctionManage.addAuction(auction);

        when(auctionDAO.findById("auction-cancel")).thenReturn(Optional.of(auction));
        when(itemDAO.updateStatus(any(Connection.class), eq("item-cancel-test"), eq(ItemStatus.ACTIVE.name()))).thenReturn(true);

        assertDoesNotThrow(() -> auctionService.cancelAuction("auction-cancel", "admin-007",UserRole.ADMIN, "Sản phẩm nằm trong danh mục cấm đăng bán"));

        verify(userDAO).unfreezeMoney(any(Connection.class), eq("winner-to-refund"), anyDouble());
        verify(bidTransactionDAO).updateStatusToRefunded(any(Connection.class), eq("auction-cancel"), eq("winner-to-refund"));
        verify(auctionDAO).updateStatus(any(Connection.class), eq("auction-cancel"), eq(AuctionStatus.CANCELED.name()));
        verify(itemDAO).updateStatus(any(Connection.class), eq("item-cancel-test"), eq(ItemStatus.ACTIVE.name()));
        verify(logDAO).insertLog(any(Connection.class), anyString(), eq("admin-007"), anyString(), eq("AUCTION"), eq("auction-cancel"));
        assertNull(auctionManage.getAuctionById("auction-cancel"));
    }

    // =========================================================
    // KHỐI KIỂM THỬ THEO DÕI DANH SÁCH (SUMMARY MAPPING)
    // =========================================================

    @Test
    void getJoinedAuctionsSummaryShouldReturnEmptyListWhenBidderJoinedNothing() {
        Bidder bidder = new Bidder("bidder1", "bidder1@example.com", "hashed-password");
        // Mồi Mock bypass hàng rào trích xuất ngữ cảnh bảo mật
        when(userDAO.findById(bidder.getId())).thenReturn(Optional.of(bidder));
        List<AuctionSummaryDTO> result = auctionService.getJoinedAuctionsSummary(bidder.getId());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getJoinedAuctionsSummaryShouldLoadAuctionFromDatabaseWhenNotInRam() {
        Bidder bidder = new Bidder("bidder1", "bidder1@example.com", "hashed-password");
        bidder.addJoinedAuction("auction-1");

        Item item = sampleItem("item-summary-1");
        Auction auction = sampleAuction("auction-1", item);

        // Mồi Mock bypass hàng rào trích xuất ngữ cảnh bảo mật
        when(userDAO.findById(bidder.getId())).thenReturn(Optional.of(bidder));
        when(auctionDAO.findById("auction-1")).thenReturn(Optional.of(auction));

        List<AuctionSummaryDTO> result = auctionService.getJoinedAuctionsSummary(bidder.getId());

        assertEquals(1, result.size());
        AuctionSummaryDTO dto = result.getFirst();
        assertEquals("auction-1", dto.getAuctionId());
        assertEquals("Laptop Dell", dto.getItemName());
        assertEquals(auction.getCurrentPrice(), dto.getCurrentPrice());
    }

    @Test
    void getJoinedAuctionsSummaryShouldSkipMissingAuction() {
        Bidder bidder = new Bidder("bidder1", "bidder1@example.com", "hashed-password");
        bidder.addJoinedAuction("missing-auction");

        // Mồi Mock bypass hàng rào trích xuất ngữ cảnh bảo mật
        when(userDAO.findById(bidder.getId())).thenReturn(Optional.of(bidder));
        when(auctionDAO.findById("missing-auction")).thenReturn(Optional.empty());

        List<AuctionSummaryDTO> result = auctionService.getJoinedAuctionsSummary(bidder.getId());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getJoinedAuctionsSummaryShouldUseFallbackNameWhenAuctionItemIsNull() {
        Bidder bidder = new Bidder("bidder1", "bidder1@example.com", "hashed-password");
        bidder.addJoinedAuction("auction-no-item");

        Item item = sampleItem("item-no-object");
        Auction auction = sampleAuction("auction-no-item", item);
        auction.setItem(null);

        // Mồi Mock bypass hàng rào trích xuất ngữ cảnh bảo mật
        when(userDAO.findById(bidder.getId())).thenReturn(Optional.of(bidder));
        when(auctionDAO.findById("auction-no-item")).thenReturn(Optional.of(auction));

        List<AuctionSummaryDTO> result = auctionService.getJoinedAuctionsSummary(bidder.getId());

        assertEquals(1, result.size());
        assertEquals("Vật phẩm #" + auction.getItemId(), result.getFirst().getItemName());
    }

    @Test
    void getAllActiveAuctionsShouldReturnOnlyOpenOrRunningAuctions() throws SQLException {
        Item item = sampleItem("item-active-1");
        Auction auction = sampleAuction("auction-active-1", item);
        auction.setStatus(AuctionStatus.RUNNING);

        when(auctionDAO.findByStatuses(any(Connection.class), any())).thenReturn(List.of(auction));

        List<AuctionSummaryDTO> result = auctionService.getAllActiveAuctions();
        assertNotNull(result);

        for (AuctionSummaryDTO dto : result) {
            assertTrue(dto.getStatus().equals(AuctionStatus.OPEN.name()) || dto.getStatus().equals(AuctionStatus.RUNNING.name()));
        }
    }

    @Test
    void loadAuctionsToRAMShouldLoadOpenAndRunningAuctionsFromDatabase() throws Exception {
        Item item = sampleItem("item-load-1");
        Auction auction = sampleAuction("auction-load-1", item);

        when(auctionDAO.findByStatuses(any(Connection.class), any())).thenReturn(List.of(auction));
        when(itemDAO.findById(auction.getItemId())).thenReturn(Optional.of(item));

        // 🔥 FIX: Mock autoBidDAO để tránh NPE khi FakeDbConnection.prepareStatement() trả null
        AutoBidDAO autoBidDAO = mock(AutoBidDAO.class);
        injectField(auctionService, "autoBidDAO", autoBidDAO);
        when(autoBidDAO.findActiveByAuctionId(any(Connection.class), eq("auction-load-1"))).thenReturn(List.of());

        auctionService.loadAuctionsToRAM();

        verify(auctionDAO).findByStatuses(any(Connection.class), any());
        assertNotNull(auction.getItem());
        assertEquals("Laptop Dell", auction.getItem().getName());
        assertNotNull(auctionManage.getAuctionById("auction-load-1"));
    }

    @Test
    void setupAutoBidShouldCreateAutoBidAndTriggerAutoBidding() throws Exception {
        Item item = sampleItem("item-autobid");
        Auction auction = sampleAuction("auction-autobid", item);
        auction.setStatus(AuctionStatus.RUNNING);
        auctionManage.addAuction(auction);

        Bidder bidder = new Bidder("bidder-autobid", "khach", "k@gmail.com", "P@ss123", UserRole.BIDDER, 50000000.0, 0.0, UserStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        when(userDAO.findById(bidder.getId())).thenReturn(Optional.of(bidder));
        setBidderOnline(bidder.getId(), true);

        AutoBidDAO autoBidDAO = mock(AutoBidDAO.class);
        injectField(auctionService, "autoBidDAO", autoBidDAO);
        when(autoBidDAO.findActiveByUserAndAuction(any(Connection.class), eq(bidder.getId()), eq("auction-autobid"))).thenReturn(Optional.empty());
        when(autoBidDAO.insertOrUpdate(any(Connection.class), any(AutoBid.class))).thenReturn(true);

        // 🔥 FIX: Stub đầy đủ dependencies cho triggerAutoBids → executeBidInternal chạy thành công
        // Khi setupAutoBid kích hoạt triggerAutoBids, nó gọi executeBidInternal cần:
        // freezeMoney=true, updatePriceAndWinner=true, insertBid=true
        when(userDAO.freezeMoney(any(Connection.class), eq(bidder.getId()), anyDouble())).thenReturn(true);
        when(auctionDAO.updatePriceAndWinner(any(Connection.class), eq("auction-autobid"), anyDouble(), eq(bidder.getId()), anyString(), any(LocalDateTime.class), anyDouble())).thenReturn(true);
        when(bidTransactionDAO.insertBid(any(Connection.class), any())).thenReturn(true);

        assertDoesNotThrow(() -> auctionService.setupAutoBid(bidder.getId(), "auction-autobid", 15000000.0, 200000.0));

        verify(autoBidDAO).insertOrUpdate(any(Connection.class), any(AutoBid.class));
        verify(auctionDAO).updatePriceAndWinner(any(Connection.class), eq("auction-autobid"), eq(12200000.0), eq(bidder.getId()), anyString(), any(LocalDateTime.class), anyDouble());
        assertEquals(12200000.0, auction.getCurrentPrice());
        assertEquals(1, auction.getAutoBidsQueue().size());
    }

    @Test
    void setupAutoBidShouldNotBidAboveMaxBid() throws Exception {
        Item item = sampleItem("item-autobid-max");
        Auction auction = sampleAuction("auction-autobid-max", item);
        auction.setStatus(AuctionStatus.RUNNING);
        auctionManage.addAuction(auction);

        Bidder bidder = new Bidder("bidder-autobid-max", "khach", "k@gmail.com", "P@ss123", UserRole.BIDDER, 50000000.0, 0.0, UserStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        when(userDAO.findById(bidder.getId())).thenReturn(Optional.of(bidder));
        setBidderOnline(bidder.getId(), true);

        AutoBidDAO autoBidDAO = mock(AutoBidDAO.class);
        injectField(auctionService, "autoBidDAO", autoBidDAO);
        when(autoBidDAO.findActiveByUserAndAuction(any(Connection.class), eq(bidder.getId()), eq("auction-autobid-max"))).thenReturn(Optional.empty());
        when(autoBidDAO.insertOrUpdate(any(Connection.class), any(AutoBid.class))).thenReturn(true);

        when(userDAO.freezeMoney(any(Connection.class), eq(bidder.getId()), anyDouble())).thenReturn(true);
        when(auctionDAO.updatePriceAndWinner(any(Connection.class), eq("auction-autobid-max"), anyDouble(), eq(bidder.getId()), anyString(), any(LocalDateTime.class), anyDouble())).thenReturn(true);
        when(bidTransactionDAO.insertBid(any(Connection.class), any())).thenReturn(true);

        assertDoesNotThrow(() -> auctionService.setupAutoBid(bidder.getId(), "auction-autobid-max", 12300000.0, 1000000.0));

        verify(auctionDAO).updatePriceAndWinner(any(Connection.class), eq("auction-autobid-max"), eq(12300000.0), eq(bidder.getId()), anyString(), any(LocalDateTime.class), anyDouble());
        assertEquals(12300000.0, auction.getCurrentPrice());
    }

    @Test
    void autoBidShouldKeepEarlierRegistrationAheadWhenMaxBidTies() throws Exception {
        Item item = sampleItem("item-autobid-tie");
        Auction auction = sampleAuction("auction-autobid-tie", item);
        auction.setStatus(AuctionStatus.RUNNING);
        auctionManage.addAuction(auction);

        Bidder manualBidder = new Bidder("bidder-manual-tie", "manual", "m@gmail.com", "P@ss123", UserRole.BIDDER, 50000000.0, 0.0, UserStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        Bidder earlierBidder = new Bidder("bidder-earlier-auto", "earlier", "e@gmail.com", "P@ss123", UserRole.BIDDER, 50000000.0, 0.0, UserStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        Bidder laterBidder = new Bidder("bidder-later-auto", "later", "l@gmail.com", "P@ss123", UserRole.BIDDER, 50000000.0, 0.0, UserStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());

        when(userDAO.findById(manualBidder.getId())).thenReturn(Optional.of(manualBidder));
        when(userDAO.findById(earlierBidder.getId())).thenReturn(Optional.of(earlierBidder));
        when(userDAO.findById(laterBidder.getId())).thenReturn(Optional.of(laterBidder));
        setBidderOnline(manualBidder.getId(), true);

        LocalDateTime baseTime = LocalDateTime.now().minusMinutes(10);
        auction.addOrUpdateAutoBidInRam(new AutoBid("auto-earlier", earlierBidder.getId(), auction.getId(), 12300000.0, 100000.0, true, baseTime));
        auction.addOrUpdateAutoBidInRam(new AutoBid("auto-later", laterBidder.getId(), auction.getId(), 12300000.0, 1000000.0, true, baseTime.plusMinutes(1)));

        when(userDAO.freezeMoney(any(Connection.class), anyString(), anyDouble())).thenReturn(true);
        when(auctionDAO.updatePriceAndWinner(any(Connection.class), eq("auction-autobid-tie"), anyDouble(), anyString(), anyString(), any(LocalDateTime.class), anyDouble())).thenReturn(true);
        when(bidTransactionDAO.insertBid(any(Connection.class), any())).thenReturn(true);

        assertDoesNotThrow(() -> auctionService.processBid(manualBidder.getId(), "auction-autobid-tie", 12100000.0));

        assertEquals(earlierBidder.getId(), auction.getHighestBidderId());
    }

    @Test
    void setupAutoBidShouldKeepOriginalPriorityWhenUpdatingActiveConfig() throws Exception {
        Item item = sampleItem("item-autobid-update");
        Auction auction = sampleAuction("auction-autobid-update", item);
        auction.setStatus(AuctionStatus.OPEN);
        auctionManage.addAuction(auction);

        Bidder bidder = new Bidder("bidder-autobid-update", "khach", "k@gmail.com", "P@ss123", UserRole.BIDDER, 50000000.0, 0.0, UserStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        when(userDAO.findById(bidder.getId())).thenReturn(Optional.of(bidder));

        LocalDateTime originalCreatedAt = LocalDateTime.now().minusMinutes(30);
        AutoBid existingAutoBid = new AutoBid("existing-autobid-id", bidder.getId(), "auction-autobid-update", 13000000.0, 100000.0, true, originalCreatedAt);

        AutoBidDAO autoBidDAO = mock(AutoBidDAO.class);
        injectField(auctionService, "autoBidDAO", autoBidDAO);
        when(autoBidDAO.findActiveByUserAndAuction(any(Connection.class), eq(bidder.getId()), eq("auction-autobid-update"))).thenReturn(Optional.of(existingAutoBid));
        when(autoBidDAO.insertOrUpdate(any(Connection.class), any(AutoBid.class))).thenReturn(true);

        assertDoesNotThrow(() -> auctionService.setupAutoBid(bidder.getId(), "auction-autobid-update", 15000000.0, 300000.0));

        org.mockito.ArgumentCaptor<AutoBid> captor = org.mockito.ArgumentCaptor.forClass(AutoBid.class);
        verify(autoBidDAO).insertOrUpdate(any(Connection.class), captor.capture());
        assertEquals(existingAutoBid.getId(), captor.getValue().getId());
        assertEquals(originalCreatedAt, captor.getValue().getCreatedAt());
        assertEquals(15000000.0, captor.getValue().getMaxBid());
        assertEquals(300000.0, captor.getValue().getIncrement());
    }

    @Test
    void cancelAutoBidShouldDisableAndRemoveFromRam() throws Exception {
        Item item = sampleItem("item-autobid-cancel");
        Auction auction = sampleAuction("auction-autobid-cancel", item);
        auction.setStatus(AuctionStatus.RUNNING);
        auctionManage.addAuction(auction);

        Bidder bidder = new Bidder("bidder-autobid-cancel", "khach", "k@gmail.com", "P@ss123", UserRole.BIDDER, 50000000.0, 0.0, UserStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        when(userDAO.findById(bidder.getId())).thenReturn(Optional.of(bidder));

        AutoBidDAO autoBidDAO = mock(AutoBidDAO.class);
        injectField(auctionService, "autoBidDAO", autoBidDAO);

        AutoBid autoBid = new AutoBid(bidder.getId(), "auction-autobid-cancel", 15000000.0, 200000.0);
        auction.addOrUpdateAutoBidInRam(autoBid);
        assertEquals(1, auction.getAutoBidsQueue().size());

        when(autoBidDAO.findActiveByUserAndAuction(any(Connection.class), eq(bidder.getId()), eq("auction-autobid-cancel"))).thenReturn(Optional.of(autoBid));
        when(autoBidDAO.disableAutoBid(any(Connection.class), eq(autoBid.getId()))).thenReturn(true);

        assertDoesNotThrow(() -> auctionService.cancelAutoBid(bidder.getId(), "auction-autobid-cancel"));

        verify(autoBidDAO).disableAutoBid(any(Connection.class), eq(autoBid.getId()));
        assertEquals(0, auction.getAutoBidsQueue().size());
    }

    private static class FakeDbConnection implements Connection {
        @Override public void setAutoCommit(boolean autoCommit) throws SQLException {}
        @Override public boolean getAutoCommit() throws SQLException { return true; }
        @Override public void commit() throws SQLException {}
        @Override public void rollback() throws SQLException {}
        @Override public void close() throws SQLException {}
        @Override public boolean isClosed() throws SQLException { return false; }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return false; }
        @Override public java.sql.Statement createStatement() throws SQLException { return null; }
        @Override public java.sql.PreparedStatement prepareStatement(String sql) throws SQLException { return null; }
        @Override public java.sql.CallableStatement prepareCall(String sql) throws SQLException { return null; }
        @Override public String nativeSQL(String sql) throws SQLException { return null; }
        @Override public java.sql.DatabaseMetaData getMetaData() throws SQLException { return null; }
        @Override public void setReadOnly(boolean readOnly) throws SQLException {}
        @Override public boolean isReadOnly() throws SQLException { return false; }
        @Override public void setCatalog(String catalog) throws SQLException {}
        @Override public String getCatalog() throws SQLException { return null; }
        @Override public void setTransactionIsolation(int level) throws SQLException {}
        @Override public int getTransactionIsolation() throws SQLException { return Connection.TRANSACTION_NONE; }
        @Override public java.sql.SQLWarning getWarnings() throws SQLException { return null; }
        @Override public void clearWarnings() throws SQLException {}
        @Override public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException { return null; }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException { return null; }
        @Override public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException { return null; }
        @Override public java.util.Map<String, Class<?>> getTypeMap() throws SQLException { return null; }
        @Override public void setTypeMap(java.util.Map<String, Class<?>> map) throws SQLException {}
        @Override public void setHoldability(int holdability) throws SQLException {}
        @Override public int getHoldability() throws SQLException { return 0; }
        @Override public java.sql.Savepoint setSavepoint() throws SQLException { return null; }
        @Override public java.sql.Savepoint setSavepoint(String name) throws SQLException { return null; }
        @Override public void rollback(java.sql.Savepoint savepoint) throws SQLException {}
        @Override public void releaseSavepoint(java.sql.Savepoint savepoint) throws SQLException {}
        @Override public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { return null; }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { return null; }
        @Override public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { return null; }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException { return null; }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException { return null; }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException { return null; }
        @Override public java.sql.Clob createClob() throws SQLException { return null; }
        @Override public java.sql.Blob createBlob() throws SQLException { return null; }
        @Override public java.sql.NClob createNClob() throws SQLException { return null; }
        @Override public java.sql.SQLXML createSQLXML() throws SQLException { return null; }
        @Override public boolean isValid(int timeout) throws SQLException { return true; }
        @Override public void setClientInfo(String name, String value) throws java.sql.SQLClientInfoException {}
        @Override public void setClientInfo(java.util.Properties properties) throws java.sql.SQLClientInfoException {}
        @Override public String getClientInfo(String name) throws SQLException { return null; }
        @Override public java.util.Properties getClientInfo() throws SQLException { return null; }
        @Override public java.sql.Array createArrayOf(String typeName, Object[] elements) throws SQLException { return null; }
        @Override public java.sql.Struct createStruct(String typeName, Object[] attributes) throws SQLException { return null; }
        @Override public void setSchema(String schema) throws SQLException {}
        @Override public String getSchema() throws SQLException { return null; }
        @Override public void abort(java.util.concurrent.Executor executor) throws SQLException {}
        @Override public void setNetworkTimeout(java.util.concurrent.Executor executor, int milliseconds) throws SQLException {}
        @Override public int getNetworkTimeout() throws SQLException { return 0; }
    }
}
