package com.auction.models.Auction;

import com.auction.enums.AuctionStatus;
import com.auction.enums.BidStatus;
import com.auction.models.Entity.Entity;
import com.auction.models.Item.Item;
import com.auction.models.User.Bidder;
import com.auction.observer.Publisher;
import com.auction.observer.Subscriber;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity implements Serializable, Publisher {
    private String itemId;              // Foreign Key
    private String sellerId;            // Track người bán
    private String highestBidderId;     // Foreign Key

    // THÊM TRƯỜNG NÀY: Thay thế hoàn toàn cho cái List cồng kềnh
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

    private transient List<Subscriber> subscribers;

    //Cấu hình Anti-sniping
    private static final int THRESHOLD_SECONDS = 30; // Nếu thầu trong 30s cuối
    private static final int EXTENSION_SECONDS = 60; // Thì cộng thêm 60s

    /**
     * CONSTRUCTOR 1: Tạo mới (New)
     * Dùng khi User bắt đầu nhấn nút "Tạo phiên đấu giá"
     */
    public Auction(Item item, String sellerId, double stepPrice, LocalDateTime startTime, LocalDateTime endTime) {
        super();
        this.item =item;
        this.itemId = item.getId();
        this.sellerId = sellerId;
        this.stepPrice = stepPrice;
        this.currentPrice = item.getStartingPrice();
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN;

        // Gán thời điểm hiện tại vì đây là lúc nó "sinh ra"
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        this.subscribers = new ArrayList<>();
    }

    /**
     * CONSTRUCTOR 2: Tái tạo từ DB (Hydration)
     * Dùng trong DAO khi ResultSet trả về dữ liệu
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

        // NHẬN GIÁ TRỊ TỪ DB: Không được dùng .now() ở đây
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;

        this.status = status;
        this.subscribers = new ArrayList<>();
    }

    //Tự động gia hạn thời gian nếu có lệnh đặt giá ở phút cuối
    private void checkAndExtend(LocalDateTime now){
        if(now.plusSeconds(THRESHOLD_SECONDS).isAfter(this.endTime)){
            this.endTime = this.endTime.plusSeconds(EXTENSION_SECONDS);
            System.out.println("Hệ thống: Tự động gia hạn phiên đấu giá thêm " +EXTENSION_SECONDS+ " giây.");
        }
    }

    /**
     * Cập nhật logic CheckBid: Thêm kiểm tra người bán
     */
    private boolean checkBid(Bidder bidder, double amount) {
        // 1. Kiểm tra trạng thái phiên
        if (this.status != AuctionStatus.RUNNING) {
            System.out.println("Lỗi: Phiên đấu giá không trong trạng thái hoạt động.");
            return false;
        }

        if(bidder.getId() == null){
            System.out.println("Lỗi: Người đặt giá không tồn tại");
            return false;
        }

        // 2. CHỐNG ĐẤU GIÁ ẢO: Người bán không được tự bid
        if (bidder.getId().equals(this.sellerId)) {
            System.out.println("Lỗi: Người bán không thể tự đặt giá cho vật phẩm của mình!");
            return false;
        }

        // 3. Kiểm tra giá đặt hợp lệ
        if ((amount - currentPrice) < stepPrice) {
            System.out.println("Lỗi: Bước giá không đủ. Tối thiểu phải cộng thêm: " + stepPrice);
            return false;
        }

        return true;
    }

    // Sửa lại hàm updateBid cho gọn gàng và chuẩn chữ ký hàm
    private void updateBid(Bidder bidder, double amount, String highestBidderId) {
        this.currentPrice = amount;
        this.highestBidder = bidder;
        this.highestBidderId = highestBidderId;
    }

    /**
     * Phương thức đặt giá (Thread-safe)
     * Sd synchronized để tránh nhiều người đặt cùng lúc
     */
    public synchronized BidTransaction placeBid(Bidder bidder, double amount, String generatedBidId) {
        LocalDateTime now = LocalDateTime.now();
        refreshStatus(now);

        if (!this.checkBid(bidder, amount)) {
            // Trả về đối tượng DTO để Service lưu log thất bại
            return new BidTransaction(generatedBidId, bidder.getId(), this.getId(), amount, now, BidStatus.REJECTED);
        }

        // Tạo lượt bid mới thành công
        BidTransaction newBid = new BidTransaction(generatedBidId, bidder.getId(), this.getId(), amount, now, BidStatus.ACCEPTED);

        // Cập nhật các trường "con trỏ"
        updateBid(bidder,amount,bidder.getId());

        // QUAN TRỌNG: Ghi nhớ ID của lượt bid vừa thắng để lần sau còn lấy ra hoàn tiền
        this.currentWinningBidId = newBid.getId();

        checkAndExtend(now);
        return newBid;
    }

    /**
     * Cập nhật trạng thái phiên dựa trên thời gian thực
     */
    public void refreshStatus(LocalDateTime now){
        if (status == com.auction.enums.AuctionStatus.PAID || status == com.auction.enums.AuctionStatus.CANCELED) return;

        if (now.isBefore(endTime)) {
            this.status = com.auction.enums.AuctionStatus.RUNNING;
        }
        else{
            this.status = com.auction.enums.AuctionStatus.FINISHED;
        }
    }

    //Thông báo cho các Subscriber khi có sự kiện mới (ví dụ: có người đặt giá mới)

    @Override
    public void subscribe(Subscriber subscriber) {
        this.subscribers.add(subscriber);
    }

    @Override
    public void unsubscribe(Subscriber subscriber) {
        this.subscribers.remove(subscriber);
    }

    @Override
    public void notifySubscribers(String message) {
        for (Subscriber subscriber : subscribers) {
            subscriber.update(message);
        }
    }

    public com.auction.enums.AuctionStatus getStatus(){return this.status;}

    public void setStatus(com.auction.enums.AuctionStatus auctionStatus) {
        status=auctionStatus;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public Bidder getHighestBidder() {
        return highestBidder;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }
    public Item getItem() {
        return item;
    }

    public double getStepPrice() {
        return stepPrice;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getHighestBidderId() {
        return highestBidderId;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public String getItemId() {
        return this.itemId;
    }
}
