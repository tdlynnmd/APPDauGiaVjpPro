package com.auction.dto;

public class GetAuctionDetailRequest {
    /**
     Luồng:
     CLient chọn 1 phiên đấu giá
     -> Gửi aciton GET_AUCTION_DETAIL
     -> Body chứa auctionId
     -> Server dùng auctionId để tìm chi tiết phiên
     */
    private String auctionId;
    public GetAuctionDetailRequest(){}      // Constructor rỗng cần cho Gson khi parse JSON thành object
    public GetAuctionDetailRequest(String auctionId){       // Constructor dung khi Client tao request
        this.auctionId = auctionId;
    }
    public String getAuctionId() {
        return auctionId;
    }

}
