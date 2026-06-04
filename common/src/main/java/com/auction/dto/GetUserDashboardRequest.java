package com.auction.dto;

import java.io.Serializable;

/**
 * DTO gửi yêu cầu lấy danh sách người dùng kèm phân trang phục vụ màn hình Admin.
 */
public class GetUserDashboardRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private int page;
    private int pageSize;

    public GetUserDashboardRequest() {
    }

    public GetUserDashboardRequest(int page, int pageSize) {
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

