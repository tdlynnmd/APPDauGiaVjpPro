package com.auction.models.Auction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auction.enums.AuctionStatus;
import com.auction.enums.BidStatus;
import com.auction.exception.AuctionErrorCode;
import com.auction.exception.AuctionException;
import com.auction.models.Entity.Entity;
import com.auction.models.Item.Item;
import com.auction.models.User.Bidder;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

/**
 * Lớp biểu diễn thực thể Auction trong hệ thống.
 */
public class Auction extends Entity implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(Auction.class);

    public static final Comparator<AutoBid> AUTO_BID_PRIORITY =
            Comparator.comparingDouble(AutoBid::getMaxBid).reversed()
                    .thenComparing(AutoBid::getCreatedAt);

    private String itemId;
    private String sellerId;
    private String highestBidderId;
    private String currentWinningBidId;

    private double currentPrice;
    private double stepPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private AuctionStatus status;

    private double liveStepPrice;
    private int extensionCount ;
    private LocalDateTime originalEndTime;

    private transient Item item;
    private transient Bidder highestBidder;
    private transient PriorityQueue<AutoBid> autoBidsQueue = new PriorityQueue<>(AUTO_BID_PRIORITY);
    private transient List<BidTransaction> bidHistoryRam = new ArrayList<>();

    private static final int THRESHOLD_SECONDS = 30;
    private static final int EXTENSION_SECONDS = 60;

    public Auction(Item item, String sellerId, double stepPrice, LocalDateTime startTime, LocalDateTime endTime) {
        super();
        this.item = item;
        this.itemId = item.getId();
        this.sellerId = sellerId;
        this.stepPrice = stepPrice;
        this.currentPrice = item.getStartingPrice();
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN;
        this.liveStepPrice = stepPrice;
        this.extensionCount = 0;
        this.originalEndTime = endTime;

        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Auction(String id, String itemId, String sellerId, String highestBidderId,
                   String currentWinningBidId, double currentPrice, double stepPrice,
                   LocalDateTime startTime, LocalDateTime endTime,
                   LocalDateTime createdAt, LocalDateTime updatedAt, AuctionStatus status) {
        super(id);
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.highestBidderId = highestBidderId;
        this.currentWinningBidId = currentWinningBidId;
        this.currentPrice = currentPrice;
        this.stepPrice = stepPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.status = status;
        this.liveStepPrice = stepPrice;
        this.extensionCount = 0;
        this.originalEndTime = endTime;
    }

    private void checkAndExtend(LocalDateTime now) {
        if (now.plusSeconds(THRESHOLD_SECONDS).isAfter(this.endTime)) {

            LocalDateTime hardCapEndTime = this.originalEndTime.plusMinutes(30);
            LocalDateTime proposedEndTime = this.endTime.plusSeconds(EXTENSION_SECONDS);

            if (proposedEndTime.isBefore(hardCapEndTime)) {
                this.endTime = proposedEndTime;
                this.extensionCount++;

                if (this.extensionCount > 3) {
                    this.liveStepPrice = this.liveStepPrice * 2;
                    log.warn("[Anti-Sniping] 🚨 Cảnh báo Bot/Spam! Bước giá live tăng lũy tiến lên: {}", this.liveStepPrice);
                } else {
                    log.debug("[Anti-Sniping] ⏱️ Gia hạn lành mạnh lần thứ {}. Bước giá giữ nguyên.", this.extensionCount);
                }
            } else {
                if (this.endTime.isBefore(hardCapEndTime)) {
                    this.endTime = hardCapEndTime;
                    this.extensionCount++;
                    this.liveStepPrice = this.liveStepPrice * 5;
                    log.warn("[Anti-Sniping] 🛑 Chạm trần cứng bảo mật! Ép bước giá gấp 5 lần để dứt điểm phiên.");
                }
            }
        }
    }

    private void validateBid(Bidder bidder, double amount) {
        if (this.status != AuctionStatus.RUNNING) {
            throw new AuctionException(AuctionErrorCode.AUCTION_NOT_RUNNING);
        }

        if (bidder == null || bidder.getId() == null) {
            throw new AuctionException(AuctionErrorCode.ITEM_NOT_FOUND, "Bidder data integrity violation.");
        }

        if (bidder.getId().equals(this.sellerId)) {
            throw new AuctionException(AuctionErrorCode.BIDDER_IS_SELLER);
        }

        if ((amount - currentPrice) < this.liveStepPrice) {
            throw new AuctionException(
                    AuctionErrorCode.BID_AMOUNT_TOO_LOW,
                    "Mức giá quá thấp! Cuộc đấu đá giây cuối đã đẩy bước giá tối thiểu hiện tại lên: " + this.liveStepPrice
            );
        }
    }

    private void updateBid(Bidder bidder, double amount, String highestBidderId) {
        this.currentPrice = amount;
        this.highestBidder = bidder;
        this.highestBidderId = highestBidderId;
        this.updatedAt = LocalDateTime.now();
    }

    public synchronized BidTransaction placeBid(Bidder bidder, double amount, String generatedBidId) {
        LocalDateTime now = LocalDateTime.now();
        refreshStatus(now);

        this.validateBid(bidder, amount);

        BidTransaction newBid = new BidTransaction(generatedBidId, bidder.getId(), this.getId(), amount, now, BidStatus.ACCEPTED);

        updateBid(bidder, amount, bidder.getId());
        this.currentWinningBidId = newBid.getId();

        checkAndExtend(now);

        if (this.bidHistoryRam == null) {
            this.bidHistoryRam = new ArrayList<>();
        }
        for (BidTransaction oldBid : this.bidHistoryRam) {
            oldBid.setStatus(com.auction.enums.BidStatus.REFUNDED);
        }
        this.bidHistoryRam.add(0, newBid);
        if (this.bidHistoryRam.size() > 200) {
            this.bidHistoryRam.remove(this.bidHistoryRam.size() - 1);
        }

        return newBid;
    }

    public synchronized void rollbackBidInRam(String oldHighestBidderId, double oldPrice, LocalDateTime oldEndTime) {
        System.out.println("[RAM Rollback] ⚠️ Đang khôi phục dữ liệu phiên " + this.getId() + " về trạng thái cũ...");

        this.currentPrice = oldPrice;
        this.highestBidderId = oldHighestBidderId;

        this.highestBidder = null;

        if (oldEndTime != null) {
            this.endTime = oldEndTime;
        }

        this.currentWinningBidId = null;

        if (this.bidHistoryRam != null && !this.bidHistoryRam.isEmpty()) {
            this.bidHistoryRam.remove(0);
            if (!this.bidHistoryRam.isEmpty()) {
                this.bidHistoryRam.get(0).setStatus(com.auction.enums.BidStatus.ACCEPTED);
            }
        }

        this.updatedAt = LocalDateTime.now();
        System.out.println("[RAM Rollback] ✅ Khôi phục RAM hoàn tất. Giá hiện tại quay về: " + this.currentPrice);
    }

    public synchronized void updateDetails(double stepPrice, LocalDateTime startTime, LocalDateTime endTime) {
        this.stepPrice = stepPrice;
        this.liveStepPrice = stepPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.originalEndTime = endTime;
        this.updatedAt = LocalDateTime.now();
    }

    public void refreshStatus(LocalDateTime now) {
        if ( status == AuctionStatus.CANCELED || status == AuctionStatus.FINISHED) {
            return;
        }

        if (now.isBefore(startTime)) {
            this.status = AuctionStatus.OPEN;
        } else if (now.isBefore(endTime)) {
            this.status = AuctionStatus.RUNNING;
        } else {
            this.status = AuctionStatus.FINISHED;
        }
    }

    public AuctionStatus getStatus() { return this.status; }
    public void setStatus(AuctionStatus auctionStatus) { this.status = auctionStatus; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public Bidder getHighestBidder() { return highestBidder; }
    public double getCurrentPrice() { return currentPrice; }
    public Item getItem() { return item; }
    public double getStepPrice() { return stepPrice; }
    public String getSellerId() { return sellerId; }
    public String getHighestBidderId() { return highestBidderId; }
    public void setItem(Item item) { this.item = item; }
    public String getItemId() { return this.itemId; }
    public String getCurrentWinningBidId() { return currentWinningBidId; }
    public double getLiveStepPrice() { return this.liveStepPrice; }

    public void setHighestBidderId(String highestBidderId) {
        this.highestBidderId = highestBidderId;
    }

    public synchronized void addOrUpdateAutoBidInRam(AutoBid autoBid) {
        if (autoBidsQueue == null) {
            autoBidsQueue = new PriorityQueue<>(AUTO_BID_PRIORITY);
        }
        autoBidsQueue.removeIf(ab -> ab.getUserId().equals(autoBid.getUserId()));
        if (autoBid.isActive()) {
            autoBidsQueue.add(autoBid);
        }
    }

    public synchronized void removeAutoBidInRam(String userId) {
        if (autoBidsQueue != null) {
            autoBidsQueue.removeIf(ab -> ab.getUserId().equals(userId));
        }
    }

    public synchronized PriorityQueue<AutoBid> getAutoBidsQueue() {
        if (autoBidsQueue == null) {
            autoBidsQueue = new PriorityQueue<>(AUTO_BID_PRIORITY);
        }
        return autoBidsQueue;
    }

    public synchronized List<BidTransaction> getBidHistoryRam() {
        if (this.bidHistoryRam == null) {
            this.bidHistoryRam = new ArrayList<>();
        }
        return new ArrayList<>(this.bidHistoryRam);
    }

    public synchronized void setBidHistoryRam(List<BidTransaction> history) {
        if (history == null) {
            this.bidHistoryRam = new ArrayList<>();
        } else {
            this.bidHistoryRam = new ArrayList<>(history);
        }
    }
}
