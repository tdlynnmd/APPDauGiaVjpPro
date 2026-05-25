package com.auction.dto;

import java.io.Serializable;

/**
 * Request DTO used when Seller/Admin hides an item from normal item flows.
 */
public class DeleteItemRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String itemId;
    private String reason;

    public DeleteItemRequest() {
    }

    public DeleteItemRequest(String itemId, String reason) {
        this.itemId = itemId;
        this.reason = reason;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
