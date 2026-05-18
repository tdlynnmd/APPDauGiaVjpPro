package com.auction.enums;

public enum ActionType {
    /**
     Danh sách chuẩn tất cả action mà Client được phép gửi lên Server
     Cần Enum vì:
     - Tránh gõ sai chuỗi action ở nhiều nơi.
     - Client và Server dùng chung một danh sách action.
     - Khi thêm action mới, chỉ cần thêm vào đây trước.
     */
    LOGIN,
    REGISTER,
    LOGOUT,
    GET_ACTIVE_AUCTIONS,        // lấy danh sách các phiên đáu giá đang mở hoặc đang chạy.
    GET_AUCTION_DETAIL,         // lấy chi tiết 1 phiên đáu giá theo auctioId
    CREATE_AUCTION,             // tạo phiên đấu giá mới, chỉ seller và admin được gọi
    PLACE_BID,                  // đặt giá vào 1 phiên dddassu giá, chỉ bidder
    SUBSCRIBE_AUCTION,          // đăng kí nhận Realtime Update của 1 phiên đấu giá, dùng khi Client mở màn hình chi tiết phiên
    UNSUBSCRIBE_AUCTION,        // hủy đăng kí Realtime Update, dùng khi Client rời màn hình chi tiết phiên
    CANCEL_AUCTION              // hủy phiên đấu giá, seller hoặc admin được gọi, sau này nên kiểm tra thêm Seller chỉ dc xóa của chính mình

}
