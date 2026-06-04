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
import com.auction.models.Auction.Auction;
import com.auction.manage.AuctionManage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dịch vụ quản lý lịch sử giao dịch đặt giá (Bid History).
 */
public class BidTransactionService {
    private final BidTransactionDAO bidTransactionDAO = new BidTransactionDAOImpl();
    private final UserDAO userDAO = new UserDAOImpl();

    public PageDTO<BidTransactionDTO> getAuctionBidsPaged(String auctionId, int page, int pageSize) {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.MISSING_REQUIRED_FIELD, "Auction ID is required.");
        }
        if (page <= 0 || pageSize <= 0) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Page index and size must be greater than 0.");
        }

        Auction auction = AuctionManage.getInstance().getAuctionById(auctionId);
        if (auction != null) {
            List<BidTransaction> ramHistory = auction.getBidHistoryRam();
            if (ramHistory == null || ramHistory.isEmpty()) {
                return new PageDTO<>(new ArrayList<>(), page, 0, 0);
            }
            int offset = (page - 1) * pageSize;
            if (offset >= ramHistory.size()) {
                int totalPages = (int) Math.ceil((double) ramHistory.size() / pageSize);
                return new PageDTO<>(new ArrayList<>(), page, totalPages, ramHistory.size());
            }
            List<BidTransaction> subList = ramHistory.subList(offset, Math.min(offset + pageSize, ramHistory.size()));
            List<BidTransactionDTO> dtoList = convertToTransactionDTOs(subList);
            long totalElements = ramHistory.size();
            int totalPages = (int) Math.ceil((double) totalElements / pageSize);
            return new PageDTO<>(dtoList, page, totalPages, totalElements);
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

    private List<BidTransactionDTO> convertToTransactionDTOs(List<BidTransaction> rawBids) {
        if (rawBids == null || rawBids.isEmpty()) return new ArrayList<>();

        List<String> bidderIds = rawBids.stream()
                .map(BidTransaction::getBidderId)
                .distinct()
                .toList();

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