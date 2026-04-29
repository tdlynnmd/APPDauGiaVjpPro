package com.auction.server;

/*import com.auction.server.manage.AuctionManage;
import com.auction.server.models.Auction.Auction;
import com.auction.server.service.AuctionService;
import java.time.LocalDateTime;*/

public class ServerApp {
    static void main()  {
        //Mô phỏng luồng hoạt động
        /*AuctionService service = new AuctionService();
        LocalDateTime now = LocalDateTime.now();

        // Tạo 1 phiên bắt đầu sau 2 giây và kéo dài 10 giây
        Auction iphone = new Auction("001", "iPhone 15", 1000.0, now.plusSeconds(2), now.plusSeconds(12));
        AuctionManage manager = service.getManager();
        manager.addAuction(iphone);
        manager.startLifecycleMonitor();

        // Giả lập luồng đặt giá từ khách hàng
        System.out.println("--- Bắt đầu mô phỏng tương tác khách hàng ---");

        // 1. Đặt giá khi chưa bắt đầu (OPEN) -> Sẽ thất bại
        Thread.sleep(1000);
        service.processBid("001", "User_A", 1200.0);

        // 2. Đợi phiên chuyển sang RUNNING
        Thread.sleep(2000);
        service.processBid("001", "User_B", 1100.0); // Thành công
        service.processBid("001", "User_C", 1105.0); // Thất bại vì chưa đủ bước giá (+10)
        service.processBid("001", "User_A", 1500.0); // Thành công, vượt qua User_B

        // 3. Đợi phiên kết thúc (FINISHED)
        Thread.sleep(10000);
        service.processBid("001", "User_B", 2000.0); // Thất bại vì đã kết thúc

        Thread.currentThread().join(5000); // Giữ server chạy thêm chút rồi tắt
        }*/
    }
}
