package com.auction.server.models.Auction;

import com.auction.server.models.Entity.Entity;
import com.auction.server.models.Item.Item;
import com.auction.server.models.User.Bidder;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity implements Serializable {
    private Item item;
    private double currentPrice;
    private Bidder highestBidder;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private final List<BidTransaction> bids;

    //Cấu hình Anti-sniping
    private static final int THRESHOLD_SECONDS = 30; // Nếu thầu trong 30s cuối
    private static final int EXTENSION_SECONDS = 60; // Thì cộng thêm 60s

    public Auction(Item item, LocalDateTime startTime, LocalDateTime endtime){
        this.item = item;
        this.currentPrice = item.getStartingPrice();
        this.startTime = startTime;
        this.endTime = endtime;
        this.status = AuctionStatus.OPEN;
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

        //2. Kiểm tra trạng thái phiên đấu giá
        if (this.status == AuctionStatus.OPEN){
            System.out.println("Lỗi: phiên đấu giá chưa bắt đầu");
            return false;
        }
        else if (this.status == AuctionStatus.FINISHED){
            System.out.println("Lỗi: phiên đấu giá đã kết thúc");
            return false;
        }

        //3. Kiểm tra giá đặt hợp lệ
        if (amount > currentPrice){
            //hoàn tiền người đặt giá cao nhất cũ
            this.highestBidder.refund(this.currentPrice);

            //update người đặt giá cao nhất mới
            this.currentPrice = amount;
            this.highestBidder = bidder;
            this.bids.add(new BidTransaction(bidder,amount,now));

            //4. Thuật toán Anti-sniping (Gia hạn tự động)
            checkAndExtend(now);
            return true;
        }
        System.out.println("Lỗi: Giá đặt phải cao hơn giá hiện tại: " + currentPrice);
        return false;
    }

    /*
    * Cập nhật trạng thái phiên dựa trên thời gian thực
    */
    public void refreshStatus(LocalDateTime now){
        if (status == AuctionStatus.PAID || status == AuctionStatus.CANCELED) return;

        if(now.isBefore(startTime)){
            this.status = AuctionStatus.OPEN;
        } else if (now.isAfter(startTime) && now.isBefore(endTime)) {
            this.status = AuctionStatus.RUNNING;
        }
        else{
            this.status = AuctionStatus.FINISHED;
        }
    }

    public AuctionStatus getStatus(){return this.status;}
}
