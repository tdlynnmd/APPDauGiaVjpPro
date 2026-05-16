package com.auction.dto;

import java.time.LocalDateTime;
import java.util.List;

// 1. DTO cho từng dòng Log
public class ActionLogDTO {
    private String logId;
    private String actionDetail;
    private LocalDateTime timestamp;

    public ActionLogDTO(String logId, String actionDetail, LocalDateTime timestamp) {
        this.logId = logId;
        this.actionDetail = actionDetail;
        this.timestamp = timestamp;
    }
    // Getters...
}
