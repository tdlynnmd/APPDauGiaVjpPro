package com.auction.dto;

/**
 * DTO gửi yêu cầu lấy thông tin chi tiết của một phiên đấu giá cụ thể.
 */
public class GetAuctionDetailRequest {
    
    private String auctionId;
    public GetAuctionDetailRequest(){}
    public GetAuctionDetailRequest(String auctionId){
        this.auctionId = auctionId;
    }
    public String getAuctionId() {
        return auctionId;
    }

}
