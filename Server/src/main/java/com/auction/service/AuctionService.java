package com.auction.service;

import com.auction.manage.AuctionManage;
import com.auction.manage.ConnectionManage;
import com.auction.models.Auction.Auction;
import com.auction.models.User.Bidder;

import java.util.concurrent.locks.ReentrantLock;

public class AuctionService {
    private final AuctionManage manager = AuctionManage.getInstance();
    private final ConnectionManage connectionManage = ConnectionManage.getInstance();
    private final ReentrantLock bidLock = new ReentrantLock();

    public  AuctionManage getManager() {
        return manager;
    }

    //Xử lý đặt giá
    public boolean processBid(Bidder bidder, String auctionId, double amount) {
        Auction auction = manager.getAuctionById(auctionId);

        if (!connectionManage.isUserOnline(bidder.getId())) {
            System.out.println("Yêu cầu không hợp lệ: Người dùng chưa đăng nhập hoặc đã mất kết nối!");
            return false;
        }

        if (auction == null) {
            System.out.println("Lỗi: Phiên đấu giá không tồn tại");
            return false;
        }

        // Thay vì lock toàn bộ Service, ta chỉ lock đúng phiên đấu giá đó
        synchronized (auction) {
            try {
                // Thực hiện quy trình: Load Bidder -> Call Auction.placeBid -> Save DAO -> Notify
                boolean success = auction.placeBid(bidder, amount);
                if (success) {
                    //Lưu vào DataBase và thông báo cho các bidder đang theo dõi
                    // auctionDAO.save(auction);

                    // auction thông báo đến người tham gia
                    String notification = "Thông báo: "+"Bidder "+bidder.getId()+" đã đặt giá mới: "+ auction.getCurrentPrice();
                    auction.notifySubscribers(notification);
                }
                return success;
            } catch (Exception e) {
                return false;
            }
        }
    }


}
