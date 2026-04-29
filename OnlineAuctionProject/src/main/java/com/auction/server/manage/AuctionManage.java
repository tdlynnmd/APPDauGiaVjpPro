package com.auction.server.manage;

import com.auction.server.models.Auction.Auction;
import com.auction.server.models.Auction.AuctionStatus;
import com.auction.server.service.NotificationService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.auction.server.models.Auction.AuctionStatus.*;


public class AuctionManage {
    private static NotificationService notificationService = NotificationService.getInstance();
    public static volatile AuctionManage instance;
    private final Map<String, Auction> activeAuctions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();;

    private AuctionManage(){}
    public static AuctionManage getInstance(){
        AuctionManage temp = instance;
        if (temp == null){
            synchronized (AuctionManage.class){
                temp = instance;
                if(temp == null){
                    temp = instance  = new AuctionManage();
                }
            }
        }
        return temp;
    }

    public void addAuction(Auction auction){
        activeAuctions.put(auction.getId(),auction);
    }

    public void removeAuctionById(String id){
        activeAuctions.remove(id);
    }

    public Auction getAuctionById(String id){
        return activeAuctions.get(id);
    }

    public List<Auction> getAllActive(){
        return new ArrayList<>(activeAuctions.values());
    }

    private void finishAuction(String auctionId) {
        Auction a = activeAuctions.get(auctionId);
        if (a != null) {
            //Lấy thông tin winner
            String idWinner = a.getHigestBidder().getId();
            double finalPrice = a.getCurrentPrice();
            String notification = "Thông báo: Phiên " + auctionId + " ĐÃ KẾT THÚC VỚI NGƯỜI THẮNG CUỘC LÀ: ID "+idWinner;

            // Xóa khỏi danh sách "đang hoạt động" để giải phóng bộ nhớ RAM
            activeAuctions.remove(auctionId);

            // Ở đây bạn sẽ gọi NotificationService.broadcast() để báo cho các Client qua Socket
            notificationService.notify(notification);
        }
    }

    //Quản lý vòng đời của auction tự động bằng realtime
    public void startLifecycleMonitor() {
        scheduler.scheduleAtFixedRate(() -> {
            LocalDateTime now = LocalDateTime.now();
            for (Auction auction : activeAuctions.values()) {
                auction.refreshStatus(now);

                //Nếu auction bắt đầu thông báo
                if(auction.getStatus() == RUNNING)
                    notificationService.notify("Thông báo: Phiên "+ auction.getId()+" ĐÃ BẮT ĐẦU. ");
                //Nếu auction kết thúc thực hiện logic nghiệp vụ
                if(auction.getStatus() == FINISHED ){
                    finishAuction(auction.getId());
                }
            }
        },0,1, TimeUnit.SECONDS);
    }
}
