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
 * Dịch vụ kiểm tra và phân quyền truy cập (Role-based access control) đối với các hành động mạng.
 */
public class AuthorizationService {
    private static final Logger log = LoggerFactory.getLogger(AuthorizationService.class);

    private static final Set<String> PUBLIC_ACTIONS = Set.of(
            ActionType.LOGIN.name(),
            ActionType.REGISTER.name(),
            ActionType.PING.name()
    );

    private static final Set<String> LOGIN_REQUIRED_ACTIONS = Set.of(
            ActionType.LOGOUT.name(),
            ActionType.GET_ACTIVE_AUCTIONS.name(),
            ActionType.GET_AUCTION_DETAIL.name(),
            ActionType.GET_USER_PROFILE.name(),
            ActionType.GET_AUCTION_BID_HISTORY.name()
    );

    private static final Map<String, Set<UserRole>> ROLE_PERMISSIONS = Map.ofEntries(
            Map.entry(ActionType.CREATE_ITEM.name(), Set.of(UserRole.SELLER)),
            Map.entry(ActionType.UPDATE_ITEM.name(), Set.of(UserRole.SELLER)),
            Map.entry(ActionType.GET_SELLER_ITEMS.name(), Set.of(UserRole.SELLER)),
            Map.entry(ActionType.GET_ITEM_DETAIL.name(), Set.of(UserRole.SELLER, UserRole.ADMIN)),
            Map.entry(ActionType.CREATE_AUCTION.name(), Set.of(UserRole.SELLER)),
            Map.entry(ActionType.SELLER_CANCEL_AUCTION.name(), Set.of(UserRole.SELLER)),
            Map.entry(ActionType.SELLER_DELETE_ITEM.name(), Set.of(UserRole.SELLER)),
            Map.entry(ActionType.GET_SELLER_AUCTIONS.name(), Set.of(UserRole.SELLER)),
            Map.entry(ActionType.UPDATE_AUCTION.name(), Set.of(UserRole.SELLER)),
            Map.entry(ActionType.UPDATE_PROFILE.name(), Set.of(UserRole.ADMIN, UserRole.SELLER, UserRole.BIDDER)),
            Map.entry(ActionType.UPDATE_PASSWORD.name(), Set.of(UserRole.ADMIN, UserRole.SELLER, UserRole.BIDDER)),

            Map.entry(ActionType.DEPOSIT_MONEY.name(), Set.of(UserRole.BIDDER, UserRole.SELLER)),
            Map.entry(ActionType.WITHDRAW_MONEY.name(), Set.of(UserRole.BIDDER, UserRole.SELLER)),
            Map.entry(ActionType.PLACE_BID.name(), Set.of(UserRole.BIDDER)),
            Map.entry(ActionType.LIVE_ENTERED.name(), Set.of(UserRole.BIDDER, UserRole.SELLER, UserRole.ADMIN)),
            Map.entry(ActionType.LIVE_EXITED.name(), Set.of(UserRole.BIDDER, UserRole.SELLER, UserRole.ADMIN)),
            Map.entry(ActionType.AUCTION_SUBSCRIBED.name(), Set.of(UserRole.BIDDER)),
            Map.entry(ActionType.AUCTION_UNSUBSCRIBED.name(), Set.of(UserRole.BIDDER)),
            Map.entry(ActionType.GET_MY_BID_HISTORY.name(), Set.of(UserRole.BIDDER)),
            Map.entry(ActionType.SETUP_AUTO_BID.name(), Set.of(UserRole.BIDDER)),
            Map.entry(ActionType.CANCEL_AUTO_BID.name(), Set.of(UserRole.BIDDER)),

            Map.entry(ActionType.CMD_ADMIN_GET_USERS.name(), Set.of(UserRole.ADMIN)),
            Map.entry(ActionType.CMD_ADMIN_LOCK_USER.name(), Set.of(UserRole.ADMIN)),
            Map.entry(ActionType.CMD_ADMIN_CANCEL_AUCTION.name(), Set.of(UserRole.ADMIN)),
            Map.entry(ActionType.CMD_ADMIN_DELETE_ITEM.name(), Set.of(UserRole.ADMIN)),
            Map.entry(ActionType.CMD_ADMIN_GET_LOGS.name(), Set.of(UserRole.ADMIN)),
            Map.entry(ActionType.CMD_ADMIN_GET_ITEMS.name(), Set.of(UserRole.ADMIN)),
            Map.entry(ActionType.CMD_ADMIN_GET_AUCTIONS.name(), Set.of(UserRole.ADMIN))
    );

    public void canAccess(String action, ClientSession session) {
        if (action == null || action.trim().isEmpty()) {
            throw new AuthorizationException(AuthorizationErrorCode.ACTION_UNAUTHORIZED, "Action type cannot be null or empty");
        }

        if (PUBLIC_ACTIONS.contains(action)) {
            return;
        }

        if (session == null || !session.isLoggedIn()) {
            throw new AuthorizationException(AuthorizationErrorCode.NOT_AUTHENTICATED);
        }

        if (LOGIN_REQUIRED_ACTIONS.contains(action)) {
            return;
        }

        Set<UserRole> allowedRoles = ROLE_PERMISSIONS.get(action);

        if (allowedRoles == null) {
            log.warn("[Guard] 🚨 Cảnh báo bảo mật: Phát hiện request gọi Action chưa được cấu hình: {}", action);
            throw new AuthorizationException(AuthorizationErrorCode.ACTION_UNAUTHORIZED);
        }

        if (!allowedRoles.contains(session.getRole())) {
            log.warn("[Guard] ⛔ Từ chối truy cập: User [{}] mang Role [{}] cố tình gọi Action hạn chế [{}]",
                    session.getUserId(), session.getRole(), action);
            throw new AuthorizationException(AuthorizationErrorCode.ROLE_ACCESS_DENIED);
        }
    }
}