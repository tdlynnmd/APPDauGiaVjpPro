package com.auction.exception;

public enum AuctionErrorCode {
    // ===== LIVE AUCTION ERRORS =====
    AUCTION_NOT_FOUND("AUC_ROOM_001", "Auction session does not exist on the system"),
    AUCTION_NOT_RUNNING("AUC_ROOM_002", "Auction session is not currently active or open for bidding"),
    AUCTION_CLOSED("AUC_ROOM_003", "This auction session has already finished or been cancelled"),
    BID_AMOUNT_TOO_LOW("AUC_ROOM_004", "Your bid amount must be higher than the current price plus the minimum step price"),
    CANNOT_UNWATCH_LEADING_AUCTION("AUC_ROOM_005", "You cannot unsubscribe from a live room where you are currently the highest bidder"),
    SUBSCRIBE_FAILED("AUC_ROOM_006", "Failed to register connection to this live auction room"),
    BIDDER_NOT_ONLINE("AUC_ROOM_007", "User must be online to perform bidding actions"), // 🔥 BỔ SUNG
    BIDDER_IS_SELLER("AUC_ROOM_008", "You cannot bid on your own product item"), // 🔥 BỔ SUNG

    // ===== ITEM / PRODUCT ERRORS =====
    ITEM_NOT_FOUND("AUC_ITEM_001", "Product item does not exist in the warehouse"),
    ITEM_ALREADY_EXISTS("AUC_ITEM_002", "Product item with this ID already exists"),
    ITEM_IS_LOCKED("AUC_ITEM_003", "This item is currently locked because it is live on the floor or already sold"),
    PRODUCT_NULL("AUC_ITEM_004", "Product item data must not be null"),
    INVALID_PRODUCT_PARAMETER("AUC_ITEM_005", "Invalid parameters or missing required fields for this item type"),
    DATABASE_ERROR("AUC_SYS_001", "Internal data persistence failed"), // 🔥 BỔ SUNG cho trường hợp insert/update DB hụt
    UPDATE_FAILED("AUC_SYS_002", "Database update operation failed"); // 🔥 BỔ SUNG cho trường hợp update DB hụt
    private final String code;
    private final String message;

    AuctionErrorCode(String code, String message) {
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