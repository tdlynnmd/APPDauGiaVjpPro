package com.auction.models.Auction;

import com.auction.enums.AuctionStatus;
import com.auction.enums.BidStatus;
import com.auction.exception.AuctionErrorCode;
import com.auction.exception.AuctionException;
import com.auction.models.Entity.Entity;
import com.auction.models.Item.Item;
import com.auction.models.User.Bidder;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity implements Serializable {
    private String itemId;              // Foreign Key
    private String sellerId;            // Track người bán
    private String highestBidderId;     // Foreign Key
    private String currentWinningBidId; // Trỏ tới BidTransaction đang ACCEPTED

    private double currentPrice;
    private double stepPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private AuctionStatus status;

    private transient Item item;                  // Load on-demand từ DB
    private transient Bidder highestBidder;       // Load on-demand từ DB

    // Cấu hình Anti-sniping
    private static final int THRESHOLD_SECONDS = 30; // Nếu thầu trong 30s cuối
    private static final int EXTENSION_SECONDS = 60; // Thì cộng thêm 60s

    /**
     * CONSTRUCTOR 1: Tạo mới (New)
     */
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

        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * CONSTRUCTOR 2: Tái tạo từ DB (Hydration)
     */
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
    }

    // Tự động gia hạn thời gian nếu có lệnh đặt giá ở phút cuối
    private void checkAndExtend(LocalDateTime now) {
        if (now.plusSeconds(THRESHOLD_SECONDS).isAfter(this.endTime)) {
            this.endTime = this.endTime.plusSeconds(EXTENSION_SECONDS);
            System.out.println("[Anti-Sniping] ⏱️ Tự động gia hạn phiên thêm " + EXTENSION_SECONDS + " giây.");
        }
    }

    /**
     * Kiểm tra tính hợp lệ của lượt đặt giá
     * 🔥 THAY ĐỔI: Ném trực tiếp AuctionException thay vì trả về true/false
     */
    private void validateBid(Bidder bidder, double amount) {
        // 1. Kiểm tra trạng thái phiên
        if (this.status != AuctionStatus.RUNNING) {
            throw new AuctionException(AuctionErrorCode.AUCTION_NOT_RUNNING);
        }

        // 2. Kiểm tra tính toàn vẹn của đối tượng người dùng
        if (bidder == null || bidder.getId() == null) {
            throw new AuctionException(AuctionErrorCode.ITEM_NOT_FOUND, "Bidder data integrity violation.");
        }

        if (bidder.getId().equals(this.sellerId)) {
            throw new AuctionException(AuctionErrorCode.BIDDER_IS_SELLER);
        }

        // 3. Kiểm tra bước giá đặt hợp lệ
        if ((amount - currentPrice) < stepPrice) {
            throw new AuctionException(AuctionErrorCode.BID_AMOUNT_TOO_LOW);
        }
    }

    private void updateBid(Bidder bidder, double amount, String highestBidderId) {
        this.currentPrice = amount;
        this.highestBidder = bidder;
        this.highestBidderId = highestBidderId;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Phương thức đặt giá (Thread-safe Aggregate Root Behavior)
     * 🔥 THAY ĐỔI: Không bao giờ trả về BidStatus.REJECTED nữa, nếu lỗi là văng Exception phá luồng luôn!
     */
    public synchronized BidTransaction placeBid(Bidder bidder, double amount, String generatedBidId) {
        LocalDateTime now = LocalDateTime.now();
        refreshStatus(now);

        // 🔥 Chốt chặn kiểm tra: Nếu sai quy tắc, Exception ném ra tại đây bẻ gãy luồng thực thi
        this.validateBid(bidder, amount);

        // Tạo lượt thầu mới thành công ở trạng thái ACCEPTED
        BidTransaction newBid = new BidTransaction(generatedBidId, bidder.getId(), this.getId(), amount, now, BidStatus.ACCEPTED);

        // Cập nhật con trỏ trạng thái nội bộ của mô hình RAM
        updateBid(bidder, amount, bidder.getId());
        this.currentWinningBidId = newBid.getId();

        // Kích hoạt cơ chế chống bắn tỉa giây cuối
        checkAndExtend(now);

        return newBid;
    }

    /**
     * Cập nhật trạng thái phiên dựa trên thời gian thực
     */
    public void refreshStatus(LocalDateTime now) {
        if (status == AuctionStatus.PAID || status == AuctionStatus.CANCELED || status == AuctionStatus.FINISHED) {
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
}