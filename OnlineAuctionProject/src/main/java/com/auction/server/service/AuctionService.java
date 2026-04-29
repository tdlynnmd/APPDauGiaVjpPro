package com.auction.server.service;
import com.auction.server.manage.AuctionManage;
import com.auction.server.models.Auction.Auction;
import com.auction.server.models.User.Bidder;

import java.util.concurrent.locks.ReentrantLock;

public class AuctionService {
    private final AuctionManage manager = AuctionManage.getInstance();
    private final ReentrantLock bidLock = new ReentrantLock();

    public  AuctionManage getManager() {
        return manager;
    }

    //Xử lý đặt giá
    public boolean processBid(Bidder bidder, String auctionId, double amount) {
        Auction auction = manager.getAuctionById(auctionId);
        if (auction == null) {
            System.out.println("Lỗi: Phiên đấu giá không tồn tại");
            return false;
        }

        // Thay vì lock toàn bộ Service, ta chỉ lock đúng món hàng đó
        synchronized (auction) {
            try {
                // Thực hiện quy trình: Load Bidder -> Call Auction.placeBid -> Save DAO -> Notify
                boolean success = auction.placeBid(bidder, amount);
                if (success) {
                    //Lưu vào DataBase và thông báo cho các bidder đang theo dõi
                    // auctionDAO.save(auction);
                    // notificationService.broadcast(auction);
                    String notification = "Thông báo: "+"Bidder "+bidder.getId()+" đã đặt giá mới: "+ auction.getCurrentPrice();
                }
                return success;
            } catch (Exception e) {
                return false;
            }
        }
    }


}
