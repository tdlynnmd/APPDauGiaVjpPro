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
    CREATE_ITEM,
    UPDATE_ITEM,
    SELLER_DELETE_ITEM,
    GET_SELLER_ITEMS,
    GET_ITEM_DETAIL,
    GET_ACTIVE_AUCTIONS,        // lấy danh sách các phiên đáu giá đang mở hoặc đang chạy.
    GET_AUCTION_DETAIL,         // lấy chi tiết 1 phiên đáu giá theo auctioId
    CREATE_AUCTION,             // tạo phiên đấu giá mới, chỉ seller và admin được gọi
    PLACE_BID,                  // đặt giá vào 1 phiên dddassu giá, chỉ bidder
    SELLER_CANCEL_AUCTION,              // hủy phiên đấu giá, seller gọi
    GET_SELLER_AUCTIONS,        // lấy danh sách phiên do seller tạo
    UPDATE_AUCTION,             // cập nhật thông số phiên chưa chạy, seller gọi
    BID_UPDATE,
    TIME_UPDATE,
    STATUS_UPDATED,
    FORCE_LOGOUT,
    GET_AUCTION_BID_HISTORY, // lấy lịch sử đặt giá của một phiên đấu giá, chỉ bidder mới được gọi
    GET_MY_BID_HISTORY, // lấy lịch sử đặt giá của một người dùng, chỉ bidder mới được gọi

    /**
     * Sự kiện: Người dùng đăng ký theo dõi phiên (Business State)
     * Payload: Map{username, message, viewerCount}
     * Khi: joinAuction() trong AuctionService
     * Mục đích: Ghi nhận tính toàn vẹn dữ liệu trong DB/RAM
     */
    AUCTION_SUBSCRIBED,

    /**
     * Sự kiện: Người dùng hủy đăng ký theo dõi phiên (Business State)
     * Payload: Map{username, message, viewerCount}
     * Khi: leaveAuction() trong AuctionService
     * Mục đích: Xóa kiểm toàn từ DB/RAM + Notify clients
     */
    AUCTION_UNSUBSCRIBED,

    /**
     * Sự kiện: Người dùng mở tab chi tiết để xem real-time (UI/Network State)
     * Payload: Map{username, message, viewerCount}
     * Khi: joinLiveRoom() trong AuctionService
     * Mục đích: Thêm ClientSession vào phòng + Broacast viewer count tăng
     */
    LIVE_ENTERED,

    /**
     * Sự kiện: Người dùng đóng tab chi tiết nhưng vẫn tracking (UI/Network State)
     * Payload: Map{username, message, viewerCount}
     * Khi: leaveLiveRoom() trong AuctionService
     * Mục đích: Xóa ClientSession khỏi phòng + Broadcast viewer count giảm
     */
    LIVE_EXITED,

    // =========================================================================
    // USER MANAGEMENT ACTIONS
    // =========================================================================
    GET_USER_PROFILE,           // lấy thông tin profile của người dùng hiện tại
    UPDATE_PROFILE,             // cập nhật thông tin cá nhân (username, email)
    UPDATE_PASSWORD,            // thay đổi mật khẩu
    DEPOSIT_MONEY,              // nạp tiền vào tài khoản (bidder)
    WITHDRAW_MONEY,             // rút tiền từ tài khoản (bidder)
    CMD_ADMIN_GET_USERS,         // xem danh sách user (admin only)
    CMD_ADMIN_LOCK_USER,         // khóa tài khoản user (admin only)
    CMD_ADMIN_GET_LOGS,          // xem danh sách log hệ thống (admin only)
    CMD_ADMIN_CANCEL_AUCTION,     // hủy phiên đấu giá (admin only)
    CMD_ADMIN_DELETE_ITEM,        // xóa vật phẩm (admin only)
    PING,                         // keep-alive: Client gửi định kỳ để giữ kết nối Socket sống
    SETUP_AUTO_BID,               // thiết lập tự động đấu giá (bidder)
    CANCEL_AUTO_BID,              // hủy tự động đấu giá (bidder)
}
