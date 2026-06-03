package com.auction.service;

import com.auction.dao.BidTransactionDAO;
import com.auction.dao.UserDAO;
import com.auction.dao.impl.BidTransactionDAOImpl;
import com.auction.dao.impl.UserDAOImpl;
import com.auction.dto.BidTransactionDTO;
import com.auction.dto.PageDTO;
import com.auction.exception.ValidationErrorCode;
import com.auction.exception.ValidationException;
import com.auction.models.Auction.BidTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BidTransactionService {
    private final BidTransactionDAO bidTransactionDAO = new BidTransactionDAOImpl();
    private final UserDAO userDAO = new UserDAOImpl();


    /**
     * LẤY LỊCH SỬ ĐẶT GIÁ THEO PHIÊN (PHÂN TRANG TRỌN GÓI)
     */
    public PageDTO<BidTransactionDTO> getAuctionBidsPaged(String auctionId, int page, int pageSize) {
        // Hàng rào kiểm tra tham số đầu vào (Đã tốt, giữ nguyên cấu trúc sạch)
        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.MISSING_REQUIRED_FIELD, "Auction ID is required.");
        }
        // 🔥 SỬA ĐỒNG BỘ: page index thông thường bắt đầu từ 1, check <= 0 là chuẩn xác
        if (page <= 0 || pageSize <= 0) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Page index and size must be greater than 0.");
        }

        int offset = (page - 1) * pageSize;

        List<BidTransaction> rawBids = bidTransactionDAO.findByAuctionIdPaged(auctionId, pageSize, offset);

        if (rawBids == null || rawBids.isEmpty()) {
            return new PageDTO<>(new ArrayList<>(), page, 0, 0);
        }

        List<BidTransactionDTO> dtoList = convertToTransactionDTOs(rawBids);
        long totalElements = bidTransactionDAO.getTotalBidCountByAuction(auctionId);
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);

        return new PageDTO<>(dtoList, page, totalPages, totalElements);
    }

    /**
     * LẤY LỊCH SỬ ĐI ĐẤU GIÁ CỦA MỘT BIDDER (PHÂN TRANG TRỌN GÓI)
     */
    public PageDTO<BidTransactionDTO> getBidderHistoryPaged(String bidderId, int page, int pageSize) {
        if (bidderId == null || bidderId.trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.MISSING_REQUIRED_FIELD, "Bidder ID is required.");
        }
        if (page <= 0 || pageSize <= 0) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Page index and size must be greater than 0.");
        }

        int offset = (page - 1) * pageSize;

        List<BidTransaction> rawBids = bidTransactionDAO.findByBidderIdPaged(bidderId, pageSize, offset);

        if (rawBids == null || rawBids.isEmpty()) {
            return new PageDTO<>(new ArrayList<>(), page, 0, 0);
        }

        List<BidTransactionDTO> dtoList = convertToTransactionDTOs(rawBids);
        long totalElements = bidTransactionDAO.getTotalBidCountByBidder(bidderId);
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);

        return new PageDTO<>(dtoList, page, totalPages, totalElements);
    }


    /**
     * 🔥 TỐI ƯU HOÀN TOÀN: Hàm chuyển đổi dữ liệu DTO
     */
    private List<BidTransactionDTO> convertToTransactionDTOs(List<BidTransaction> rawBids) {
        if (rawBids == null || rawBids.isEmpty()) return new ArrayList<>();

        List<String> bidderIds = rawBids.stream()
                .map(BidTransaction::getBidderId)
                .distinct()
                .toList();

        // 💡 GIẢI PHÁP CHUYÊN NGHIỆP THAY THẾ N+1 QUERY:
        // Thay vì gọi userDAO.findById từng vòng lặp, bạn nên viết thêm hàm findUsernamesByIds(bidderIds) trong UserDAO.
        // Câu lệnh SQL bên dưới DAO sẽ dùng cú pháp: SELECT id, username FROM users WHERE id IN (?, ?, ?, ...)
        // Điều này gộp 20 câu lệnh SELECT đơn lẻ thành duy nhất 1 câu lệnh quét, tối ưu hóa tốc độ phản hồi gấp hàng chục lần!
        Map<String, String> userMap = userDAO.findUsernamesByIds(bidderIds);

        return rawBids.stream().map(bid -> {
            BidTransactionDTO dto = new BidTransactionDTO(
                    userMap.getOrDefault(bid.getBidderId(), "Người dùng ẩn danh"),
                    bid.getAmount(),
                    bid.getTime(),
                    bid.getStatus().name()
            );
            dto.setBidId(bid.getId());
            dto.setAuctionId(bid.getAuctionId());
            return dto;
        }).collect(Collectors.toList());
    }
}