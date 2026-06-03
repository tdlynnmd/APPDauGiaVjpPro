package com.auction.controller;

import com.auction.dto.*;
import com.auction.enums.UserRole;
import com.auction.exception.ValidationErrorCode;
import com.auction.exception.ValidationException;
import com.auction.dao.AutoBidDAO;
import com.auction.dao.impl.AutoBidDAOImpl;
import com.auction.models.Auction.AutoBid;
import com.auction.network.ClientSession;
import com.auction.service.AuctionService;
import com.auction.service.BidTransactionService;
import com.auction.config.DatabaseConnection;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * =========================================================================
 * AuctionController - Bộ điều phối chức năng đấu giá phía Server (Đã tối ưu)
 * =========================================================================
 */
public class AuctionController {
    private final AuctionService auctionService = new AuctionService();
    private final BidTransactionService bidTransactionService = new BidTransactionService();

    /**
     * Lấy danh sách các phiên đấu giá đang hoạt động
     * GET_ACTIVE_AUCTIONS
     */
    public List<AuctionSummaryDTO> getActiveAuctions(String currentUserId) {
        return auctionService.getAllActiveAuctions(currentUserId);
    }

    /**
     * Lấy chi tiết thông tin của một phiên đấu giá cụ thể
     * GET_AUCTION_DETAIL
     */
    public AuctionDetailDTO getAuctionDetail(GetAuctionDetailRequest request) {
        if (request == null) throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Yêu cầu không hợp lệ.");
        return auctionService.getAuctionDetail(request.getAuctionId());
    }

