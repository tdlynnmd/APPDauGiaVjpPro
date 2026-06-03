package com.auction.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auction.enums.ActionType;
import com.auction.enums.UserRole;
import com.auction.exception.AuthorizationErrorCode;
import com.auction.exception.AuthorizationException;
import com.auction.network.ClientSession;

import java.util.Map;
import java.util.Set;

/**
 * =========================================================================
 * AuthorizationService - Trung tâm kiểm soát và phân quyền tối cao phía Server
 * =========================================================================
 */
public class AuthorizationService {
    private static final Logger log = LoggerFactory.getLogger(AuthorizationService.class);

    /**
     * 1. PUBLIC ACTIONS: Hoàn toàn mở, không cần check Session đăng nhập
     */
    private static final Set<String> PUBLIC_ACTIONS = Set.of(
            ActionType.LOGIN.name(),
            ActionType.REGISTER.name(),
            ActionType.PING.name()           // Keep-alive: cho phép client chưa đăng nhập gửi PING
    );

    /**
     * 2. LOGIN REQUIRED ACTIONS: Chỉ cần đăng nhập thành công là có quyền gọi,
     * không phân biệt vai trò (Bao gồm cả các tính năng xem thông tin chung mới bổ sung)
     */
    private static final Set<String> LOGIN_REQUIRED_ACTIONS = Set.of(
            ActionType.LOGOUT.name(),
            ActionType.GET_ACTIVE_AUCTIONS.name(),
            ActionType.GET_AUCTION_DETAIL.name(),
            ActionType.GET_USER_PROFILE.name(),        // Ai cũng có quyền xem profile của chính mình
            ActionType.GET_AUCTION_BID_HISTORY.name()  // Lịch sử đặt giá phòng live mở công khai cho mọi người xem
    );

    /**
     * 3. ROLE PERMISSIONS: Whitelist kiểm soát nghiêm ngặt theo từng Actor đặc thù.
     * Chia nhóm rõ ràng để sau này hệ thống phình to ra vẫn cực kỳ dễ bảo trì.
     */
    private static final Map<String, Set<UserRole>> ROLE_PERMISSIONS = Map.ofEntries(
            // -----------------------------------------------------------------
            // PHÂN HỆ DÀH CHO NGƯỜI BÁN (SELLER)
            // -----------------------------------------------------------------
            Map.entry(ActionType.CREATE_ITEM.name(), Set.of(UserRole.SELLER)),
            Map.entry(ActionType.UPDATE_ITEM.name(), Set.of(UserRole.SELLER)),
            Map.entry(ActionType.GET_SELLER_ITEMS.name(), Set.of(UserRole.SELLER)),
            Map.entry(ActionType.GET_ITEM_DETAIL.name(), Set.of(UserRole.SELLER, UserRole.ADMIN)),
            Map.entry(ActionType.CREATE_AUCTION.name(), Set.of(UserRole.SELLER)),
            Map.entry(ActionType.SELLER_CANCEL_AUCTION.name(), Set.of(UserRole.SELLER)), // Seller tự hủy phòng trống
            Map.entry(ActionType.SELLER_DELETE_ITEM.name(), Set.of(UserRole.SELLER)),   // Seller tự xóa sản phẩm chưa đấu giá
            Map.entry(ActionType.GET_SELLER_AUCTIONS.name(), Set.of(UserRole.SELLER)),
            Map.entry(ActionType.UPDATE_AUCTION.name(), Set.of(UserRole.SELLER)),
            Map.entry(ActionType.UPDATE_PROFILE.name(), Set.of(UserRole.ADMIN, UserRole.SELLER, UserRole.BIDDER)),
            Map.entry(ActionType.UPDATE_PASSWORD.name(), Set.of(UserRole.ADMIN, UserRole.SELLER, UserRole.BIDDER)),

            // -----------------------------------------------------------------
            // PHÂN HỆ DÀNH CHO NGƯỜI ĐẤU GIÁ (BIDDER)
            // -----------------------------------------------------------------
            Map.entry(ActionType.DEPOSIT_MONEY.name(), Set.of(UserRole.BIDDER)),
            Map.entry(ActionType.WITHDRAW_MONEY.name(), Set.of(UserRole.BIDDER)),
            Map.entry(ActionType.PLACE_BID.name(), Set.of(UserRole.BIDDER)),
            Map.entry(ActionType.LIVE_ENTERED.name(), Set.of(UserRole.BIDDER, UserRole.SELLER, UserRole.ADMIN)),
            Map.entry(ActionType.LIVE_EXITED.name(), Set.of(UserRole.BIDDER, UserRole.SELLER, UserRole.ADMIN)),
            Map.entry(ActionType.AUCTION_SUBSCRIBED.name(), Set.of(UserRole.BIDDER)),
            Map.entry(ActionType.AUCTION_UNSUBSCRIBED.name(), Set.of(UserRole.BIDDER)),
            Map.entry(ActionType.GET_MY_BID_HISTORY.name(), Set.of(UserRole.BIDDER)),   // Xem lịch sử đi chợ của cá nhân
            Map.entry(ActionType.SETUP_AUTO_BID.name(), Set.of(UserRole.BIDDER)),        // Đăng ký thầu tự động
            Map.entry(ActionType.CANCEL_AUTO_BID.name(), Set.of(UserRole.BIDDER)),       // Hủy thầu tự động

            // -----------------------------------------------------------------
            // PHÂN HỆ QUẢN TRỊ TỐI CAO (ADMIN)
            // -----------------------------------------------------------------
            Map.entry(ActionType.CMD_ADMIN_GET_USERS.name(), Set.of(UserRole.ADMIN)),
            Map.entry(ActionType.CMD_ADMIN_LOCK_USER.name(), Set.of(UserRole.ADMIN)),
            Map.entry(ActionType.CMD_ADMIN_CANCEL_AUCTION.name(), Set.of(UserRole.ADMIN)), // Admin cưỡng chế hủy
            Map.entry(ActionType.CMD_ADMIN_DELETE_ITEM.name(), Set.of(UserRole.ADMIN)),   // Admin cưỡng chế hạ tải
            Map.entry(ActionType.CMD_ADMIN_GET_LOGS.name(), Set.of(UserRole.ADMIN))       // Xem nhật ký hệ thống
    );

