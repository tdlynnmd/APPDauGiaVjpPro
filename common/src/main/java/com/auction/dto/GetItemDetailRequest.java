package com.auction.dto;

import java.io.Serializable;

/**
 * DTO gửi yêu cầu lấy thông tin chi tiết của một sản phẩm/vật phẩm.
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
