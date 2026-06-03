package com.auction.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auction.dao.*;
import com.auction.dao.impl.*;
import com.auction.dto.AuctionDetailDTO;
import com.auction.dto.AuctionSummaryDTO;
import com.auction.dto.BidTransactionDTO;
import com.auction.dto.UpdateAuctionRequest;
import com.auction.enums.AuctionStatus;
import com.auction.enums.ItemStatus;
import com.auction.enums.UserRole;
import com.auction.exception.*;
import com.auction.manage.AuctionManage;
import com.auction.manage.ConnectionManage;
import com.auction.manage.LiveRoomManage;
import com.auction.manage.ProductManage;
import com.auction.models.Auction.Auction;
import com.auction.models.User.Seller;
import com.auction.models.User.User;
import com.auction.network.ClientSession;
import com.auction.models.Auction.BidTransaction;
import com.auction.models.Auction.AutoBid;
import com.auction.models.Item.Item;
import com.auction.models.User.Bidder;
import com.auction.event.AuctionEvent;
import com.auction.event.AuctionEventBus;
import com.auction.event.AuctionEventType;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class AuctionService {
    private static final Logger log = LoggerFactory.getLogger(AuctionService.class);
    private final AuctionManage auctionManage = AuctionManage.getInstance();
    private final ConnectionManage connectionManage = ConnectionManage.getInstance();
    private final ProductManage productManage = ProductManage.getInstance();

    private final UserDAO userDAO = new UserDAOImpl();
    private final AuctionDAO auctionDAO = new AuctionDAOImpl();
    private final ItemDAO itemDAO = new ItemDAOImpl();
    private final BidTransactionDAO bidTransactionDAO = new BidTransactionDAOImpl();
    private final LogDAO logDAO = new LogDAOImpl();

    /**
     * TẠO PHÒNG ĐẤU GIÁ MỚI (Giữ nguyên)
     */
    public void createAuction(String itemId, String sellerId,
                              double stepPrice, LocalDateTime startTime, LocalDateTime endTime) {
        validateAuctionInput(itemId, sellerId, stepPrice, startTime, endTime);

        synchronized (itemId.intern()) {
            Item liveItem = productManage.getProduct(itemId);
            if (liveItem == null) {
                liveItem = itemDAO.findById(itemId)
                        .orElseThrow(() -> new AuctionException(AuctionErrorCode.ITEM_NOT_FOUND, "Product item does not exist in the warehouse."));
                productManage.addProduct(liveItem);
            }

            if (liveItem.getStatus() != ItemStatus.ACTIVE) {
                throw new AuctionException(AuctionErrorCode.ITEM_IS_LOCKED, "This item is currently locked because it is live on the floor or already sold.");
            }

            if (!liveItem.getSellerId().equals(sellerId)) {
                throw new AuthorizationException(AuthorizationErrorCode.RESOURCE_OWNERSHIP_VIOLATION, "Access denied: You do not own this item resource.");
            }

            Auction newAuction = new Auction(liveItem, sellerId, stepPrice, startTime, endTime);

            try (Connection conn = com.auction.config.DatabaseConnection.getConnection()) {
                conn.setAutoCommit(false);

                boolean isAuctionSaved = auctionDAO.insertAuction(conn, newAuction);
                if (!isAuctionSaved) {
                    throw new AuctionException(AuctionErrorCode.AUCTION_SAVE_FAILED, "Internal data persistence failed for creating auction.");
                }

                boolean isItemUpdated = itemDAO.updateStatus(conn, itemId, ItemStatus.INACTIVE.name());
                if (!isItemUpdated) {
                    throw new AuctionException(AuctionErrorCode.UPDATE_FAILED, "Database update operation failed for item status.");
                }

                conn.commit();
            } catch (SQLException e) {
                throw new AuctionException(AuctionErrorCode.AUCTION_SAVE_FAILED, "Transaction failed at createAuction scope: " + e.getMessage());
            }

            liveItem.setStatus(ItemStatus.INACTIVE);
            auctionManage.addAuction(newAuction);
        }
    }

    private void validateAuctionInput(String itemId, String sellerId, double stepPrice,
                                      LocalDateTime startTime, LocalDateTime endTime) {
        if (itemId == null || itemId.trim().isEmpty() || sellerId == null || sellerId.trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.MISSING_REQUIRED_FIELD, "Item ID and Seller ID cannot be empty.");
        }
        if (startTime == null || endTime == null) {
            throw new ValidationException(ValidationErrorCode.MISSING_REQUIRED_FIELD, "Start time and End time cannot be null.");
        }
        if (stepPrice <= 0) {
            throw new ValidationException(ValidationErrorCode.INVALID_STEP_PRICE, "Step price must be greater than zero.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (startTime.isBefore(now.minusSeconds(5))) {
            throw new ValidationException(ValidationErrorCode.START_TIME_IN_PAST, "Auction start time cannot be in the past.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new ValidationException(ValidationErrorCode.INVALID_END_TIME, "End time must be after start time.");
        }
    }

    /**
     * THỰC THI ĐẶT GIÁ REALTIME (Đã chuyển đổi sang bốc Bidder ID nội bộ)
     */
    public void processBid(String bidderId, String auctionId, double amount) {
        Bidder bidder = getBidderContext(bidderId);
        validateBidInput(bidder, auctionId, amount);

        Auction auction = getAuctionContext(auctionId);

        if (!connectionManage.isUserOnline(bidder.getId())) {
            throw new AuctionException(AuctionErrorCode.BIDDER_NOT_ONLINE);
        }

        synchronized (auction) {
            if (auction.getStatus() != AuctionStatus.RUNNING) {
                throw new AuctionException(AuctionErrorCode.AUCTION_NOT_RUNNING);
            }

            if (amount < auction.getCurrentPrice() + auction.getLiveStepPrice()) {
                throw new AuctionException(AuctionErrorCode.BID_AMOUNT_TOO_LOW);
            }

            // Thực thi đặt giá thủ công cho User
            executeBidInternal(bidder, auction, amount);

            // Kích hoạt chuỗi đấu giá tự động để tranh chấp thầu
            triggerAutoBids(auction);
        }
    }

    private void executeBidInternal(Bidder bidder, Auction auction, double amount) {
        String oldHighestBidderId = auction.getHighestBidderId();
        String oldWinningBidId = auction.getCurrentWinningBidId();
        double oldPrice = auction.getCurrentPrice();
        LocalDateTime oldEndTime = auction.getEndTime();

        String newBidId = UUID.randomUUID().toString();
        Connection conn = null;

        try {
            conn = com.auction.config.DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            boolean freezeSuccess = userDAO.freezeMoney(conn, bidder.getId(), amount);
            if (!freezeSuccess) {
                throw new WalletException(WalletErrorCode.INSUFFICIENT_BALANCE);
            }

            BidTransaction resultBid = auction.placeBid(bidder, amount, newBidId);

            boolean updateAuctionDB = auctionDAO.updatePriceAndWinner(
                    conn, auction.getId(), amount, bidder.getId(), newBidId, auction.getEndTime(), auction.getLiveStepPrice()
            );
            if (!updateAuctionDB) {
                throw new AuctionException(AuctionErrorCode.BID_SAVE_FAILED, "Sync price to database failed.");
            }

            boolean insertedBid = bidTransactionDAO.insertBid(conn, resultBid);
            if (!insertedBid) {
                throw new AuctionException(AuctionErrorCode.BID_SAVE_FAILED, "Failed to persist bid transaction.");
            }

            if (oldHighestBidderId != null) {
                userDAO.unfreezeMoney(conn, oldHighestBidderId, oldPrice);
                if (oldWinningBidId != null) {
                    bidTransactionDAO.updateStatusByBidId(conn, oldWinningBidId, com.auction.enums.BidStatus.REFUNDED.name());
                }
            }

            if (!bidder.getJoinedAuctionIds().contains(auction.getId())) {
                userDAO.addJoinedAuction(conn, bidder.getId(), auction.getId());
            }

            conn.commit();
            System.out.println("[DB Transaction] ✅ Commit thành công luồng đặt giá xuống Database cho: " + bidder.getUsername() + ", Giá: " + amount);

            // 1. Đồng bộ RAM cho Người đặt giá hiện tại
            synchronized (bidder.getId().intern()) {
                if (oldHighestBidderId != null && oldHighestBidderId.equals(bidder.getId())) {
                    bidder.unfreeze(oldPrice);
                }
                bidder.freeze(amount);
                if (!bidder.getJoinedAuctionIds().contains(auction.getId())) {
                    bidder.addJoinedAuction(auction.getId());
                }
            }

            // 2. Đồng bộ RAM cho người bị vượt giá cũ
            if (oldHighestBidderId != null && !oldHighestBidderId.equals(bidder.getId())) {
                User oldRamUser = com.auction.manage.UserManage.getInstance().getUser(oldHighestBidderId);
                if (oldRamUser instanceof Bidder oldBidderLive) {
                    synchronized (oldHighestBidderId.intern()) {
                        oldBidderLive.setAvailableBalance(oldBidderLive.getAvailableBalance() + oldPrice);
                        oldBidderLive.setFrozenBalance(oldBidderLive.getFrozenBalance() - oldPrice);
                        System.out.println("[RAM Sync] 🔄 Đã hoàn tiền trên RAM cho người bị vượt giá cũ: " + oldHighestBidderId);
                    }
                }
            }

            // Gửi cập nhật ví qua socket
            try {
                connectionManage.sendBalanceUpdate(bidder.getId(), bidder.getAvailableBalance(), bidder.getFrozenBalance());
            } catch (Exception ex) {
                System.err.println("[AuctionService] Lỗi gửi WALLET_UPDATE cho bidder mới: " + ex.getMessage());
            }
            if (oldHighestBidderId != null && !oldHighestBidderId.equals(bidder.getId())) {
                User oldRamUser = com.auction.manage.UserManage.getInstance().getUser(oldHighestBidderId);
                if (oldRamUser instanceof Bidder oldBidderLive) {
                    try {
                        connectionManage.sendBalanceUpdate(oldHighestBidderId, oldBidderLive.getAvailableBalance(), oldBidderLive.getFrozenBalance());
                    } catch (Exception ex) {
                        System.err.println("[AuctionService] Lỗi gửi WALLET_UPDATE cho bidder cũ: " + ex.getMessage());
                    }
                }
            }

            // 3. Đóng gói payload chứa thông tin Anti-sniping
            BidTransactionDTO bidData = new BidTransactionDTO(
                    bidder.getUsername(), amount, resultBid.getTime(), com.auction.enums.BidStatus.ACCEPTED.name(),
                    auction.getEndTime(), auction.getLiveStepPrice()
            );
            bidData.setBidId(resultBid.getId());
            bidData.setAuctionId(auction.getId());

            AuctionEvent bidEvent = new AuctionEvent(auction.getId(), AuctionEventType.NEW_BID, bidData);
            AuctionEventBus.getInstance().publish(bidEvent);

        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }

            auction.rollbackBidInRam(oldHighestBidderId, oldPrice, oldEndTime);

            if (e instanceof com.auction.exception.BaseException) throw (com.auction.exception.BaseException) e;
            throw new AuctionException(AuctionErrorCode.BID_SAVE_FAILED, "Fatal crash during executeBidInternal: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                } finally {
                    try {
                        conn.close();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }


    public void triggerAutoBids(Auction auction) {
        int loops = 0;
        // Giới hạn tối đa 100 lượt đấu giá tự động liên tiếp để tránh quá tải server
        while (loops < 100) {
            double minBidRequired = auction.getCurrentPrice() + auction.getLiveStepPrice();
            List<AutoBid> activeAutoBids = snapshotActiveAutoBids(auction);
            AutoBid challenger = selectAutoBidChallenger(activeAutoBids, auction.getHighestBidderId(), minBidRequired);

            if (challenger == null) {
                break;
            }

            AutoBid currentLeaderAutoBid = findAutoBidByUser(activeAutoBids, auction.getHighestBidderId());
            double bidAmount = calculateAutoBidAmount(auction, challenger, currentLeaderAutoBid, minBidRequired);
            if (bidAmount < minBidRequired) {
                break;
            }

            try {
                Bidder challengerBidder = getBidderContext(challenger.getUserId());
                executeBidInternal(challengerBidder, auction, bidAmount);
            } catch (Exception e) {
                log.warn("[Auto-Bidding] ❌ Lượt thầu tự động của user {} thất bại: {}", challenger.getUserId(), e.getMessage());
                // Vô hiệu hóa auto bid nếu xảy ra lỗi (ví dụ không đủ tiền) để tránh spam
                challenger.setActive(false);
                try (Connection conn = com.auction.config.DatabaseConnection.getConnection()) {
                    autoBidDAO.disableAutoBid(conn, challenger.getId());
                } catch (SQLException ex) {
                    log.error("[Auto-Bidding] Lỗi khi disable AutoBid trong DB: {}", ex.getMessage(), ex);
                }
                auction.removeAutoBidInRam(challenger.getUserId());
            }
            loops++;
        }
    }

    private List<AutoBid> snapshotActiveAutoBids(Auction auction) {
        PriorityQueue<AutoBid> queue = auction.getAutoBidsQueue();
        synchronized (queue) {
            return queue.stream()
                    .filter(AutoBid::isActive)
                    .sorted(Auction.AUTO_BID_PRIORITY)
                    .collect(Collectors.toList());
        }
    }

    private AutoBid selectAutoBidChallenger(List<AutoBid> activeAutoBids, String highestBidderId, double minBidRequired) {
        return activeAutoBids.stream()
                .filter(autoBid -> !autoBid.getUserId().equals(highestBidderId))
                .filter(autoBid -> autoBid.canCover(minBidRequired))
                .findFirst()
                .orElse(null);
    }

    private AutoBid findAutoBidByUser(List<AutoBid> activeAutoBids, String userId) {
        if (userId == null) {
            return null;
        }
        return activeAutoBids.stream()
                .filter(autoBid -> autoBid.getUserId().equals(userId))
                .findFirst()
                .orElse(null);
    }

    private double calculateAutoBidAmount(Auction auction, AutoBid challenger, AutoBid currentLeaderAutoBid, double minBidRequired) {
        double bidAmount = challenger.calculateNextBidAmount(auction.getCurrentPrice(), minBidRequired);

        if (currentLeaderAutoBid != null && Auction.AUTO_BID_PRIORITY.compare(currentLeaderAutoBid, challenger) <= 0) {
            double leaderDefenseLimit = currentLeaderAutoBid.getMaxBid() - auction.getLiveStepPrice();
            bidAmount = Math.min(bidAmount, leaderDefenseLimit);
        }

        return bidAmount;
    }

    private void validateBidInput(Bidder bidder, String auctionId, double amount) {
        if (bidder == null || bidder.getId() == null) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Bidder data cannot be null.");
        }
        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.MISSING_REQUIRED_FIELD, "Auction ID cannot be empty.");
        }
        if (amount <= 0) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Bid amount must be strictly greater than zero.");
        }
    }

    /**
     * ĐÓNG PHÒNG ĐẦU GIÁ KHI HẾT GIỜ (Đã đồng bộ tài chính RAM cho Winner và Seller)
     */
    public void finalizeAuction(String auctionId) {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.MISSING_REQUIRED_FIELD, "Auction ID is required for finalization.");
        }

        synchronized (auctionId.trim().intern()) {
            Auction auction = auctionManage.getAuctionById(auctionId);
            if (auction == null) {
                auction = auctionDAO.findById(auctionId)
                        .orElseThrow(() -> new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND, "Cannot finalize an invalid auction."));
                Item itemFromDb = itemDAO.findById(auction.getItemId()).orElse(null);
                auction.setItem(itemFromDb);
            }

            // Kiểm tra trạng thái thực tế dưới DB thay vì trên RAM
            // (vì AuctionManage.refreshStatus() có thể đã chuyển RAM sang FINISHED trước khi gọi hàm này)
            Auction dbAuction = auctionDAO.findById(auctionId).orElse(null);
            if (dbAuction != null && (dbAuction.getStatus() == AuctionStatus.FINISHED || dbAuction.getStatus() == AuctionStatus.CANCELED)) {
                System.out.println("[Finalize Guard] ℹ️ Phiên " + auctionId + " đã được xử lý kết thúc từ trước (DB). Bỏ qua.");
                return;
            }

            String winnerId = auction.getHighestBidderId();
            double finalPrice = auction.getCurrentPrice();
            String sellerId = auction.getSellerId();
            String statusMessage;

            Connection conn = null;
            try {
                conn = com.auction.config.DatabaseConnection.getConnection();
                conn.setAutoCommit(false);

                if (winnerId != null) {
                    boolean deductOk = userDAO.deductFrozenMoney(conn, winnerId, finalPrice);
                    if (deductOk) {
                        userDAO.addAvailableBalance(conn, sellerId, finalPrice);
                    } else {
                        throw new WalletException(WalletErrorCode.DEDUCTION_FAILED);
                    }
                    statusMessage = "Thông báo: Phiên " + auctionId + " ĐÃ KẾT THÚC. Người thắng: ID " + winnerId + " với giá: " + finalPrice;
                    itemDAO.updateStatus(conn, auction.getItemId(), com.auction.enums.ItemStatus.SOLD.name());

                    var ramItem = productManage.getProduct(auction.getItemId());
                    if (ramItem != null) { ramItem.setStatus(com.auction.enums.ItemStatus.SOLD); }
                } else {
                    statusMessage = "Thông báo: Phiên " + auctionId + " ĐÃ KẾT THÚC. Không có người đặt giá.";
                    itemDAO.updateStatus(conn, auction.getItemId(), com.auction.enums.ItemStatus.ACTIVE.name());

                    var ramItem = productManage.getProduct(auction.getItemId());
                    if (ramItem != null) { ramItem.setStatus(com.auction.enums.ItemStatus.ACTIVE); }
                }

                auctionDAO.updateStatus(conn, auctionId, AuctionStatus.FINISHED.name());
                conn.commit();
                System.out.println("[DB Transaction] ✅ Chốt hạ tài chính đóng phiên đấu giá thành công xuống DB.");

                // 🔥 ĐỒNG BỘ RAM LẬP TỨC CHO WINNER VÀ SELLER KHI PHIÊN ĐẤU GIÁ KẾT THÚC THÀNH CÔNG
                if (winnerId != null) {
                    // 1. Trừ hẳn tiền đóng băng của Winner trên RAM live
                    User winRam = com.auction.manage.UserManage.getInstance().getUser(winnerId);
                    if (winRam instanceof Bidder winnerLive) {
                        synchronized (winnerId.intern()) {
                            winnerLive.setFrozenBalance(winnerLive.getFrozenBalance() - finalPrice);
                            System.out.println("[RAM Sync] 🎯 Đã khấu trừ số dư đóng băng của Winner trên RAM live.");
                        }
                    }

                    // 2. Cộng tiền khả dụng trực tiếp cho Seller trên RAM live
                    User selRam = com.auction.manage.UserManage.getInstance().getUser(sellerId);
                    if (selRam instanceof Seller sellerLive) {
                        synchronized (sellerId.intern()) {
                            sellerLive.setAvailableBalance(sellerLive.getAvailableBalance() + finalPrice);
                            System.out.println("[RAM Sync] 🎯 Đã cộng số dư khả dụng của Seller trên RAM live.");
                        }
                    }

                    // Gửi cập nhật ví qua socket cho Winner và Seller
                    if (winRam instanceof Bidder winnerLive) {
                        try {
                            connectionManage.sendBalanceUpdate(winnerId, winnerLive.getAvailableBalance(), winnerLive.getFrozenBalance());
                        } catch (Exception ex) {
                            System.err.println("[AuctionService] Lỗi gửi WALLET_UPDATE cho winner: " + ex.getMessage());
                        }
                    }
                    if (selRam instanceof Seller sellerLive) {
                        try {
                            connectionManage.sendBalanceUpdate(sellerId, sellerLive.getAvailableBalance(), sellerLive.getFrozenBalance());
                        } catch (Exception ex) {
                            System.err.println("[AuctionService] Lỗi gửi WALLET_UPDATE cho seller: " + ex.getMessage());
                        }
                    }
                }

            } catch (Exception e) {
                if (conn != null) {
                    try { conn.rollback(); } catch (SQLException ex) { log.error("[Finalize] Rollback thất bại: {}", ex.getMessage()); }
                }
                log.error("[Finalize] ❌ Lỗi khi kết thúc phiên {}: {}", auctionId, e.getMessage(), e);
                if (e instanceof com.auction.exception.BaseException) throw (com.auction.exception.BaseException) e;
                throw new AuctionException(AuctionErrorCode.AUCTION_UPDATE_FAILED, "Finalization transaction failed: " + e.getMessage());
            } finally {
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    } finally {
                        try {
                            conn.close();
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }

            Map<String, Object> statusPayload = new HashMap<>();
            statusPayload.put("newStatus", AuctionStatus.FINISHED.name());
            statusPayload.put("message", statusMessage);

            AuctionEvent statusEvent = new AuctionEvent(auctionId, AuctionEventType.STATUS_CHANGED, statusPayload);
            AuctionEventBus.getInstance().publish(statusEvent);

            LiveRoomManage.getInstance().clearRoom(auctionId);
            auctionManage.removeAuctionById(auctionId);
        }
    }

    public List<AuctionSummaryDTO> getJoinedAuctionsSummary(String bidderId) {
        Bidder bidder = getBidderContext(bidderId);
        List<AuctionSummaryDTO> summaries = new ArrayList<>();
        List<String> joinedIds = bidder.getJoinedAuctionIds();

        for (String id : joinedIds) {
            Auction auction = auctionManage.getAuctionById(id);
            if (auction == null) {
                auction = auctionDAO.findById(id).orElse(null);
            }
            if (auction != null) {
                summaries.add(convertToSummaryDTO(auction, bidderId));
            }
        }
        return summaries;
    }

    private AuctionSummaryDTO convertToSummaryDTO(Auction auction, String currentUserId) {
        String itemName = (auction.getItem() != null) ? auction.getItem().getName() : "Vật phẩm #" + auction.getItemId();
        String statusText = auction.getStatus().name();
        if (auction.getStatus() == AuctionStatus.FINISHED) {
            if (currentUserId != null && currentUserId.equals(auction.getHighestBidderId())) {
                statusText = "PAID";
            }
        }
        return new AuctionSummaryDTO(
                auction.getId(), itemName, auction.getCurrentPrice(), statusText, auction.getEndTime()
        );
    }

    public List<AuctionSummaryDTO> getAllActiveAuctions(String currentUserId) {
        List<AuctionSummaryDTO> resultList = new ArrayList<>();
        try (Connection conn = com.auction.config.DatabaseConnection.getConnection()) {
            List<Auction> dbAuctions = auctionDAO.findActiveAndRecentlyFinished(conn, LocalDateTime.now().minusMinutes(5));
            if (dbAuctions == null || dbAuctions.isEmpty()) {
                return resultList;
            }

            for (Auction dbAuction : dbAuctions) {
                Auction ramAuction = auctionManage.getAuctionById(dbAuction.getId());
                Auction finalAuction = (ramAuction != null) ? ramAuction : dbAuction;
                resultList.add(convertToSummaryDTO(finalAuction, currentUserId));
            }
            resultList.sort((a, b) -> b.getStatus().compareTo(a.getStatus()));
        } catch (SQLException e) {
            log.error("[Central Guard] ❌ Lỗi kết nối Database khi tải danh sách: {}", e.getMessage(), e);
            for (Auction ramAuction : auctionManage.getAllActive()) {
                resultList.add(convertToSummaryDTO(ramAuction, currentUserId));
            }
        }
        return resultList;
    }

    public void loadAuctionsToRAM() {
        List<AuctionStatus> activeStatuses = List.of(AuctionStatus.OPEN, AuctionStatus.RUNNING);
        try (Connection conn = com.auction.config.DatabaseConnection.getConnection()) {
            List<Auction> activeAuctionsFromDb = auctionDAO.findByStatuses(conn, activeStatuses);
            for (Auction auction : activeAuctionsFromDb) {
                itemDAO.findById(auction.getItemId()).ifPresent(auction::setItem);
                
                // Hydrate active auto-bids into the auction RAM queue
                List<AutoBid> activeAutoBids = autoBidDAO.findActiveByAuctionId(conn, auction.getId());
                for (AutoBid autoBid : activeAutoBids) {
                    auction.addOrUpdateAutoBidInRam(autoBid);
                }
                
                auctionManage.addAuction(auction);
            }
            log.info("Hệ thống: Đã nạp thành công {} phiên đấu giá lên RAM.", activeAuctionsFromDb.size());
        } catch (SQLException e) {
            log.error("❌ Lỗi nghiêm trọng khi khởi động nạp dữ liệu lên RAM: {}", e.getMessage(), e);
        }
    }

    public AuctionDetailDTO getAuctionDetail(String auctionId) {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.MISSING_REQUIRED_FIELD, "Auction ID cannot be empty.");
        }

        Auction auction = getAuctionContext(auctionId);
        Item item = productManage.getProduct(auction.getItemId());
        if (item == null) {
            item = itemDAO.findById(auction.getItemId()).orElse(null);
            if (item != null) { productManage.addProduct(item); }
        }

        List<BidTransaction> rawBidHistory = bidTransactionDAO.findTopByAuctionId(auctionId, 15);
        String sellerName = userDAO.findById(auction.getSellerId())
                .map(User::getUsername)
                .orElse("Người bán ẩn danh");

        return buildAuctionDetailDTO(auction, item, sellerName, rawBidHistory);
    }

    private Auction getAuctionContext(String auctionId) {
        Auction auction = auctionManage.getAuctionById(auctionId);
        if (auction == null) {
            synchronized (auctionManage) {
                auction = auctionManage.getAuctionById(auctionId);
                if (auction == null) {
                    auction = auctionDAO.findById(auctionId).orElse(null);
                    if (auction != null) {
                        itemDAO.findById(auction.getItemId()).ifPresent(auction::setItem);

                        // Khôi phục danh sách AutoBid hoạt động từ DB lên RAM queue
                        if (auction.getStatus() == AuctionStatus.OPEN || auction.getStatus() == AuctionStatus.RUNNING) {
                            try (Connection conn = com.auction.config.DatabaseConnection.getConnection()) {
                                List<AutoBid> activeAutoBids = autoBidDAO.findActiveByAuctionId(conn, auction.getId());
                                for (AutoBid autoBid : activeAutoBids) {
                                    auction.addOrUpdateAutoBidInRam(autoBid);
                                }
                            } catch (SQLException e) {
                                log.warn("[AutoBid] Không thể khôi phục AutoBid từ DB cho phiên {}: {}", auctionId, e.getMessage());
                            }
                            auctionManage.addAuction(auction);
                        }
                    }
                }
            }
        }
        if (auction == null) {
            throw new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND);
        }
        return auction;
    }

    private AuctionDetailDTO buildAuctionDetailDTO(Auction auction, Item item, String sellerName, List<BidTransaction> rawHistory) {
        String itemName = (item != null) ? item.getName() : "Vật phẩm #" + auction.getItemId();
        String itemDesc = (item != null) ? item.getDescription() : "Không có mô tả";
        String itemImg = (item != null) ? item.getImageUrl() : "";
        List<BidTransactionDTO> historyDTOs = convertToBidHistoryDTO(rawHistory);

        AuctionDetailDTO dto = new AuctionDetailDTO(
                auction.getId(), auction.getCurrentPrice(), auction.getStepPrice(),
                auction.getEndTime(), auction.getStatus().name(), itemName, itemDesc, itemImg, sellerName, historyDTOs
        );
        dto.setLiveStepPrice(auction.getLiveStepPrice());
        return dto;
    }

    private List<BidTransactionDTO> convertToBidHistoryDTO(List<BidTransaction> rawHistory) {
        if (rawHistory == null || rawHistory.isEmpty()) return new ArrayList<>();
        return rawHistory.stream().map(bid -> {
            String bidderName = userDAO.findById(bid.getBidderId()).map(User::getUsername).orElse("Người dùng ẩn danh");
            BidTransactionDTO dto = new BidTransactionDTO(bidderName, bid.getAmount(), bid.getTime(), bid.getStatus().name());
            dto.setBidId(bid.getId());
            dto.setAuctionId(bid.getAuctionId());
            return dto;
        }).collect(Collectors.toList());
    }


    /**
     * 🔥 CẬP NHẬT: HỦY PHÒNG ĐẤU GIÁ ĐA DIỆN (Hỗ trợ phân quyền Admin và Seller chung mạch bộ cục)
     */
    public void cancelAuction(String auctionId, String operatorId, UserRole operatorRole, String reason) {
        if (auctionId == null || auctionId.trim().isEmpty() || operatorId == null || operatorId.trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.MISSING_REQUIRED_FIELD, "Auction ID and Operator ID cannot be empty.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "A valid reason must be provided.");
        }

        Auction auction = getAuctionContext(auctionId);

        synchronized (auction) {
            if (auction.getStatus() == AuctionStatus.FINISHED || auction.getStatus() == AuctionStatus.CANCELED) {
                throw new AuctionException(AuctionErrorCode.AUCTION_CLOSED);
            }

            if (operatorRole == UserRole.SELLER) {
                if (!auction.getSellerId().equals(operatorId)) {
                    throw new AuthorizationException(AuthorizationErrorCode.RESOURCE_OWNERSHIP_VIOLATION, "Access denied: You do not own this auction room.");
                }
                if (auction.getStatus() == AuctionStatus.RUNNING || auction.getHighestBidderId() != null) {
                    throw new AuctionException(AuctionErrorCode.CANNOT_CANCEL_AUCTION_RUNNING, "Cannot cancel an auction that is currently running or already has active bids.");
                }
            }

            String currentWinnerId = auction.getHighestBidderId();
            double currentPrice = auction.getCurrentPrice();

            Connection conn = null;
            try {
                conn = com.auction.config.DatabaseConnection.getConnection();
                conn.setAutoCommit(false);

                // Hoàn tiền đóng băng dưới DB cho người dẫn đầu hiện tại (Chỉ xảy ra khi Admin cưỡng chế gỡ)
                if (currentWinnerId != null) {
                    userDAO.unfreezeMoney(conn, currentWinnerId, currentPrice);
                    bidTransactionDAO.updateStatusToRefunded(conn, auctionId, currentWinnerId);
                }

                auction.setStatus(AuctionStatus.CANCELED);
                auctionDAO.updateStatus(conn, auctionId, AuctionStatus.CANCELED.name());
                itemDAO.updateStatus(conn, auction.getItemId(), com.auction.enums.ItemStatus.ACTIVE.name());

                String logId = UUID.randomUUID().toString();
                String actorStr = (operatorRole == UserRole.ADMIN) ? "Admin cưỡng chế hủy" : "Seller tự hủy";
                String actionDetail = actorStr + " phiên đấu giá [" + auctionId + "]. Lý do: " + reason;
                logDAO.insertLog(conn, logId, operatorId, actionDetail, "AUCTION", auctionId);

                conn.commit();
                System.out.println("[DB Transaction] ✅ Phiên đấu giá đã được hủy thành công bởi: " + operatorRole);

                // 🔥 ĐỒNG BỘ RAM LẬP TỨC: Hoàn trả lại cọc cho người dẫn đầu trên bộ đệm RAM live
                if (currentWinnerId != null) {
                    User winRam = com.auction.manage.UserManage.getInstance().getUser(currentWinnerId);
                    if (winRam instanceof Bidder winnerLive) {
                        synchronized (currentWinnerId.intern()) {
                            winnerLive.setAvailableBalance(winnerLive.getAvailableBalance() + currentPrice);
                            winnerLive.setFrozenBalance(winnerLive.getFrozenBalance() - currentPrice);
                            System.out.println("[RAM Sync] 🔄 Đã xả đóng băng hoàn tiền cọc trên RAM cho: " + currentWinnerId);
                        }
                    }
                }

            } catch (SQLException e) {
                if (conn != null) {
                    try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
                }
                throw new AuctionException(AuctionErrorCode.AUCTION_CANCEL_FAILED, "Cancellation transaction failed: " + e.getMessage());
            } finally {
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    } finally {
                        try {
                            conn.close();
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }

            var ramItem = productManage.getProduct(auction.getItemId());
            if (ramItem != null) { ramItem.setStatus(com.auction.enums.ItemStatus.ACTIVE); }

            Map<String, Object> cancelPayload = new HashMap<>();
            cancelPayload.put("newStatus", AuctionStatus.CANCELED.name());
            String displayMessage = (operatorRole == UserRole.ADMIN)
                    ? "Phiên đấu giá đã bị Admin cưỡng chế hủy bỏ. Lý do: " + reason
                    : "Phiên đấu giá đã bị chủ phòng chủ động đóng cửa.";
            cancelPayload.put("message", displayMessage);

            AuctionEvent cancelEvent = new AuctionEvent(auctionId, AuctionEventType.STATUS_CHANGED, cancelPayload);
            AuctionEventBus.getInstance().publish(cancelEvent);

            LiveRoomManage.getInstance().clearRoom(auctionId);
            auctionManage.removeAuctionById(auctionId);
        }
    }

    /**
     * THEO DÕI PHIÊN NỀN (Đã sửa nhận String bidderId)
     */
    public void joinAuction(String bidderId, String auctionId) {
        Bidder bidder = getBidderContext(bidderId);
        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.MISSING_REQUIRED_FIELD, "Auction ID cannot be empty.");
        }
        Auction auction = auctionManage.getAuctionById(auctionId);
        if (auction == null) { auction = auctionDAO.findById(auctionId).orElse(null); }

        if (auction == null) { throw new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND); }

        if (!bidder.getJoinedAuctionIds().contains(auctionId)) {
            try (Connection conn = com.auction.config.DatabaseConnection.getConnection()) {
                boolean savedToDB = userDAO.addJoinedAuction(conn, bidder.getId(), auctionId);
                if (!savedToDB) { throw new AuctionException(AuctionErrorCode.JOIN_AUCTION_FAILED, "Failed to save joined auction."); }
            } catch (SQLException e) {
                throw new AuctionException(AuctionErrorCode.JOIN_AUCTION_FAILED, "Database link failed at joinAuction: " + e.getMessage());
            }

            synchronized (bidder.getId().intern()) {
                bidder.addJoinedAuction(auctionId);
            }

            Map<String, Object> subscribePayload = new HashMap<>();
            subscribePayload.put("username", bidder.getUsername());
            subscribePayload.put("message", "Bidder " + bidder.getUsername() + " đã đăng ký theo dõi phiên.");
            subscribePayload.put("viewerCount", LiveRoomManage.getInstance().getRoomSize(auctionId));

            AuctionEventBus.getInstance().publish(new AuctionEvent(auctionId, AuctionEventType.AUCTION_SUBSCRIBED, subscribePayload));
        }
    }

    /**
     * VÀO XEM PHÒNG LIVE REALTIME (Đã sửa nhận String bidderId)
     */
    public void joinLiveRoom(String userId, String auctionId, ClientSession clientSession) {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.MISSING_REQUIRED_FIELD, "Auction ID cannot be empty.");
        }

        com.auction.models.User.User user = userDAO.findById(userId).orElse(null);
        String username = (user != null) ? user.getUsername() : (clientSession.getUsername() != null ? clientSession.getUsername() : "User");

        if (user instanceof Bidder) {
            joinAuction(userId, auctionId);
        }

        LiveRoomManage.getInstance().joinRoom(auctionId, clientSession);

        int viewerCount = LiveRoomManage.getInstance().getRoomSize(auctionId);
        Map<String, Object> liveEnteredPayload = new HashMap<>();
        liveEnteredPayload.put("username", clientSession.getUsername());
        liveEnteredPayload.put("message", username + " vừa tham gia phòng.");
        liveEnteredPayload.put("viewerCount", viewerCount);

        AuctionEventBus.getInstance().publish(new AuctionEvent(auctionId, AuctionEventType.LIVE_ENTERED, liveEnteredPayload));
    }

    /**
     * RỜI PHÒNG LIVE REALTIME (Đã sửa nhận String bidderId)
     */
    public void leaveLiveRoom(String userId, String auctionId, ClientSession clientSession) {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.MISSING_REQUIRED_FIELD, "Auction ID cannot be empty.");
        }
        
        com.auction.models.User.User user = userDAO.findById(userId).orElse(null);
        String username = (user != null) ? user.getUsername() : (clientSession.getUsername() != null ? clientSession.getUsername() : "User");

        LiveRoomManage.getInstance().leaveRoom(auctionId, clientSession);

        int viewerCount = LiveRoomManage.getInstance().getRoomSize(auctionId);
        Map<String, Object> liveExitedPayload = new HashMap<>();
        liveExitedPayload.put("username", clientSession.getUsername());
        liveExitedPayload.put("message", username + " đã rời phòng.");
        liveExitedPayload.put("viewerCount", viewerCount);

        AuctionEventBus.getInstance().publish(new AuctionEvent(auctionId, AuctionEventType.LIVE_EXITED, liveExitedPayload));
    }

    /**
     * HỦY THEO DÕI PHIÊN NỀN VĨNH VIỄN (Đã sửa nhận String bidderId)
     */
    public void leaveAuction(String bidderId, String auctionId, ClientSession clientSession) {
        Bidder bidder = getBidderContext(bidderId);
        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.MISSING_REQUIRED_FIELD, "Auction ID cannot be empty.");
        }
        Auction auction = auctionManage.getAuctionById(auctionId);
        if (auction == null) { auction = auctionDAO.findById(auctionId).orElse(null); }

        if (auction != null) {
            if (bidder.getId().equals(auction.getHighestBidderId()) && auction.getStatus() == AuctionStatus.RUNNING) {
                throw new AuctionException(AuctionErrorCode.CANNOT_UNWATCH_LEADING_AUCTION, "Bạn không thể hủy theo dõi vì bạn là người dẫn đầu trên phiên này");
            }
        }

        try (Connection conn = com.auction.config.DatabaseConnection.getConnection()) {
            userDAO.removeJoinedAuction(conn, bidder.getId(), auctionId);
        } catch (SQLException e) {
            throw new AuctionException(AuctionErrorCode.LEAVE_AUCTION_FAILED, "Database link failed at leaveAuction: " + e.getMessage());
        }

        synchronized (bidder.getId().intern()) { bidder.removeJoinedAuction(auctionId); }

        if (clientSession != null) { LiveRoomManage.getInstance().leaveRoom(auctionId, clientSession); }

        int viewerCount = LiveRoomManage.getInstance().getRoomSize(auctionId);
        Map<String, Object> unsubscribePayload = new HashMap<>();
        unsubscribePayload.put("username", bidder.getUsername());
        unsubscribePayload.put("message", "Bidder " + bidder.getUsername() + " đã hủy theo dõi phiên.");
        unsubscribePayload.put("viewerCount", viewerCount);

        AuctionEventBus.getInstance().publish(new AuctionEvent(auctionId, AuctionEventType.AUCTION_UNSUBSCRIBED, unsubscribePayload));
    }

    // =========================================================================
    // 🛡️ PRIVATE HELPER METHODS - TRÍCH XUẤT NGỮ CẢNH AN TOÀN
    // =========================================================================

    /**
     * Chốt chặn tối cao giúp bốc Entity sạch từ Database kiêm ép kiểu Bidder chuẩn chỉ
     */
    private Bidder getBidderContext(String bidderId) {
        if (bidderId == null || bidderId.trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.MISSING_REQUIRED_FIELD, "Operator identity token cannot be null.");
        }
        User user = userDAO.findById(bidderId)
                .orElseThrow(() -> new AuthenticationException(AuthErrorCode.USER_NOT_FOUND, "User authentication failed: user not found."));

        if (!(user instanceof Bidder)) {
            throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "The requested operation restriction rule requires a BIDDER profile scope.");
        }
        return (Bidder) user;
    }

    private final AutoBidDAO autoBidDAO = new AutoBidDAOImpl();

    public void setupAutoBid(String bidderId, String auctionId, double maxBid, double increment) {
        Bidder bidder = getBidderContext(bidderId);
        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.MISSING_REQUIRED_FIELD, "Auction ID cannot be empty.");
        }
        if (maxBid <= 0 || increment <= 0) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Max bid and increment must be greater than zero.");
        }

        Auction auction = getAuctionContext(auctionId);

        synchronized (auction) {
            if (auction.getStatus() != AuctionStatus.OPEN && auction.getStatus() != AuctionStatus.RUNNING) {
                throw new AuctionException(AuctionErrorCode.AUCTION_NOT_RUNNING);
            }
            if (bidder.getId().equals(auction.getSellerId())) {
                throw new AuctionException(AuctionErrorCode.BIDDER_IS_SELLER);
            }
            if (maxBid < auction.getCurrentPrice() + auction.getLiveStepPrice()) {
                throw new AuctionException(AuctionErrorCode.BID_AMOUNT_TOO_LOW, "Max bid must be at least the next required bid.");
            }

            AutoBid autoBid = new AutoBid(bidder.getId(), auctionId, maxBid, increment);

            try (Connection conn = com.auction.config.DatabaseConnection.getConnection()) {
                conn.setAutoCommit(false);
                Optional<AutoBid> existingAutoBid = autoBidDAO.findActiveByUserAndAuction(conn, bidder.getId(), auctionId);
                if (existingAutoBid != null && existingAutoBid.isPresent()) {
                    AutoBid existing = existingAutoBid.get();
                    autoBid.setId(existing.getId());
                    autoBid.setCreatedAt(existing.getCreatedAt());
                }

                boolean success = autoBidDAO.insertOrUpdate(conn, autoBid);
                if (!success) {
                    throw new AuctionException(AuctionErrorCode.AUTO_BID_SAVE_FAILED, "Failed to save auto-bid configuration.");
                }

                // Auto watch/join auction in DB
                if (!bidder.getJoinedAuctionIds().contains(auctionId)) {
                    userDAO.addJoinedAuction(conn, bidder.getId(), auctionId);
                }

                conn.commit();
            } catch (SQLException e) {
                throw new AuctionException(AuctionErrorCode.AUTO_BID_SAVE_FAILED, "Setup auto-bid failed: " + e.getMessage());
            }

            // Sync RAM
            synchronized (bidder.getId().intern()) {
                if (!bidder.getJoinedAuctionIds().contains(auctionId)) {
                    bidder.addJoinedAuction(auctionId);
                }
            }
            auction.addOrUpdateAutoBidInRam(autoBid);
            System.out.println("[Auto-Bidding] ✅ Đã thiết lập Auto-Bid cho User: " + bidder.getUsername() + ", Max: " + maxBid);

            // Trigger thầu tự động ngay lập tức nếu người dùng này hiện tại không phải người dẫn đầu
            // và phiên đang chạy.
            if (auction.getStatus() == AuctionStatus.RUNNING && !bidder.getId().equals(auction.getHighestBidderId())) {
                triggerAutoBids(auction);
            }
        }
    }

    public void cancelAutoBid(String bidderId, String auctionId) {
        Bidder bidder = getBidderContext(bidderId);
        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.MISSING_REQUIRED_FIELD, "Auction ID cannot be empty.");
        }

        Auction auction = getAuctionContext(auctionId);

        synchronized (auction) {
            try (Connection conn = com.auction.config.DatabaseConnection.getConnection()) {
                conn.setAutoCommit(false);
                Optional<AutoBid> opt = autoBidDAO.findActiveByUserAndAuction(conn, bidder.getId(), auctionId);
                if (opt.isPresent()) {
                    AutoBid autoBid = opt.get();
                    autoBidDAO.disableAutoBid(conn, autoBid.getId());
                    conn.commit();
                } else {
                    conn.rollback();
                }
            } catch (SQLException e) {
                throw new AuctionException(AuctionErrorCode.AUTO_BID_CANCEL_FAILED, "Cancel auto-bid failed: " + e.getMessage());
            }

            // Sync RAM
            auction.removeAutoBidInRam(bidder.getId());
            log.info("[Auto-Bidding] ❌ Đã hủy Auto-Bid cho User: {}", bidder.getUsername());
        }
    }

    /**
     * Lấy danh sách các phiên đấu giá do Seller tạo
     */
    public List<AuctionSummaryDTO> getSellerAuctions(String sellerId) {
        if (sellerId == null || sellerId.trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.MISSING_REQUIRED_FIELD, "Seller ID cannot be empty.");
        }
        List<Auction> dbAuctions = auctionDAO.findBySellerId(sellerId);
        List<AuctionSummaryDTO> summaries = new ArrayList<>();
        for (Auction dbAuction : dbAuctions) {
            // Xem thử có phiên live trên RAM không để lấy trạng thái mới nhất
            Auction ramAuction = auctionManage.getAuctionById(dbAuction.getId());
            Auction finalAuction = (ramAuction != null) ? ramAuction : dbAuction;

            // Load Item nếu chưa có
            if (finalAuction.getItem() == null) {
                var item = productManage.getProduct(finalAuction.getItemId());
                if (item == null) {
                    item = itemDAO.findById(finalAuction.getItemId()).orElse(null);
                }
                finalAuction.setItem(item);
            }

            String itemName = (finalAuction.getItem() != null) ? finalAuction.getItem().getName() : "Vật phẩm #" + finalAuction.getItemId();

            // Cập nhật trạng thái theo mốc thời gian thực tế
            finalAuction.refreshStatus(LocalDateTime.now());

            summaries.add(new AuctionSummaryDTO(
                    finalAuction.getId(),
                    itemName,
                    finalAuction.getCurrentPrice(),
                    finalAuction.getStatus().name(),
                    finalAuction.getEndTime(),
                    finalAuction.getStartTime(),
                    finalAuction.getStepPrice()
            ));
        }
        return summaries;
    }

    /**
     * Cập nhật thông số phiên đấu giá chưa chạy (startTime, endTime, stepPrice)
     */
    public void updateAuction(String sellerId, UpdateAuctionRequest request) {
        if (request == null) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Request request content cannot be null.");
        }
        String auctionId = request.getAuctionId();
        double stepPrice = request.getStepPrice();
        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();

        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.MISSING_REQUIRED_FIELD, "Auction ID cannot be empty.");
        }
        if (stepPrice <= 0) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Step price must be greater than zero.");
        }
        if (startTime == null || endTime == null) {
            throw new ValidationException(ValidationErrorCode.MISSING_REQUIRED_FIELD, "Start time and End time cannot be null.");
        }
        if (!startTime.isBefore(endTime)) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Start time must be before End time.");
        }
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Start time must be in the future.");
        }

        Auction auction = getAuctionContext(auctionId);

        // Bảo vệ đa luồng: Lock đối tượng auction khi thực hiện thay đổi
        synchronized (auction) {
            // Kiểm tra sở hữu
            if (!auction.getSellerId().equals(sellerId)) {
                throw new AuthorizationException(AuthorizationErrorCode.RESOURCE_OWNERSHIP_VIOLATION, "Access denied: You do not own this auction room.");
            }

            // Cập nhật trạng thái trước khi kiểm tra
            auction.refreshStatus(LocalDateTime.now());

            // Chỉ cho phép update khi trạng thái là OPEN
            if (auction.getStatus() != AuctionStatus.OPEN) {
                throw new AuctionException(AuctionErrorCode.CANNOT_UPDATE_RUNNING_AUCTION, "Cannot update an auction that is currently running or finished.");
            }

            Connection conn = null;
            try {
                conn = com.auction.config.DatabaseConnection.getConnection();
                conn.setAutoCommit(false);

                boolean dbUpdated = auctionDAO.updateAuctionDetails(conn, auctionId, stepPrice, startTime, endTime);
                if (!dbUpdated) {
                    throw new AuctionException(AuctionErrorCode.AUCTION_UPDATE_FAILED, "Failed to update auction details in the database.");
                }

                conn.commit();
                System.out.println("[DB Transaction] ✅ Đã cập nhật thông tin phiên đấu giá xuống DB.");
            } catch (SQLException e) {
                if (conn != null) {
                    try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
                }
                throw new AuctionException(AuctionErrorCode.AUCTION_UPDATE_FAILED, "Update auction transaction failed: " + e.getMessage());
            } finally {
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    } finally {
                        try {
                            conn.close();
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }

            // Đồng bộ lên RAM
            auction.updateDetails(stepPrice, startTime, endTime);
            System.out.println("[RAM Sync] 🔄 Đã đồng bộ thông tin cập nhật lên RAM cho phiên: " + auctionId);
        }
    }
}
