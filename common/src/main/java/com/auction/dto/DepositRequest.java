package com.auction.dto;

import java.io.Serializable;

/**
 * DTO gửi yêu cầu nạp tiền vào ví khả dụng.
 */
public class DepositRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private double amount;

    public DepositRequest() {
    }

    public DepositRequest(double amount) {
        this.amount = amount;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}

