package com.auction.dto;

import java.io.Serializable;

/**
 * DTO gửi yêu cầu rút tiền từ ví điện tử về tài khoản ngân hàng.
 */
public class WithdrawRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private double amount;

    public WithdrawRequest() {
    }

    public WithdrawRequest(double amount) {
        this.amount = amount;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}

