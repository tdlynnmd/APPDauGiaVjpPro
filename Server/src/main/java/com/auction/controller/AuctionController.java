package com.auction.controller;

import com.auction.dao.UserDAO;
import com.auction.dao.impl.UserDAOImpl;
import com.auction.dto.AuctionDetailDTO;
import com.auction.dto.AuctionSubscriptionRequest;
import com.auction.dto.AuctionSummaryDTO;
import com.auction.dto.CancelAuctionRequest;
import com.auction.dto.CreateAuctionRequest;
import com.auction.dto.GetAuctionDetailRequest;
import com.auction.dto.PlaceBidRequest;
import com.auction.models.User.Bidder;
import com.auction.models.User.User;
import com.auction.network.ClientSession;
import com.auction.service.AuctionService;
import com.google.gson.Gson;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AuctionController là controller phía Server cho nhóm chức năng đấu giá.
 *
 * Vai trò:
 * - Nhận body JSON đã được RequestDispatcher chuyển đến.
 * - Parse body thành Request DTO cụ thể.
 * - Lấy thông tin user hiện tại từ ClientSession khi cần.
 * - Gọi AuctionService để xử lý nghiệp vụ.
 * - Trả dữ liệu kết quả về cho RequestDispatcher.
 *
 * Lưu ý:
 * - AuctionController KHÔNG gửi socket trực tiếp.
 * - AuctionController KHÔNG tạo SocketResponse.
 * - RequestDispatcher mới là nơi bọc kết quả thành SocketResponse và gửi về Client.
 */
public class AuctionController {
    private final Gson gson = new Gson();
    private final AuctionService auctionService = new AuctionService();
    private final UserDAO userDAO = new UserDAOImpl();

    /**
     * Lấy danh sách các phiên đấu giá đang hoạt động.
     *
     * Action tương ứng:
     * - GET_ACTIVE_AUCTIONS
     *
     * Response body:
     * - List<AuctionSummaryDTO>
     */
    public List<AuctionSummaryDTO> getActiveAuctions() {
        return auctionService.getAllActiveAuctions();
    }

    /**
     * Lấy chi tiết một phiên đấu giá.
     *
     * Action tương ứng:
     * - GET_AUCTION_DETAIL
     *
     * Body JSON cần có:
     * - auctionId
     *
     * Response body:
     * - AuctionDetailDTO
     */
    public AuctionDetailDTO getAuctionDetail(String bodyJson) {
        GetAuctionDetailRequest request = parseBody(bodyJson, GetAuctionDetailRequest.class);

        requireText(request.getAuctionId(), "auctionId must not be empty.");

        AuctionDetailDTO detail = auctionService.getAuctionDetail(request.getAuctionId());

        if (detail == null) {
            throw new IllegalArgumentException("Auction not found.");
        }

        return detail;
    }

    /**
     * Tạo phiên đấu giá mới.
     *
     * Action tương ứng:
     * - CREATE_AUCTION
     *
     * Body JSON cần có:
     * - itemId
     * - stepPrice
     * - startTime
     * - endTime
     *
     * Lưu ý bảo mật:
     * - Client không được gửi sellerId.
     * - Server lấy sellerId từ ClientSession để tránh giả mạo.
     */
    public Boolean createAuction(String bodyJson, ClientSession session) {
        CreateAuctionRequest request = parseBody(bodyJson, CreateAuctionRequest.class);

        String sellerId = requireLoggedInUserId(session);
        requireText(request.getItemId(), "itemId must not be empty.");

        if (request.getStepPrice() <= 0) {
            throw new IllegalArgumentException("stepPrice must be greater than 0.");
        }

        LocalDateTime startTime = parseDateTime(request.getStartTime(), "startTime");
        LocalDateTime endTime = parseDateTime(request.getEndTime(), "endTime");

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime.");
        }

        /*
         * AuctionService.createAuction hiện có tham số startPrice,
         * nhưng implementation hiện tại giá khởi điểm được lấy từ Item.
         * Vì CreateAuctionRequest chưa có startPrice, ta truyền 0.0.
         * Nếu sau này service thật sự dùng startPrice, hãy thêm field startPrice vào DTO.
         */
        boolean created = auctionService.createAuction(
                request.getItemId(),
                sellerId,
                0.0,
                request.getStepPrice(),
                startTime,
                endTime
        );

        if (!created) {
            throw new IllegalStateException("Create auction failed.");
        }