    /**
     * Tạo một phiên đấu giá hoàn toàn mới (Dành cho Seller)
     * CREATE_AUCTION
     */
    public void createAuction(String sellerId, CreateAuctionRequest request) {
        if (request == null) throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Yêu cầu không hợp lệ.");
        if (request.getStepPrice() <= 0) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "stepPrice must be greater than 0.");
        }

        LocalDateTime startTime = LocalDateTime.parse(request.getStartTime());
        LocalDateTime endTime = LocalDateTime.parse(request.getEndTime());

        if (!endTime.isAfter(startTime)) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "endTime must be after startTime.");
        }

        auctionService.createAuction(request.getItemId(), sellerId, request.getStepPrice(), startTime, endTime);
    }

    /**
     * Thực hiện đặt một bước giá mới vào phiên đấu giá
     * PLACE_BID
     */
    public void placeBid(String bidderId, PlaceBidRequest request) {
        if (request == null) throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Yêu cầu không hợp lệ.");
        if (request.getAmount() <= 0) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "amount must be greater than 0.");
        }
        // Truyền thẳng bidderId xuống, để Service tự quyết định việc lấy đối tượng từ RAM/DB
        auctionService.processBid(bidderId, request.getAuctionId(), request.getAmount());
    }

    // =========================================================================
    // 🌐 NHÓM 1: CÁC TÁC VỤ GIAO DIỆN / MẠNG TRỰC TIẾP (STATELESS LIVE ROOM)
    // =========================================================================

    public AuctionDetailDTO joinLiveRoom(String bidderId, AuctionSubscriptionRequest request, ClientSession session) {
        if (request == null) throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Yêu cầu không hợp lệ.");
        auctionService.joinLiveRoom(bidderId, request.getAuctionId(), session);
        AuctionDetailDTO detail = auctionService.getAuctionDetail(request.getAuctionId());

        // Trả về cấu hình AutoBid đang hoạt động của bidder nếu có
        // để Client có thể tự động điền lại form AutoBid khi quay lại phòng
        try (Connection conn = DatabaseConnection.getConnection()) {
            AutoBidDAO autoBidDAO = new AutoBidDAOImpl();
            Optional<AutoBid> autoBidOpt = autoBidDAO.findActiveByUserAndAuction(conn, bidderId, request.getAuctionId());
            if (autoBidOpt.isPresent() && autoBidOpt.get().isActive()) {
                AutoBid autoBid = autoBidOpt.get();
                detail.setActiveAutoBidMaxBid(autoBid.getMaxBid());
                detail.setActiveAutoBidIncrement(autoBid.getIncrement());
            }
        } catch (Exception e) {
            // Không nên fail toàn bộ request chỉ vì không đọc được AutoBid
        }

        return detail;
    }

    public void leaveLiveRoom(String bidderId, AuctionSubscriptionRequest request, ClientSession session) {
        if (request == null) throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Yêu cầu không hợp lệ.");
        auctionService.leaveLiveRoom(bidderId, request.getAuctionId(), session);
    }

    // =========================================================================
    // 💼 NHÓM 2: CÁC TÁC VỤ NGHIỆP VỤ LƯU TRỮ BỀN VỮNG (STATEFUL BUSINESS STATE)
    // =========================================================================

    public void joinAuction(String bidderId, AuctionSubscriptionRequest request) {
        if (request == null) throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Yêu cầu không hợp lệ.");
        auctionService.joinAuction(bidderId, request.getAuctionId());
    }

    public void leaveAuction(String bidderId, AuctionSubscriptionRequest request, ClientSession session) {
        if (request == null) throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Yêu cầu không hợp lệ.");
        auctionService.leaveAuction(bidderId, request.getAuctionId(), session);
    }

    // =========================================================================
    // 🛡️ NHÓM 3: SELLER TỰ HỦY PHÒNG ĐẤU GIÁ CHÍNH CHỦ
    // =========================================================================

    public void cancelAuctionBySeller(String sellerId, CancelAuctionRequest request) {
        if (request == null) throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Yêu cầu không hợp lệ.");
        String reason = (request.getReason() == null || request.getReason().trim().isEmpty())
                ? "Chủ phòng tự nguyện hủy."
                : request.getReason();

        // Gọi hàm Service dùng chung với vai trò SELLER để kích hoạt hàng rào kiểm tra chính chủ
        auctionService.cancelAuction(request.getAuctionId(), sellerId, UserRole.SELLER, reason);
    }

    public PageDTO<BidTransactionDTO> getAuctionBidHistory(GetAuctionBidsRequest request) {
        if (request == null) throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Yêu cầu không hợp lệ.");
        // 1. Kiểm tra tính toàn vẹn dữ liệu ngay tại cửa ngõ Controller
        if (request.getAuctionId() == null || request.getAuctionId().trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.MISSING_REQUIRED_FIELD, "auctionId must not be empty.");
        }
        if (request.getPage() <= 0 || request.getPageSize() <= 0) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Page and pageSize must be positive.");
        }

        // 2. Điều phối gọi xuống Service chuyên trách đọc lịch sử phòng
        return bidTransactionService.getAuctionBidsPaged(
                request.getAuctionId(),
                request.getPage(),
                request.getPageSize()
        );
    }

    public void setupAutoBid(String bidderId, SetupAutoBidRequest request) {
        if (request == null) throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Yêu cầu không hợp lệ.");
        if (request.getMaxBid() <= 0 || request.getIncrement() <= 0) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Parameters must be greater than 0.");
        }
        auctionService.setupAutoBid(bidderId, request.getAuctionId(), request.getMaxBid(), request.getIncrement());
    }

    public void cancelAutoBid(String bidderId, CancelAutoBidRequest request) {
        if (request == null) throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Yêu cầu không hợp lệ.");
        auctionService.cancelAutoBid(bidderId, request.getAuctionId());
    }

    /**
     * Lấy danh sách các phiên đấu giá do Seller tạo
     * GET_SELLER_AUCTIONS
     */
    public List<AuctionSummaryDTO> getSellerAuctions(String sellerId) {
        return auctionService.getSellerAuctions(sellerId);
    }

    /**
     * Cập nhật phiên đấu giá chưa chạy (Dành cho Seller)
     * UPDATE_AUCTION
     */
    public void updateAuction(String sellerId, UpdateAuctionRequest request) {
        if (request == null) throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Yêu cầu không hợp lệ.");
        auctionService.updateAuction(sellerId, request);
    }
}