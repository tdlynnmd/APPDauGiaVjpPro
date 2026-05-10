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
    //private AuctionDAO auctionDAO;

    public  AuctionManage getManager() {
        return manager;
    }


    // 1. Tạo phiên đấu giá mới (Nạp từ DB lên RAM luôn)
    //public boolean createAuction(String itemId, double startPrice, LocalDateTime endTime) { ... }

    //2. Xử lý đặt giá
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

    //Từ userId -> Tìm các auctionId -> Tìm từng Auction -> Đóng gói thành AuctionSummaryDTO -> giúp hiển thị các auction theo dỗi
    //Xây xog DAO quay lại mở khoá
    // 3. [HÀM CỦA BIDDER] - Lấy danh sách các phiên đang tham gia để hiện lên JavaFX
    /*public List<AuctionSummaryDTO> getJoinedAuctionsSummary(String userId) {
        List<AuctionSummaryDTO> summaries = new ArrayList<>();

        // 1. Lấy thông tin Bidder từ DB (Giả sử bạn có userDAO)
        User user = userDAO.findById(userId);
        if (!(user instanceof Bidder)) {
            return summaries; // Trả về list rỗng nếu không phải Bidder
        }

        Bidder bidder = (Bidder) user;
        List<String> joinedIds = bidder.getJoinedAuctionIds();

        // 2. Lặp qua từng ID để lấy thông tin phiên đấu giá
        for (String auctionId : joinedIds) {
            // Ưu tiên lấy trên RAM trước cho nhanh
            Auction auction = auctionManage.getAuctionById(auctionId);

            // Nếu phiên đã FINISHED (bị xóa khỏi RAM), thì chui xuống DB lấy
            if (auction == null) {
                auction = auctionDAO.findById(auctionId);
            }

            // 3. Đóng gói DTO
            if (auction != null) {
                String itemName = (auction.getItem() != null) ? auction.getItem().getName() : "Vật phẩm ẩn";

                AuctionSummaryDTO dto = new AuctionSummaryDTO(
                        auction.getId(),
                        itemName,
                        auction.getCurrentPrice(),
                        auction.getStatus().toString(),
                        auction.getEndTime()
                );
                summaries.add(dto);
            }
        }
        return summaries;
    }*/

    // 4. Lấy danh sách TẤT CẢ phiên đang chạy (Để hiển thị trang chủ)
    //public List<AuctionSummaryDTO> getAllActiveAuctions() { ... }

}
