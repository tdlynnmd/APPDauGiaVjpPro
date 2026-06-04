package com.auction.dto;

/**
 * DTO gửi yêu cầu lấy lịch sử đấu giá cá nhân dưới dạng phân trang.
 */
public class GetBidderHistoryRequest {
    private int page;
    private int pageSize;

    public GetBidderHistoryRequest() {}

    public GetBidderHistoryRequest(int page, int pageSize) {
        this.page = page;
        this.pageSize = pageSize;
    }

    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
}