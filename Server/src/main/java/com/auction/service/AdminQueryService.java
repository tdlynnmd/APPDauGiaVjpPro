package com.auction.service;

import com.auction.dao.AuctionDAO;
import com.auction.dao.ItemDAO;
import com.auction.dao.impl.AuctionDAOImpl;
import com.auction.dao.impl.ItemDAOImpl;
import com.auction.dto.AuctionSummaryDTO;
import com.auction.dto.ItemSummaryDTO;
import com.auction.dto.PageDTO;
import com.auction.exception.ValidationErrorCode;
import com.auction.exception.ValidationException;
import com.auction.manage.AuctionManage;
import com.auction.models.Auction.Auction;
import com.auction.models.Item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dịch vụ cung cấp các truy vấn dữ liệu thống kê, quản trị dành riêng cho màn hình Admin.
 */
public class AdminQueryService {

    private final ItemDAO itemDAO = new ItemDAOImpl();
    private final AuctionDAO auctionDAO = new AuctionDAOImpl();
    private final AuctionManage auctionManage = AuctionManage.getInstance();

    public PageDTO<ItemSummaryDTO> getAdminItemDashboard(int page, int pageSize) {
        if (page <= 0 || pageSize <= 0) {
            throw new ValidationException(
                    ValidationErrorCode.INVALID_PARAMETER,
                    "Page index and page size must be positive.");
        }

        int offset = (page - 1) * pageSize;
        List<Item> dbItems = itemDAO.findAllPaginated(pageSize, offset);
        List<ItemSummaryDTO> dtoList = new ArrayList<>();

        for (Item item : dbItems) {
            dtoList.add(new ItemSummaryDTO(
                    item.getId(),
                    item.getName(),
                    item.getStartingPrice(),
                    item.getItemType().name(),
                    item.getStatus().name()
            ));
        }

        long totalElements = itemDAO.countAllItems();
        int totalPages;
        if (totalElements > 0) {
            totalPages = (int) Math.ceil((double) totalElements / pageSize);
        } else {
            totalPages = dtoList.isEmpty() ? 0 : 1;
        }

        return new PageDTO<>(dtoList, page, totalPages, totalElements);
    }

    public PageDTO<AuctionSummaryDTO> getAdminAuctionDashboard(int page, int pageSize) {
        if (page <= 0 || pageSize <= 0) {
            throw new ValidationException(
                    ValidationErrorCode.INVALID_PARAMETER,
                    "Page index and page size must be positive.");
        }

        int offset = (page - 1) * pageSize;
        List<Auction> dbAuctions = auctionDAO.findAllPaginated(pageSize, offset);

        Map<String, Auction> ramSnapshot = auctionManage.getAllActive()
                .stream()
                .collect(Collectors.toMap(Auction::getId, a -> a, (a, b) -> a));

        List<AuctionSummaryDTO> dtoList = new ArrayList<>();
        for (Auction dbAuction : dbAuctions) {
            Auction ramAuction = ramSnapshot.get(dbAuction.getId());

            double displayPrice = (ramAuction != null)
                    ? ramAuction.getCurrentPrice()
                    : dbAuction.getCurrentPrice();

            String displayStatus = dbAuction.getStatus().name();

            dtoList.add(new AuctionSummaryDTO(
                    dbAuction.getId(),
                    dbAuction.getItemId(),
                    displayPrice,
                    displayStatus,
                    dbAuction.getEndTime(),
                    dbAuction.getStartTime(),
                    dbAuction.getStepPrice()
            ));
        }

        long totalElements = auctionDAO.countAllAuctions();
        int totalPages;
        if (totalElements > 0) {
            totalPages = (int) Math.ceil((double) totalElements / pageSize);
        } else {
            totalPages = dtoList.isEmpty() ? 0 : 1;
        }

        return new PageDTO<>(dtoList, page, totalPages, totalElements);
    }
}
