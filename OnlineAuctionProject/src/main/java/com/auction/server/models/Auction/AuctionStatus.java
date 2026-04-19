package com.auction.server.models.Auction;

public enum AuctionStatus {
    OPEN,      // Phiên vừa tạo, chưa bắt đầu
    RUNNING,   // Đang trong thời gian đấu giá
    FINISHED,  // Đã hết thời gian
    PAID,      // Người thắng đã thanh toán
    CANCELED   // Phiên bị hủy do lỗi hoặc vi phạm
}
