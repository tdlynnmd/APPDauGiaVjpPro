package com.auction.service;

import com.auction.dao.AuctionDAO;
import com.auction.dao.BidTransactionDAO;
import com.auction.dao.ItemDAO;
import com.auction.dao.UserDAO;
import com.auction.dao.impl.AuctionDAOImpl;
import com.auction.dao.impl.BidTransactionDAOImpl;
import com.auction.dao.impl.ItemDAOImpl;
import com.auction.dao.impl.UserDAOImpl;
import com.auction.dto.AuctionDetailDTO;
import com.auction.dto.AuctionSummaryDTO;
import com.auction.dto.BidTransactionDTO;
import com.auction.enums.AuctionStatus;
import com.auction.enums.BidStatus;
import com.auction.manage.AuctionManage;
import com.auction.manage.ConnectionManage;
import com.auction.models.Auction.Auction;
import com.auction.models.Auction.BidTransaction;
import com.auction.models.Item.Item;
import com.auction.models.User.Bidder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class AuctionService {
    private final AuctionManage manager = AuctionManage.getInstance();
    private final ConnectionManage connectionManage = ConnectionManage.getInstance();
    private final ReentrantLock bidLock = new ReentrantLock();

    private final UserDAO userDAO = new UserDAOImpl();
    private final AuctionDAO auctionDAO = new AuctionDAOImpl();
    private final ItemDAO itemDAO = new ItemDAOImpl();
    private final BidTransactionDAO bidTransactionDAO = new BidTransactionDAOImpl();
    //private AuctionDAO auctionDAO;

    public AuctionManage getManager() {
        return manager;
    }


    // 1. Tạo phiên đấu giá mới (Nạp từ DB lên RAM luôn)
    public boolean createAuction(String itemId, String sellerId, double startPrice,
                                 double stepPrice, LocalDateTime startTime, LocalDateTime endTime) {

        // Tạo đối tượng Auction (Constructor mới không cần ID, không cần Status)
        Auction newAuction = new Auction(itemId, sellerId, stepPrice, startTime, endTime);

        // Lưu vào Database
        boolean isSaved = auctionDAO.insertAuction(newAuction);

        if (isSaved) {
            // Đẩy lên RAM để AuctionManage bắt đầu theo dõi vòng đời
            manager.addAuction(newAuction);
            return true;
        }
        return false;
    }

    /**
     * Xử lý đặt giá (Process Bid)
     * Luồng: Lock Auction -> Freeze tiền mới -> Place Bid RAM -> Unfreeze tiền cũ -> Update DB Auction
     */
    public boolean processBid(Bidder bidder, String auctionId, double amount) {
        Auction auction = manager.getAuctionById(auctionId);

        if (auction == null) {
            System.err.println("Lỗi: Phiên đấu giá không tồn tại");
            return false;
        }

        if (!connectionManage.isUserOnline(bidder.getId())) {
            System.err.println("Lỗi: Người dùng " + bidder.getUsername() + " không online.");
            return false;
        }

        // Khóa đúng đối tượng auction để đảm bảo tính tuần tự
        synchronized (auction) {
            try {
                // 1. Lưu thông tin người dẫn đầu cũ để hoàn tiền sau
                String oldHighestBidderId = auction.getHighestBidderId();
                double oldPrice = auction.getCurrentPrice();

                // 2. Thực hiện đóng băng tiền của người đặt giá mới trong Database
                // Sử dụng cơ chế Atomic Update (WHERE available >= amount) chống race condition
                boolean freezeSuccess = userDAO.freezeMoney(bidder.getId(), amount);
                if (!freezeSuccess) {
                    System.err.println("Thất bại: Bidder " + bidder.getUsername() + " không đủ tiền khả dụng.");
                    return false;
                }

                // 3. Thực hiện đặt giá trên Logic RAM
                String newBidId = UUID.randomUUID().toString();
                BidTransaction resultBid = auction.placeBid(bidder, amount, newBidId);

                if (resultBid.getStatus() == BidStatus.ACCEPTED) {
                    // Đã chấp nhận trên RAM -> Cập nhật lại số dư RAM cho Bidder hiện tại
                    bidder.freeze(amount);

                    // 4. Cập nhật Database cho phiên đấu giá (Giá mới, Người dẫn đầu mới)
                    boolean updateAuctionDB = auctionDAO.updatePriceAndWinner(
                            auctionId,
                            amount,
                            bidder.getId(),
                            newBidId
                    );

                    if (updateAuctionDB) {
                        // 5. Giải phóng tiền cho người bị Outbid (Người dẫn đầu cũ)
                        if (oldHighestBidderId != null && !oldHighestBidderId.equals(bidder.getId())) {
                            userDAO.unfreezeMoney(oldHighestBidderId, oldPrice);
                            // Lưu ý: Việc cập nhật RAM cho người cũ sẽ được xử lý khi họ thực hiện hành động tiếp theo
                            // hoặc thông qua một hệ thống Cache/UserManage nếu bạn muốn đồng bộ tức thì 100%.
                        }

                        // 6. Gửi thông báo qua Socket cho các Subscriber
                        String msg = "Thông báo: " + bidder.getUsername() + " đã đặt giá " + amount;
                        auction.notifySubscribers(msg);
                        return true;
                    } else {
                        // Nếu update Auction DB lỗi -> Rollback tiền lại cho Bidder
                        userDAO.unfreezeMoney(bidder.getId(), amount);
                        bidder.unfreeze(amount);
                        return false;
                    }

                } else {
                    // Nếu Logic RAM từ chối (ví dụ: không đủ bước giá) -> Trả lại tiền ngay
                    userDAO.unfreezeMoney(bidder.getId(), amount);
                    return false;
                }
            } catch (Exception e) {
                System.err.println("Lỗi nghiêm trọng trong processBid: " + e.getMessage());
                // Cố gắng hoàn tiền nếu có lỗi crash giữa chừng
                userDAO.unfreezeMoney(bidder.getId(), amount);
                return false;
            }
        }
    }

    /**
     * Kết thúc phiên đấu giá (Finalize Auction)
     * Luồng: Deduct Frozen tiền người thắng -> Add Available tiền người bán -> Update Status
     */
    public void finalizeAuction(String auctionId) {
        Auction auction = manager.getAuctionById(auctionId);
        if (auction == null) return;

        synchronized (auction) {
            String winnerId = auction.getHighestBidderId();
            double finalPrice = auction.getCurrentPrice();
            String sellerId = auction.getSellerId();

            String notification;

            if (winnerId != null) {
                // 1. Khấu trừ vĩnh viễn tiền trong cột đóng băng của người thắng
                boolean deductOk = userDAO.deductFrozenMoney(winnerId, finalPrice);
                if (deductOk) {
                    // 2. Cộng tiền vào cột khả dụng cho người bán
                    userDAO.addAvailableBalance(sellerId, finalPrice);
                }
                notification = "Thông báo: Phiên " + auctionId + " ĐÃ KẾT THÚC. Người thắng: ID " + winnerId + " với giá: " + finalPrice;
            } else {
                notification = "Thông báo: Phiên " + auctionId + " ĐÃ KẾT THÚC. Không có người đặt giá.";

            }

            // 3. Cập nhật trạng thái kết thúc trong DB
            auctionDAO.updateStatus(auctionId, AuctionStatus.FINISHED.name());

            // 4. Thông báo kết thúc
            auction.notifySubscribers(notification);
        }
    }

    //Từ userId -> Tìm các auctionId -> Tìm từng Auction -> Đóng gói thành AuctionSummaryDTO -> giúp hiển thị các auction theo dỗi
    //Xây xog DAO quay lại mở khoá
    // 3. [HÀM CỦA BIDDER] - Lấy danh sách các phiên đang tham gia để hiện lên JavaFX
    public List<AuctionSummaryDTO> getJoinedAuctionsSummary(Bidder bidder) {
        List<AuctionSummaryDTO> summaries = new ArrayList<>();
        List<String> joinedIds = bidder.getJoinedAuctionIds();

        for (String id : joinedIds) {
            // Ưu tiên tìm trên RAM (Những phiên đang ACTIVE)
            Auction auction = manager.getAuctionById(id);

            // Nếu không thấy trên RAM, tìm trong DB (Những phiên đã FINISHED)
            if (auction == null) {
                auction = auctionDAO.findById(id).orElse(null);
            }

            if (auction != null) {
                summaries.add(convertToSummaryDTO(auction));
            }
        }
        return summaries;
    }

    private AuctionSummaryDTO convertToSummaryDTO(Auction auction) {
        // Nếu Item chưa được load (transient), bạn có thể load bổ sung ở đây
        String itemName = (auction.getItem() != null) ? auction.getItem().getName() : "Vật phẩm #" + auction.getItemId();

        return new AuctionSummaryDTO(
                auction.getId(),
                itemName,
                auction.getCurrentPrice(),
                auction.getStatus().name(),
                auction.getEndTime()
        );
    }

    // 4. Lấy danh sách TẤT CẢ phiên đang chạy (Để hiển thị trang chủ)
    public List<AuctionSummaryDTO> getAllActiveAuctions() {
        return manager.getAllActive().stream()
                .filter(a -> a.getStatus() == AuctionStatus.RUNNING || a.getStatus() == AuctionStatus.OPEN)
                .map(this::convertToSummaryDTO)
                .collect(Collectors.toList());
    }

    /**
     * Load toàn bộ phiên đang OPEN hoặc RUNNING từ DB lên RAM khi khởi động Server.
     */
    public void loadAuctionsToRAM() {
        List<AuctionStatus> activeStatuses = List.of(AuctionStatus.OPEN, AuctionStatus.RUNNING);
        List<Auction> activeAuctionsFromDb = auctionDAO.findByStatuses(activeStatuses);

        for (Auction auction : activeAuctionsFromDb) {
            // Cần đảm bảo Item được nạp vào Object Auction
            itemDAO.findById(auction.getItemId()).ifPresent(auction::setItem);

            // Đẩy lên RAM để AuctionManage tiếp quản đếm giờ
            manager.addAuction(auction);
        }
        System.out.println("Hệ thống: Đã nạp " + activeAuctionsFromDb.size() + " phiên đấu giá lên RAM.");
    }

    //Hàm Lấy Chi Tiết Phiên đấu giá (Để hiển thị trang chi tiết)
    public AuctionDetailDTO getAuctionDetail(String auctionId) {
        // 1. Lấy dữ liệu thô (Models) từ DB/RAM
        Auction auction = manager.getAuctionById(auctionId); // Ưu tiên tìm trên RAM trước
        if (auction == null) {
            auction = auctionDAO.findById(auctionId).orElse(null);
        }

        // Nếu vẫn không thấy thì trả về null
        if (auction == null) return null;

        // Lấy các thông tin liên quan
        Item item = itemDAO.findById(auction.getItemId()).orElse(null);
        List<BidTransaction> rawBidHistory = bidTransactionDAO.findTopByAuctionId(auctionId, 15);
        String sellerName = userDAO.findById(auction.getSellerId()).get().getUsername();

        // 2. Gọi Helper để đóng gói và trả về
        return buildAuctionDetailDTO(auction, item, sellerName, rawBidHistory);
    }

    /**
     * Helper 1: Lắp ráp các thành phần lại thành AuctionDetailDTO
     */
    private AuctionDetailDTO buildAuctionDetailDTO(Auction auction, Item item, String sellerName, List<BidTransaction> rawHistory) {

        // An toàn kiểm tra Item null (phòng trường hợp data lỗi)
        String itemName = (item != null) ? item.getName() : "Vật phẩm #" + auction.getItemId();
        String itemDesc = (item != null) ? item.getDescription() : "Không có mô tả";
        String itemImg = (item != null) ? item.getImageUrl() : "";

        // Đổi List Model sang List DTO thông qua Helper 2
        List<BidTransactionDTO> historyDTOs = convertToBidHistoryDTO(rawHistory);

        return new AuctionDetailDTO(
                auction.getId(),
                auction.getCurrentPrice(),
                auction.getStepPrice(),
                auction.getEndTime(),
                auction.getStatus().name(),
                itemName,
                itemDesc,
                itemImg,
                sellerName,
                historyDTOs
        );
    }

    /**
     * Helper 2: Xử lý riêng việc convert danh sách Bid
     */
    private List<BidTransactionDTO> convertToBidHistoryDTO(List<BidTransaction> rawHistory) {
        if (rawHistory == null || rawHistory.isEmpty()) {
            return new ArrayList<>();
        }

        return rawHistory.stream().map(bid -> {
            // Lấy tên người bid (Nếu muốn tối ưu hiệu năng hơn, có thể dùng câu JOIN SQL ở DAO,
            // nhưng với Top 15 thì gọi DB ở đây vẫn chấp nhận được)
            String bidderName = userDAO.findById(bid.getBidderId()).get().getUsername();
            if (bidderName == null) bidderName = "Người dùng ẩn danh";

            return new BidTransactionDTO(
                    bidderName,
                    bid.getAmount(),
                    bid.getTime(),
                    bid.getStatus().name()
            );
        }).collect(Collectors.toList());
    }

    /**
     * Hủy phiên đấu giá ngay lập tức và giải phóng tiền cho người đang dẫn đầu.
     */
    public boolean cancelAuction(String auctionId, String reason) {
        Auction auction = manager.getAuctionById(auctionId);
        if (auction == null) return false;

        synchronized (auction) {
            // 1. Hoàn tiền đóng băng cho người dẫn đầu hiện tại (nếu có)
            String currentWinnerId = auction.getHighestBidderId();
            if (currentWinnerId != null) {
                userDAO.unfreezeMoney(currentWinnerId, auction.getCurrentPrice());
            }

            // 2. Cập nhật trạng thái DB và RAM
            auction.setStatus(AuctionStatus.CANCELED);
            auctionDAO.updateStatus(auctionId, AuctionStatus.CANCELED.name());

            // 3. Thông báo cho những người đang theo dõi
            auction.notifySubscribers("Thông báo: Phiên đấu giá bị hủy do: " + reason);

            // 4. Xóa khỏi RAM
            manager.removeAuctionById(auctionId);
            return true;
        }
    }


}

