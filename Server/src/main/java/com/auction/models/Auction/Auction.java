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

public class Auction extends Entity implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(Auction.class);

    public static final Comparator<AutoBid> AUTO_BID_PRIORITY =
            Comparator.comparingDouble(AutoBid::getMaxBid).reversed()
                    .thenComparing(AutoBid::getCreatedAt);

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

    private double liveStepPrice;      // Bước giá live biến động
    private int extensionCount ;    // Đếm số lần đã gia hạn thành công
    private LocalDateTime originalEndTime; // 🔥 Thời gian kết thúc ban đầu, dùng làm mốc trần cứng Anti-sniping

    private transient Item item;                  // Load on-demand từ DB
    private transient Bidder highestBidder;       // Load on-demand từ DB
    private transient PriorityQueue<AutoBid> autoBidsQueue = new PriorityQueue<>(AUTO_BID_PRIORITY);

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
        this.liveStepPrice = stepPrice;
        this.extensionCount = 0; // Khởi tạo mốc đếm bằng 0
        this.originalEndTime = endTime; // 🔥 Lưu mốc kết thúc gốc cho Anti-sniping

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
        this.liveStepPrice = stepPrice;
        this.extensionCount = 0; // Khởi tạo mốc đếm bằng 0
        this.originalEndTime = endTime; // 🔥 Lưu mốc kết thúc gốc cho Anti-sniping
    }

    // Tự động gia hạn thời gian nếu có lệnh đặt giá ở phút cuối
    /**
     * Tự động gia hạn thời gian có kiểm soát trần động và lũy tiến bước giá sau 3 lần
     */
    private void checkAndExtend(LocalDateTime now) {
        if (now.plusSeconds(THRESHOLD_SECONDS).isAfter(this.endTime)) {

            // 💡 CƠ CHẾ TRẦN ĐỘNG: Giới hạn tối đa 30 phút kể từ thời gian kết thúc gốc (originalEndTime)
            // 🔥 SỬa LỖI: Trước đây dùng createdAt.plusMinutes(30) gây vô hiệu hóa Anti-sniping cho phiên dài > 30 phút
            LocalDateTime hardCapEndTime = this.originalEndTime.plusMinutes(30);
            LocalDateTime proposedEndTime = this.endTime.plusSeconds(EXTENSION_SECONDS);

            if (proposedEndTime.isBefore(hardCapEndTime)) {
                this.endTime = proposedEndTime;
                this.extensionCount++; // Tăng số lần gia hạn thành công

                // 🔥 YÊU CẦU CỦA BẠN: Chỉ nhân giá sau 3 lần gia hạn đầu tiên công bằng
                if (this.extensionCount > 3) {
                    this.liveStepPrice = this.liveStepPrice * 2; // Nhân đôi bước giá live
                    log.warn("[Anti-Sniping] 🚨 Cảnh báo Bot/Spam! Bước giá live tăng lũy tiến lên: {}", this.liveStepPrice);
                } else {
                    log.debug("[Anti-Sniping] ⏱️ Gia hạn lành mạnh lần thứ {}. Bước giá giữ nguyên.", this.extensionCount);
                }
            } else {
                // Đóng bẫy giây cuối: Ép về mốc trần cứng cao nhất để chốt hạ phòng
                if (this.endTime.isBefore(hardCapEndTime)) {
                    this.endTime = hardCapEndTime;
                    this.extensionCount++;
                    this.liveStepPrice = this.liveStepPrice * 5; // Lần cuối ép giá cực đại để kết thúc cuộc chơi
                    log.warn("[Anti-Sniping] 🛑 Chạm trần cứng bảo mật! Ép bước giá gấp 5 lần để dứt điểm phiên.");
                }
            }
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
        // 🔥 SỬA TẠI ĐÂY: So sánh với liveStepPrice thay vì stepPrice gốc
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
     * 🔥 THÊM MỚI: Khôi phục trạng thái mô hình RAM (Rollback State)
     * Hàm này được kích hoạt ở khối catch của Service khi Transaction DB bị thất bại,
     * giúp đưa các thông số giá đỉnh, người thắng cũ và thời gian về nguyên trạng.
     *
     * @param oldHighestBidderId ID của người dẫn đầu cũ trước khi bị Overbid
     * @param oldPrice           Mức giá đỉnh cũ trước khi lượt bid này diễn ra
     * @param oldEndTime         Thời gian kết thúc cũ đề phòng trường hợp Anti-sniping lỡ gia hạn hụt
     */
    public synchronized void rollbackBidInRam(String oldHighestBidderId, double oldPrice, LocalDateTime oldEndTime) {
        System.out.println("[RAM Rollback] ⚠️ Đang khôi phục dữ liệu phiên " + this.getId() + " về trạng thái cũ...");

        // 1. Trả lại giá cũ và ID người thắng cũ
        this.currentPrice = oldPrice;
        this.highestBidderId = oldHighestBidderId;

        // 2. Vì highestBidder object (Bidder) được load on-demand, khi rollback ta nên tạm thời
        // hủy tham chiếu object cũ (hoặc set null) để Service tự động nạp lại chuẩn xác ở request sau.
        this.highestBidder = null;

        // 3. Khôi phục lại thời gian kết thúc cũ (đề phòng Anti-sniping lỡ cộng thêm thời gian)
        if (oldEndTime != null) {
            this.endTime = oldEndTime;
        }

        // 4. Tìm lại mã con trỏ BidTransaction đang ACCEPTED trước đó dưới DB
        // Tạm thời để null hoặc rỗng, luồng sau khi load lại từ DB sẽ tự bù đắp trường này
        this.currentWinningBidId = null;

        this.updatedAt = LocalDateTime.now();
        System.out.println("[RAM Rollback] ✅ Khôi phục RAM hoàn tất. Giá hiện tại quay về: " + this.currentPrice);
    }

    /**
     * Cập nhật thông số phiên đấu giá trên RAM đồng thời thiết lập lại các mốc Anti-sniping
     */
    public synchronized void updateDetails(double stepPrice, LocalDateTime startTime, LocalDateTime endTime) {
        this.stepPrice = stepPrice;
        this.liveStepPrice = stepPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.originalEndTime = endTime;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Cập nhật trạng thái phiên dựa trên thời gian thực
     */
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
    // 🔥 Đừng quên thêm Getter để Service có thể đọc được bước giá mới đồng bộ xuống DB
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
}
