package com.auction.dto;

/**
 * Request DTO for loading paginated audit logs in the admin dashboard.
 * Fields are mutable so Gson can deserialize request bodies reliably.
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
