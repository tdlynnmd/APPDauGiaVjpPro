package com.auction.exception;

public enum WalletErrorCode {
    // ===== WALLET & FINANCIAL ERRORS =====
    INSUFFICIENT_BALANCE("WAL_ACC_001", "Your available balance is insufficient to place this bid"),
    WALLET_LOCKED("WAL_ACC_002", "Financial wallet is currently locked for audit or transaction dispute"),
    FREEZE_MONEY_FAILED("WAL_ACC_003", "System failed to hold/freeze the bidding amount securely"),
    UNFREEZE_MONEY_FAILED("WAL_ACC_004", "System failed to release the frozen amount for outbid user"),
    DEDUCTION_FAILED("WAL_ACC_005", "Failed to permanently deduct money from the winner's wallet"),
    TRANSACTION_FAILED("WAL_ACC_006", "Wallet transaction persistence failed"),
    ACCOUNT_BANNED("WAL_ACC_007", "Account is currently banned from performing financial transactions.");

    private final String code;
    private final String message;

    WalletErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return message;
    }
}