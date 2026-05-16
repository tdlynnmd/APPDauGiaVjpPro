package com.auction.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class BidTransactionDTO implements Serializable {
    private String bidderName; // Chứa Tên hoặc Username thay vì ID ẩn danh
    private double amount;
    private LocalDateTime time;
    private String status;

    public BidTransactionDTO(String bidderName, double amount, LocalDateTime time, String status) {
        this.bidderName = bidderName;
        this.amount = amount;
        this.time = time;
        this.status = status;
    }

    // Getters và Setters...
}