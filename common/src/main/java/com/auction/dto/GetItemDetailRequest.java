package com.auction.dto;

import java.io.Serializable;

/**
 * Request DTO used when the client asks for full detail of one item.
 */
public class GetItemDetailRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String itemId;

    public GetItemDetailRequest() {
    }

    public GetItemDetailRequest(String itemId) {
        this.itemId = itemId;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
}
