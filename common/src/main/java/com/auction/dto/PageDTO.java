package com.auction.dto;

import java.io.Serializable;
import java.util.List;

/**
 * DTO tổng quát hỗ trợ đóng gói dữ liệu phân trang (Pagination) trả về cho Client.
 */
public class PageDTO<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<T> data;
    private int currentPage;
    private int totalPages;
    private long totalElements;

    public PageDTO(List<T> data, int currentPage, int totalPages, long totalElements) {
        this.data = data;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
    }

    public List<T> getData() { return data; }
    public void setData(List<T> data) { this.data = data; }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
}