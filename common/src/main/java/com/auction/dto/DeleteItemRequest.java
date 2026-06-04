package com.auction.dto;

import java.io.Serializable;

/**
 * DTO gửi yêu cầu xóa sản phẩm từ người bán hoặc quản trị viên.
 */
public class DeleteItemRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String itemId;
    private String reason;
    private String userId;

    public DeleteItemRequest() {
    }

    public DeleteItemRequest(String itemId, String reason, String userId) {
        this.itemId = itemId;
        this.reason = reason;
        this.userId = userId;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}