package com.auction.service;

import com.auction.dao.*;
import com.auction.dao.impl.*;
import com.auction.dto.AuctionDetailDTO;
import com.auction.dto.AuctionSummaryDTO;
import com.auction.dto.BidTransactionDTO;
import com.auction.enums.AuctionStatus;
import com.auction.enums.BidStatus;
import com.auction.enums.ItemStatus;
import com.auction.manage.AuctionManage;
import com.auction.manage.ConnectionManage;
import com.auction.manage.LiveRoomManage;
import com.auction.manage.ProductManage;
import com.auction.models.Auction.Auction;
import com.auction.network.ClientSession;
import com.auction.models.Auction.BidTransaction;
import com.auction.models.Item.Item;
import com.auction.models.User.Bidder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AuctionService {
    private final AuctionManage auctionManage = AuctionManage.getInstance();
    private final ConnectionManage connectionManage = ConnectionManage.getInstance();
    private final ProductManage productManage = ProductManage.getInstance();

    private final UserDAO userDAO = new UserDAOImpl();
    private final AuctionDAO auctionDAO = new AuctionDAOImpl();
    private final ItemDAO itemDAO = new ItemDAOImpl();
    private final BidTransactionDAO bidTransactionDAO = new BidTransactionDAOImpl();
    private final LogDAO logDAO = new LogDAOImpl();



    public boolean createAuction(String itemId, String sellerId, double startPrice,
                                 double stepPrice, LocalDateTime startTime, LocalDateTime endTime) {

        // Lấy đối tượng Live trên RAM để kiểm tra trạng thái tức thì
        Item liveItem = productManage.getProduct(itemId);
        if (liveItem == null) {
            // Nếu RAM chưa load, cố gắng tìm dưới DB
            liveItem = itemDAO.findById(itemId).orElse(null);
            if (liveItem == null) return false;
        }

        // BẢO VỆ ĐA LUỒNG: Đồng bộ hóa trên chính vật phẩm để chặn đứng spam click tạo 2 phòng song song
        synchronized (liveItem) {
            if (liveItem.getStatus() != ItemStatus.ACTIVE) {
                System.err.println("Lỗi: Vật phẩm đã nằm trong phiên khác hoặc đã bán.");
                return false;
            }

            Auction newAuction = new Auction(liveItem, sellerId, stepPrice, startTime, endTime);
            boolean isSaved = auctionDAO.insertAuction(newAuction);

            if (isSaved) {
                // 1. Đẩy lên RAM điều phối vòng đời đếm giờ công khai
                auctionManage.addAuction(newAuction);

                // 2. Đồng bộ chuyển trạng thái vật phẩm sang INACTIVE (Khóa kỹ thuật số)
                itemDAO.updateStatus(itemId, com.auction.enums.ItemStatus.INACTIVE.name());
                liveItem.setStatus(com.auction.enums.ItemStatus.INACTIVE);
                return true;
            }
        }
        return false;
    }

    /**
     * Xử lý đặt giá (Process Bid)
     * Luồng: Lock Auction -> Freeze tiền mới -> Place Bid RAM -> Unfreeze tiền cũ -> Update DB Auction
     */
    public boolean processBid(Bidder bidder, String auctionId, double amount) {
        Auction auction = auctionManage.getAuctionById(auctionId);

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
                    synchronized (bidder) {
                        bidder.freeze(amount);
                    }

                    // 4. Cập nhật Database cho phiên đấu giá (Giá mới, Người dẫn đầu mới)
                    boolean updateAuctionDB = auctionDAO.updatePriceAndWinner(
                            auctionId, amount, bidder.getId(), newBidId
                    );

                    if (updateAuctionDB) {
                        // 🔥 TÍCH HỢP TẠI ĐÂY: Lưu biên lai giao dịch thành công ('ACCEPTED') xuống DB bảng bid_transactions
                        // Dữ liệu từ đối tượng 'resultBid' đã có sẵn đầy đủ id, amount, status, và time
                        bidTransactionDAO.insertBid(resultBid);

                        // 5. Giải phóng tiền cho người bị Outbid (Người dẫn đầu cũ)
                        if (oldHighestBidderId != null && !oldHighestBidderId.equals(bidder.getId())) {
                            userDAO.unfreezeMoney(oldHighestBidderId, oldPrice);
                            // Lưu ý: Việc cập nhật RAM cho người cũ sẽ được xử lý khi họ thực hiện hành động tiếp theo
                            // hoặc thông qua một hệ thống Cache/UserManage nếu bạn muốn đồng bộ tức thì 100%.

                            // 🔥 BỔ SUNG TẠI ĐÂY: Chuyển biên lai cũ của người đó thành REFUNDED dưới DB
                            bidTransactionDAO.updateStatusToRefunded(auctionId, oldHighestBidderId);
                        }

                        // Sau khi bid thành công, tự động đưa phiên này vào danh sách theo dõi của Bidder (nếu chưa có)
                        if (!bidder.getJoinedAuctionIds().contains(auctionId)) {
                            userDAO.addJoinedAuction(bidder.getId(), auctionId);
                            synchronized (bidder) {
                                bidder.addJoinedAuction(auctionId);
                            }
                        }

                        // 6. Broadcast thông báo qua LiveRoom cho tất cả clients trong phòng
                        String msg = "Thông báo: " + bidder.getUsername() + " đã đặt giá " + amount;
                        LiveRoomManage.getInstance().broadcast(auctionId, msg);
                        return true;
                    } else {
                        // Nếu update Auction DB lỗi -> Rollback tiền lại cho Bidder
                        userDAO.unfreezeMoney(bidder.getId(), amount);
                        synchronized (bidder) {
                            bidder.unfreeze(amount);
                        }
                        return false;
                    }

                } else {
                    // Nếu Logic RAM từ chối (ví dụ: không đủ bước giá) -> Trả lại tiền ngay
                    userDAO.unfreezeMoney(bidder.getId(), amount);
                    //Lưu vết bid thất bại
                    bidTransactionDAO.insertBid(resultBid);
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
        Auction auction = auctionManage.getAuctionById(auctionId);
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
                // 🔥 SỬA BỔ SUNG: Chuyển trạng thái Item thành SOLD (Đã bán)
                itemDAO.updateStatus(auction.getItemId(), com.auction.enums.ItemStatus.SOLD.name());
                productManage.getProduct(auction.getItemId()).setStatus(com.auction.enums.ItemStatus.SOLD);
            } else {
                notification = "Thông báo: Phiên " + auctionId + " ĐÃ KẾT THÚC. Không có người đặt giá.";
                // 🔥 SỬA BỔ SUNG: Không ai mua -> Trả tự do cho Item thành ACTIVE
                itemDAO.updateStatus(auction.getItemId(), com.auction.enums.ItemStatus.ACTIVE.name());
                var ramItem = productManage.getProduct(auction.getItemId());
                if (ramItem != null) {
                    ramItem.setStatus(com.auction.enums.ItemStatus.ACTIVE);
                }

            }

            // 3. Cập nhật trạng thái kết thúc trong DB
            auctionDAO.updateStatus(auctionId, AuctionStatus.FINISHED.name());

            // 4. Broadcast thông báo kết thúc
            LiveRoomManage.getInstance().broadcast(auctionId, notification);

            // 5. Xóa phòng (không ai cần broadcast nữa)
            LiveRoomManage.getInstance().clearRoom(auctionId);
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
            Auction auction = auctionManage.getAuctionById(id);

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
        return auctionManage.getAllActive().stream()
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
            auctionManage.addAuction(auction);
        }
        System.out.println("Hệ thống: Đã nạp " + activeAuctionsFromDb.size() + " phiên đấu giá lên RAM.");
    }

    //Hàm Lấy Chi Tiết Phiên đấu giá (Để hiển thị trang chi tiết)
    public AuctionDetailDTO getAuctionDetail(String auctionId) {
        // 1. Lấy dữ liệu thô (Models) từ DB/RAM
        Auction auction = auctionManage.getAuctionById(auctionId); // Ưu tiên tìm trên RAM trước
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
    public boolean cancelAuction(String auctionId, String adminId, String reason) {
        Auction auction = auctionManage.getAuctionById(auctionId);
        if (auction == null || adminId == null) return false;

        synchronized (auction) {
            // 1. Hoàn tiền đóng băng cho người dẫn đầu hiện tại (nếu có)
            String currentWinnerId = auction.getHighestBidderId();
            if (currentWinnerId != null) {
                userDAO.unfreezeMoney(currentWinnerId, auction.getCurrentPrice());

                // 🔥 BỔ SUNG TẠI ĐÂY: Người dẫn đầu bị hủy phiên -> Hoàn tiền -> Đổi trạng thái sang REFUNDED
                bidTransactionDAO.updateStatusToRefunded(auctionId, currentWinnerId);
            }

            // 2. Cập nhật trạng thái DB và RAM
            auction.setStatus(AuctionStatus.CANCELED);
            auctionDAO.updateStatus(auctionId, AuctionStatus.CANCELED.name());

            // 🔥 SỬA BỔ SUNG: Giải phóng Item trở lại trạng thái ACTIVE
            itemDAO.updateStatus(auction.getItemId(), com.auction.enums.ItemStatus.ACTIVE.name());
            var ramItem = productManage.getProduct(auction.getItemId());
            if (ramItem != null) {
                ramItem.setStatus(com.auction.enums.ItemStatus.ACTIVE);
            }

            // 3. Broadcast thông báo cho những người đang theo dõi
            LiveRoomManage.getInstance().broadcast(auctionId,
                    "Thông báo: Phiên đấu giá bị hủy do: " + reason);

            // 4. Xóa phòng
            LiveRoomManage.getInstance().clearRoom(auctionId);

            // 5. Xóa khỏi RAM
            auctionManage.removeAuctionById(auctionId);

            // 🔥 BƯỚC CỐT LÕI: GHI NHẬT KÝ HÀNH ĐỘNG HỦY PHIÊN CỦA ADMIN XUỐNG DB
            String logId = UUID.randomUUID().toString();
            String actionDetail = "Cưỡng chế hủy phiên đấu giá bị hủy do: " + reason;
            logDAO.insertLog(logId, adminId, actionDetail, "AUCTION", auctionId);

            return true;
        }
    }

    /**
     * Bidder join vào phiên đấu giá để tracking + receive notifications
     * Luồng: -> chỉ ghi ấn nút tham gia phiên
     * 1. Lưu vào DB bảng bidder_joined_auctions (persist dữ liệu)
     * 2. Cập nhật RAM của Bidder (joinedAuctionIds)
     * 3. Thêm ClientSession vào LiveRoom (để receive real-time broadcasts)
     *
     * @param bidder Người dùng join
     * @param auctionId Phiên cần join
     * @param clientSession Kết nối socket để gửi real-time notifications
     * @return true nếu join thành công
     */
    public boolean joinAuction(Bidder bidder, String auctionId, ClientSession clientSession) {
        Auction auction = auctionManage.getAuctionById(auctionId);
        if (auction == null) {
            auction = auctionDAO.findById(auctionId).orElse(null);
        }

        if (auction == null) {
            System.err.println("Lỗi: Phiên đấu giá không tồn tại");
            return false;
        }

        // Phòng trường hợp User đã bấm Tham gia rồi nhưng click lại hoặc kết nối lại Socket
        if (!bidder.getJoinedAuctionIds().contains(auctionId)) {
            // 1. Lưu vào DB (bảng trung gian: bidder_joined_auctions)
            boolean savedToDB = userDAO.addJoinedAuction(bidder.getId(), auctionId);
            if (!savedToDB) {
                System.err.println("Lỗi: Không thể lưu vào DB");
                return false;
            }

            // 2. Cập nhật RAM của Bidder
            synchronized (bidder) {
                if (bidder.addJoinedAuction(auctionId)) {
                    System.out.println("✅ Bidder " + bidder.getUsername() + " đã join phiên " + auctionId);
                }
                System.out.println("Join không thành công");
            }
        }

        // 3. Thêm vào LiveRoom để nhận broadcasts real-time
        LiveRoomManage.getInstance().joinRoom(auctionId, clientSession);

        // 4. Broadcast thông báo có người join mới
        String joinMsg = "Thông báo: " + bidder.getUsername() + " đã tham gia phiên";
        LiveRoomManage.getInstance().broadcast(auctionId, joinMsg);

        return true;
    }

    /**
     * THAY ĐỔI: Người dùng ĐÓNG TAB CHI TIẾT (Chỉ thoát giao diện và ngắt socket)
     * Tuyệt đối không xóa dữ liệu theo dõi hoặc tiền của họ trong DB/RAM.
     */
    public boolean leaveLiveRoom(Bidder bidder, String auctionId, ClientSession clientSession) {
        // Chỉ dọn dẹp liên kết socket, giữ nguyên tính toàn vẹn dữ liệu tài chính
        LiveRoomManage.getInstance().leaveRoom(auctionId, clientSession);

        String leaveMsg = "Thông báo: " + bidder.getUsername() + " đã thoát màn hình xem trực tuyến.";
        LiveRoomManage.getInstance().broadcast(auctionId, leaveMsg);
        return true;
    }

    /**
     * THAY ĐỔI: BỔ SUNG THÊM NGIỆP VỤ MỚI
     * Người dùng chủ động bấm nút "Hủy theo dõi" (Unwatch) trên màn hình "Phiên của tôi"
     */
    public boolean unwatchAuction(Bidder bidder, String auctionId) {
        Auction auction = auctionManage.getAuctionById(auctionId);
        if (auction == null) {
            auction = auctionDAO.findById(auctionId).orElse(null);
        }

        if (auction != null) {
            // LUẬT: Nếu đang là người dẫn đầu, tuyệt đối không được phép bỏ cuộc/ẩn danh sách!
            if (bidder.getId().equals(auction.getHighestBidderId()) && auction.getStatus() == AuctionStatus.RUNNING) {
                System.err.println("Lỗi nghiêm trọng: Không thể hủy theo dõi phiên bạn đang dẫn đầu giá.");
                return false;
            }
        }

        // Thực hiện xóa khỏi DB và RAM giám sát của User
        userDAO.removeJoinedAuction(bidder.getId(), auctionId);
        synchronized (bidder) {
            bidder.removeJoinedAuction(auctionId);
        }
        System.out.println("✅ Bidder " + bidder.getUsername() + " đã ngừng theo dõi phiên " + auctionId);
        return true;
    }
}

