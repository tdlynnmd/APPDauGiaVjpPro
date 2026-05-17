package com.auction.service;

import com.auction.enums.UserRole;
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
    private static final Set<String> PUBLIC_ACTIONS = Set.of("LOGIN", "REGISTER");

    /**
     Action chỉ ần user đã đăng nhập, ko phân biệt role
     */
    private static final Set<String> LOGIN_REQUIRED_ACTIONS = Set.of("LOGOUT", "GET_AUCTION_LIST", "GET_AUCTION_DETAIL");

    /**
     Phân quyền theo role dùng Map với Key: tên action Client gửi lên
                                        Value: danh sách role được phép thực hiện action
     */
    private static final Map<String, Set<UserRole>> ROLE_PERMISSIONS = Map.of(
            "CREATE_AUCTION", Set.of(UserRole.SELLER),
            "UPDATE_AUCTION", Set.of(UserRole.SELLER),
            "DELETE_AUCTION", Set.of(UserRole.SELLER),

            "PLACE_BID", Set.of(UserRole.BIDDER),

            "MANAGE_USERS", Set.of(UserRole.ADMIN),
            "LOCK_USER", Set.of(UserRole.ADMIN),
            "UNLOCK_USER", Set.of(UserRole.ADMIN)
            );

    /**
     Kiểm tra session hiện tai có được phép chạy action hay ko
     */
    public boolean canAccess(String action, ClientSession session){
        if (action == null || action.isBlank()){
            return false;
        }

        //LOGIN va REGISTER ko can dang nhap
        if (PUBLIC_ACTIONS.contains(action)){
            return true;
        }

        // Các action còn lại đều cần có session.
        if (session == null || !session.isLoggedIn()) {
            return false;
        }

        // Các action chỉ cần login, không cần xét role cụ thể.
        if (LOGIN_REQUIRED_ACTIONS.contains(action)) {
            return true;
        }

        // Lấy danh sách role được phép thực hiện action
        Set<UserRole> allowedRoles = ROLE_PERMISSIONS.get(action);

        // Nếu action không nằm trong bảng phân quyền,
        // mặc định không cho chạy để tránh hở quyền.
        if (allowedRoles == null) {
            return false;
        }

        // Kiểm tra role hiện tại có thuộc nhóm được phép không.
        return allowedRoles.contains(session.getRole());
    }
}