    /**
     * Kiểm tra phiên Session hiện tại có đủ thẩm quyền thực thi tác vụ hay không
     */
    public void canAccess(String action, ClientSession session) {
        if (action == null || action.trim().isEmpty()) {
            throw new AuthorizationException(AuthorizationErrorCode.ACTION_UNAUTHORIZED, "Action type cannot be null or empty");
        }

        // Bước 1: Cho qua ngay nếu là Action công khai hoàn toàn
        if (PUBLIC_ACTIONS.contains(action)) {
            return;
        }

        // Bước 2: Chặn cưỡng chế nếu chưa thiết lập trạng thái Session hợp lệ
        if (session == null || !session.isLoggedIn()) {
            throw new AuthorizationException(AuthorizationErrorCode.NOT_AUTHENTICATED);
        }

        // Bước 3: Cho qua nếu Action chỉ yêu cầu duy nhất điều kiện đã đăng nhập
        if (LOGIN_REQUIRED_ACTIONS.contains(action)) {
            return;
        }

        // Bước 4: Trích xuất cấu hình quyền hạn nghiêm ngặt theo Role
        Set<UserRole> allowedRoles = ROLE_PERMISSIONS.get(action);

        // Chốt chặn an toàn (Fail-Safe): Nếu Action lạ chưa được khai báo ở trên, cấm tuyệt đối truy cập
        if (allowedRoles == null) {
            log.warn("[Guard] 🚨 Cảnh báo bảo mật: Phát hiện request gọi Action chưa được cấu hình: {}", action);
            throw new AuthorizationException(AuthorizationErrorCode.ACTION_UNAUTHORIZED);
        }

        // Bước 5: Kiểm tra xem vai trò của Session có nằm trong Whitelist cho phép hay không
        if (!allowedRoles.contains(session.getRole())) {
            log.warn("[Guard] ⛔ Từ chối truy cập: User [{}] mang Role [{}] cố tình gọi Action hạn chế [{}]",
                    session.getUserId(), session.getRole(), action);
            throw new AuthorizationException(AuthorizationErrorCode.ROLE_ACCESS_DENIED);
        }
    }
}