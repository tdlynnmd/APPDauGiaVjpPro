package com.auction.service;

import com.auction.enums.ActionType;
import com.auction.enums.UserRole;
import com.auction.exception.AuthorizationErrorCode;
import com.auction.exception.AuthorizationException;
import com.auction.network.ClientSession;

import java.util.Map;
import java.util.Set;

/**
 AuthorizationService chịu trách nhiệm kiểm tra phân quyền phía Server
 Class này trả lời câu hỏi: "User hiện tại có được phép thực hiện action hay ko?"

 -DashboardController chỉ ẩn/hiện button ở Client.
 - Client không đáng tin tuyệt đối vì người dùng có thể sửa request.
 - Vì vậy mọi action quan trọng đều phải được Server kiểm tra lại.
 */

public class AuthorizationService {
    /**
     Action này ko cần login
     Ví dụ: LOGIN: chưa đăng nhập thì cần login
     REGISTER: người dùng chưa có tài khoản vẫn được đăng kí
     */
    private static final Set<String> PUBLIC_ACTIONS = Set.of(
            ActionType.LOGIN.name(),
            ActionType.REGISTER.name()
    );

    /**
     Action chỉ cần user đã đăng nhập, ko phân biệt role
     */
    private static final Set<String> LOGIN_REQUIRED_ACTIONS = Set.of(
            ActionType.LOGOUT.name(),
            ActionType.GET_ACTIVE_AUCTIONS.name(),
            ActionType.GET_AUCTION_DETAIL.name(),
            ActionType.AUCTION_SUBSCRIBED.name(),
            ActionType.AUCTION_UNSUBSCRIBED.name()
    );

    /**
     Phân quyền theo role dùng Map với Key: tên action Client gửi lên
     Value: danh sách role được phép thực hiện action
     */
    private static final Map<String, Set<UserRole>> ROLE_PERMISSIONS = Map.of(
            ActionType.CREATE_ITEM.name(), Set.of(UserRole.SELLER, UserRole.ADMIN),
            ActionType.UPDATE_ITEM.name(), Set.of(UserRole.SELLER, UserRole.ADMIN),
            ActionType.DELETE_ITEM.name(), Set.of(UserRole.SELLER, UserRole.ADMIN),
            ActionType.GET_SELLER_ITEMS.name(), Set.of(UserRole.SELLER, UserRole.ADMIN),
            ActionType.GET_ITEM_DETAIL.name(), Set.of(UserRole.SELLER, UserRole.ADMIN),

            ActionType.CREATE_AUCTION.name(), Set.of(UserRole.SELLER, UserRole.ADMIN),
            ActionType.CANCEL_AUCTION.name(), Set.of(UserRole.SELLER, UserRole.ADMIN),

            ActionType.PLACE_BID.name(), Set.of(UserRole.BIDDER)
    );

    /**
     Kiểm tra session hiện tai có được phép chạy action hay ko
     */
    public void canAccess(String action, ClientSession session){
        // Kiểm tra tính hợp lệ thô của chuỗi hành động gửi lên
        if (action == null || action.trim().isEmpty()) {
            throw new AuthorizationException(AuthorizationErrorCode.ACTION_UNAUTHORIZED, "Action type cannot be null or empty");
        }

        // LOGIN và REGISTER không cần đăng nhập -> Cho qua ngay lập tức
        if (PUBLIC_ACTIONS.contains(action)) {
            return;
        }

        // Kiểm tra xem User đã thiết lập Session đăng nhập hợp lệ chưa
        if (session == null || !session.isLoggedIn()) {
            throw new AuthorizationException(AuthorizationErrorCode.NOT_AUTHENTICATED);
        }

        // Các action chỉ cần login, không cần xét quyền hạn vai trò cụ thể -> Cho qua
        if (LOGIN_REQUIRED_ACTIONS.contains(action)) {
            return;
        }

        // Bốc tách danh sách cấu hình phân quyền vai trò cho Action tương ứng
        Set<UserRole> allowedRoles = ROLE_PERMISSIONS.get(action);

        // Nếu action không nằm trong danh mục cấu hình, mặc định chặn cứng để bảo vệ hệ thống
        if (allowedRoles == null) {
            System.err.println("[Guard] 🚨 Cảnh báo bảo mật: Phát hiện request gọi Action chưa được cấu hình: " + action);
            throw new AuthorizationException(AuthorizationErrorCode.ACTION_UNAUTHORIZED);
        }

        // Kiểm tra xem Vai trò hiện tại của Session có nằm trong Whitelist được cho phép hay không
        if (!allowedRoles.contains(session.getRole())) {
            System.err.println("[Guard] ⛔ Từ chối truy cập: User [" + session.getUserId()
                    + "] mang Role [" + session.getRole() + "] cố tình gọi Action hạn chế [" + action + "]");

            throw new AuthorizationException(AuthorizationErrorCode.ROLE_ACCESS_DENIED);
        }
    }
}
