package com.auction.dto;

/**
 * DTO gửi yêu cầu lấy danh sách nhật ký kiểm toán hệ thống kèm phân trang và lọc.
 */
public class GetAuditLogsRequest {
    private int page;
    private int pageSize;

    public GetAuditLogsRequest() {
        this.page = 1;
        this.pageSize = 10;
    }

    public GetAuditLogsRequest(int page, int pageSize) {
        this.page = page;
        this.pageSize = pageSize;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
