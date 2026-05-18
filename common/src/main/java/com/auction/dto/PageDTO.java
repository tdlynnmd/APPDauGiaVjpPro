package com.auction.dto;

import java.io.Serializable;
import java.util.List;

public class PageDTO<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<T> data;          // Danh sách dữ liệu (Có thể là List<BidTransactionDTO>, List<UserDTO>...)
    private int currentPage;       // Trang hiện tại
    private int totalPages;        // Tổng số trang
    private long totalElements;    // Tổng số dòng dưới DB

    public PageDTO(List<T> data, int currentPage, int totalPages, long totalElements) {
        this.data = data;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
    }

    // =========================================================================
    // GETTERS & SETTERS
    // =========================================================================
    public List<T> getData() { return data; }
    public void setData(List<T> data) { this.data = data; }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
}