        return true;
    }

    /**
     * Đặt giá vào một phiên đấu giá.
     *
     * Action tương ứng:
     * - PLACE_BID
     *
     * Body JSON cần có:
     * - auctionId
     * - amount
     *
     * Lưu ý:
     * - AuctionService.processBid cần object Bidder.
     * - Vì vậy controller phải load user hiện tại và kiểm tra user đó là Bidder.
     */
    public Boolean placeBid(String bodyJson, ClientSession session) {
        PlaceBidRequest request = parseBody(bodyJson, PlaceBidRequest.class);

        requireText(request.getAuctionId(), "auctionId must not be empty.");

        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0.");
        }

        Bidder bidder = getCurrentBidder(session);

        boolean placed = auctionService.processBid(
                bidder,
                request.getAuctionId(),
                request.getAmount()
        );

        if (!placed) {
            throw new IllegalStateException("Place bid failed.");
        }

        return true;
    }

    /**
     * Đăng ký nhận realtime update của một phiên đấu giá.
     *
     * Action tương ứng:
     * - SUBSCRIBE_AUCTION
     *
     * Body JSON cần có:
     * - auctionId
     */
    public Boolean subscribeAuction(String bodyJson, ClientSession session) {
        AuctionSubscriptionRequest request = parseBody(bodyJson, AuctionSubscriptionRequest.class);

        requireText(request.getAuctionId(), "auctionId must not be empty.");

        Bidder bidder = getCurrentBidder(session);

        boolean subscribed = auctionService.joinAuction(
                bidder,
                request.getAuctionId(),
                session
        );

        if (!subscribed) {
            throw new IllegalStateException("Subscribe auction failed.");
        }

        return true;
    }

    /**
     * Hủy đăng ký nhận realtime update của một phiên đấu giá.
     *
     * Action tương ứng:
     * - UNSUBSCRIBE_AUCTION
     *
     * Body JSON cần có:
     * - auctionId
     */
    public Boolean unsubscribeAuction(String bodyJson, ClientSession session) {
        AuctionSubscriptionRequest request = parseBody(bodyJson, AuctionSubscriptionRequest.class);

        requireText(request.getAuctionId(), "auctionId must not be empty.");

        Bidder bidder = getCurrentBidder(session);

        /*
         * Sửa tại đây:
         * AuctionService không có hàm leaveAuction().
         * Với luồng UNSUBSCRIBE_AUCTION hiện tại, mục tiêu là rời phòng realtime,
         * nên phải gọi leaveLiveRoom().
         */
        boolean unsubscribed = auctionService.leaveLiveRoom(
                bidder,
                request.getAuctionId(),
                session
        );

        if (!unsubscribed) {
            throw new IllegalStateException("Unsubscribe auction failed.");
        }

        return true;
    }

    /**
     * Hủy một phiên đấu giá.
     *
     * Action tương ứng:
     * - CANCEL_AUCTION
     *
     * Body JSON cần có:
     * - auctionId
     * - reason
     */
    public Boolean cancelAuction(String bodyJson, ClientSession session) {
        CancelAuctionRequest request = parseBody(bodyJson, CancelAuctionRequest.class);

        /*
         * Sửa tại đây:
         * AuctionService.cancelAuction cần biết user hiện tại là ai
         * để ghi log người thực hiện hành động hủy phiên.
         */
        String userId = requireLoggedInUserId(session);

        requireText(request.getAuctionId(), "auctionId must not be empty.");

        String reason = request.getReason();
        if (isBlank(reason)) {
            reason = "No reason provided.";
        }

        boolean canceled = auctionService.cancelAuction(
                request.getAuctionId(),
                userId,
                reason
        );

        if (!canceled) {
            throw new IllegalStateException("Cancel auction failed.");
        }

        return true;
    }

    /**
     * Parse body JSON thành DTO cụ thể.
     */
    private <T> T parseBody(String bodyJson, Class<T> requestType) {
        if (isBlank(bodyJson)) {
            throw new IllegalArgumentException("Request body must not be empty.");
        }

        T request = gson.fromJson(bodyJson, requestType);

        if (request == null) {
            throw new IllegalArgumentException("Request body is invalid.");
        }

        return request;
    }

    /**
     * Lấy userId từ session.
     * Server dùng session làm nguồn tin cậy, không lấy userId từ request body.
     */
    private String requireLoggedInUserId(ClientSession session) {
        if (session == null || isBlank(session.getUserId())) {
            throw new IllegalStateException("User is not logged in.");
        }

        return session.getUserId();
    }

    /**
     * Lấy user hiện tại từ database.
     */
    private User getCurrentUser(ClientSession session) {
        String userId = requireLoggedInUserId(session);

        return userDAO.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Current user not found."));
    }

    /**
     * Lấy Bidder hiện tại.
     *
     * Dùng cho các action chỉ Bidder mới thực hiện được:
     * - PLACE_BID
     * - SUBSCRIBE_AUCTION
     * - UNSUBSCRIBE_AUCTION
     */
    private Bidder getCurrentBidder(ClientSession session) {
        User user = getCurrentUser(session);

        if (!(user instanceof Bidder)) {
            throw new IllegalStateException("Current user is not a bidder.");
        }

        return (Bidder) user;
    }

    /**
     * Parse chuỗi thời gian sang LocalDateTime.
     *
     * Format nên gửi từ Client:
     * - 2026-05-18T10:30:00
     */
    private LocalDateTime parseDateTime(String value, String fieldName) {
        requireText(value, fieldName + " must not be empty.");

        try {
            return LocalDateTime.parse(value);
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " must be ISO local date time, for example 2026-05-18T10:30:00.");
        }
    }

    /**
     * Kiểm tra text bắt buộc.
     */
    private void requireText(String value, String message) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Không dùng String.isBlank() để tránh lỗi nếu IDE compile nhầm language level thấp.
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}