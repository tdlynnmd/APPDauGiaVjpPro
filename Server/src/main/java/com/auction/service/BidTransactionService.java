package com.auction.service;

import com.auction.dao.BidTransactionDAO;
import com.auction.dao.UserDAO;
import com.auction.dao.impl.BidTransactionDAOImpl;
import com.auction.dao.impl.UserDAOImpl;
import com.auction.dto.BidTransactionDTO;
import com.auction.dto.PageDTO; // 🔥 Nạp lớp PageDTO dùng chung
import com.auction.models.Auction.BidTransaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BidTransactionService {
    private final BidTransactionDAO bidTransactionDAO = new BidTransactionDAOImpl();
    private final UserDAO userDAO = new UserDAOImpl();

    public boolean recordNewBid(BidTransaction bid) {
        if (bid == null) return false;
        return bidTransactionDAO.insertBid(bid);
    }

    /**
     * LẤY LỊCH SỬ ĐẶT GIÁ THEO PHIÊN (ĐÃ NÂNG CẤP PHÂN TRANG TRỌN GÓI)
     * Phục vụ màn hình quản lý phòng đấu giá nâng cao của Seller hoặc Admin kiểm toán
     */
    public PageDTO<BidTransactionDTO> getAuctionBidsPaged(String auctionId, int page, int pageSize) {
        if (auctionId == null || page <= 0 || pageSize <= 0) return null;

        int offset = (page - 1) * pageSize;

        // 1. Lấy danh sách model thô từ DB phân đoạn LIMIT OFFSET
        List<BidTransaction> rawBids = bidTransactionDAO.findByAuctionIdPaged(auctionId, pageSize, offset);

        // 2. Chuyển đổi chống lỗi N+1 query sang danh sách DTO chứa Username
        List<BidTransactionDTO> dtoList = convertToTransactionDTOs(rawBids);

        // 3. Đếm tổng số lượt bid của riêng phiên này và tính toán số trang
        long totalElements = bidTransactionDAO.getTotalBidCountByAuction(auctionId);
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);

        // 4. Trả về thực thể phân trang dùng chung
        return new PageDTO<>(dtoList, page, totalPages, totalElements);
    }

    /**
     * LẤY LỊCH SỬ ĐI ĐẤU GIÁ CỦA MỘT BIDDER (ĐÃ NÂNG CẤP PHÂN TRANG TRỌN GÓI)
     * Phục vụ tính năng Tab "Lịch sử đi chợ / Lịch sử ví" của Bidder trên Client JavaFX
     */
    public PageDTO<BidTransactionDTO> getBidderHistoryPaged(String bidderId, int page, int pageSize) {
        if (bidderId == null || page <= 0 || pageSize <= 0) return null;

        int offset = (page - 1) * pageSize;

        // 1. Lấy danh sách đặt giá phân đoạn của riêng User này
        List<BidTransaction> rawBids = bidTransactionDAO.findByBidderIdPaged(bidderId, pageSize, offset);

        // 2. Convert sang DTO
        List<BidTransactionDTO> dtoList = convertToTransactionDTOs(rawBids);

        // 3. Đếm tổng số lượt bid của User này dưới DB để làm Metadata lật trang
        long totalElements = bidTransactionDAO.getTotalBidCountByBidder(bidderId);
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);

        return new PageDTO<>(dtoList, page, totalPages, totalElements);
    }

    // =========================================================================
    // PRIVATE HELPER - BIẾN ĐỔI DATA CHỐNG LỖI N+1 QUERY (GIỮ NGUYÊN)
    // =========================================================================
    private List<BidTransactionDTO> convertToTransactionDTOs(List<BidTransaction> rawBids) {
        if (rawBids == null || rawBids.isEmpty()) return new ArrayList<>();

        List<String> bidderIds = rawBids.stream()
                .map(BidTransaction::getBidderId)
                .distinct()
                .toList();

        Map<String, String> userMap = bidderIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> userDAO.findById(id)
                                .map(com.auction.models.User.User::getUsername)
                                .orElse("Người dùng ẩn danh")
                ));

        return rawBids.stream().map(bid -> new BidTransactionDTO(
                userMap.getOrDefault(bid.getBidderId(), "Người dùng ẩn danh"),
                bid.getAmount(),
                bid.getTime(),
                bid.getStatus().name()
        )).collect(Collectors.toList());
    }
}