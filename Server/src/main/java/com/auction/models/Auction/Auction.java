package com.auction.models.Auction;



import com.auction.models.Entity.Entity;
import com.auction.models.Item.Item;
import com.auction.models.User.Bidder;
import com.auction.observer.Publisher;
import com.auction.observer.Subscriber;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity implements Serializable, Publisher {
    private Item item;
    private double currentPrice;
    private double stepPrice;
    private Bidder highestBidder;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private com.auction.enums.AuctionStatus status;
    private final List<BidTransaction> bids;
    private final List<Subscriber> subscribers = new ArrayList<>();

    //Cấu hình Anti-sniping
    private static final int THRESHOLD_SECONDS = 30; // Nếu thầu trong 30s cuối
    private static final int EXTENSION_SECONDS = 60; // Thì cộng thêm 60s

    public Auction(Item item, double stepPrice, LocalDateTime startTime, LocalDateTime endtime){
        this.item = item;
        this.stepPrice = stepPrice;
        this.currentPrice = item.getStartingPrice();
        this.startTime = startTime;
        this.endTime = endtime;
        this.status = com.auction.enums.AuctionStatus.OPEN;
        this.bids = new ArrayList<>();
    }

    //Tự động gia hạn thời gian nếu có lệnh đặt giá ở phút cuối
    private void checkAndExtend(LocalDateTime now){
        if(now.plusSeconds(THRESHOLD_SECONDS).isAfter(this.endTime)){
            this.endTime = this.endTime.plusSeconds(EXTENSION_SECONDS);
            System.out.println("Hệ thống: Tự động gia hạn phiên đấu giá thêm " +EXTENSION_SECONDS+ " giây.");
        }
    }

    /*
     * Phương thức đặt giá (Thread-safe)
     * Sd synchronized để tránh nhiều người đặt cùng lúc
     */
    public synchronized boolean placeBid(Bidder bidder, double amount) {
        //1. Cập nhật trạng thái lúc đặt giá
        LocalDateTime now = LocalDateTime.now();
        refreshStatus(now);

        //2. Kiểm tra hợp lệ
        if (this.checkBid(bidder,amount)){
            //hoàn tiền người đặt giá cao nhất cũ
            if (this.highestBidder != null) {
                this.highestBidder.addBalance(this.currentPrice);
            }

            //update người đặt giá cao nhất mới
            this.updateBid(bidder,amount,now);

            //3. Thuật toán Anti-sniping (Gia hạn tự động)
            checkAndExtend(now);
            return true;
        }
        System.out.println("Lỗi: Giá đặt phải cao hơn giá hiện tại: " + currentPrice);
        return false;
    }

    private boolean checkBid(Bidder bidder,double amount){
        //Kiểm tra trạng thái phiên đấu giá
        if (this.status != com.auction.enums.AuctionStatus.RUNNING){
            if (this.status == com.auction.enums.AuctionStatus.OPEN) {
                System.out.println("Lỗi: phiên đấu giá chưa bắt đầu");
            } else if (this.status == com.auction.enums.AuctionStatus.FINISHED){
                System.out.println("Lỗi: phiên đấu giá đã kết thúc");
            }
            return false;
        }

        //Kiểm tra người đấu giá
        if(bidder == null){
            System.out.println("Lỗi: Người đặt giá không tồn tại");
            return false;
        }

        // Kiểm tra giá đặt hợp lệ
        if ((amount - currentPrice)<= stepPrice){
            System.out.println("Lỗi: đặt giá bé hơn step tối thiểu");
            return false;
        }
        return true;
    }

    private void updateBid(Bidder bidder, double amount, LocalDateTime time ){
        this.currentPrice = amount;
        this.highestBidder = bidder;
        this.bids.add(new BidTransaction(bidder,amount,time));
    }

    /*
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

    public ChronoLocalDateTime<?> getStartTime() {
        return startTime;
    }

    public ChronoLocalDateTime<?> getEndTime() {
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

    public List<BidTransaction> getBids() {
        return new ArrayList<>(bids);
    }
}
