package com.auction.dto;

import com.auction.dto.ActionLogDTO;

import java.util.List;

// 2. DTO gói cả trang lại để gửi về Client
public class LogPageDTO {
    private int currentPage;
    private int totalPages;
    private List<ActionLogDTO> logs;

    public LogPageDTO(int currentPage, int totalPages, List<ActionLogDTO> logs) {
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.logs = logs;
    }
    // Getters...
}