package com.auction.service;

import com.auction.enums.BidStatus;
import com.auction.manage.AuctionManage;
import com.auction.manage.ConnectionManage;
import com.auction.models.Auction.Auction;
import com.auction.models.Auction.BidTransaction;
import com.auction.models.User.Bidder;

import java.util.UUID;
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
        // Trong AuctionService.java
        synchronized (auction) {
            try {
                String newBidId = UUID.randomUUID().toString();
                BidTransaction resultBid = auction.placeBid(bidder, amount, newBidId );

                // Chỉ khi ACCEPTED mới thông báo cho các Subscriber
                if (resultBid.getStatus() == BidStatus.ACCEPTED) {

                    // TODO: Lưu resultBid vào bảng `bids` trong Database
                    // bidDAO.save(resultBid);

                    // TODO: Update trạng thái REFUNDED cho bid cũ trong Database
                    // bidDAO.updateStatus(auction.getId(), BidStatus.REFUNDED, ...);

                    String notification = "Thông báo: Bidder " + bidder.getId() + " đã vươn lên dẫn đầu với giá: " + amount;
                    auction.notifySubscribers(notification);
                    return true;

                } else {
                    // Nếu là REJECTED
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }
}
