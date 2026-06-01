package com.auction.service;

import com.auction.enums.ActionType;
import com.auction.enums.UserRole;
import com.auction.exception.AuthorizationErrorCode;
import com.auction.exception.AuthorizationException;
import com.auction.network.ClientSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationServiceTest {

    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService();
    }

    // Tạo session chưa login
    private ClientSession anonymousSession() {
        return new ClientSession((Socket) null, new PrintWriter(System.out));
    }

    // Tạo session đã login với role cụ thể
    private ClientSession loggedInSession(String userId, UserRole role) {
        ClientSession session = new ClientSession((Socket) null, new PrintWriter(System.out));
        session.setUserId(userId);
        session.setRole(role);
        return session;
    }

    // Check đúng mã lỗi AuthorizationException
    private void assertAuthorizationError(AuthorizationException exception, AuthorizationErrorCode expectedError) {
        assertEquals(expectedError.getCode(), exception.getErrorCode());
    }

    // =========================================================
    // Invalid action
    // =========================================================

    // action null phải bị chặn ACTION_UNAUTHORIZED
    @Test
    void canAccessShouldThrowWhenActionIsNull() {
        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            authorizationService.canAccess(null, anonymousSession());
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.ACTION_UNAUTHORIZED);
    }

    // action rỗng phải bị chặn ACTION_UNAUTHORIZED
    @Test
    void canAccessShouldThrowWhenActionIsBlank() {
        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            authorizationService.canAccess("   ", anonymousSession());
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.ACTION_UNAUTHORIZED);
    }

    // action lạ chưa cấu hình phải bị chặn ACTION_UNAUTHORIZED
    @Test
    void canAccessShouldThrowWhenActionIsUnknown() {
        ClientSession bidder = loggedInSession("bidder-1", UserRole.BIDDER);

        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            authorizationService.canAccess("UNKNOWN_ACTION", bidder);
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.ACTION_UNAUTHORIZED);
    }

    // action phân biệt hoa thường, login viết thường không được coi là LOGIN
    @Test
    void canAccessShouldBeCaseSensitive() {
        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            authorizationService.canAccess("login", anonymousSession());
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.NOT_AUTHENTICATED);
    }

    // =========================================================
    // Public actions
    // =========================================================

    // LOGIN không cần session
    @Test
    void canAccessShouldAllowLoginWithoutSession() {
        assertDoesNotThrow(() -> {
            authorizationService.canAccess(ActionType.LOGIN.name(), null);
        });
    }

    // REGISTER không cần session
    @Test
    void canAccessShouldAllowRegisterWithoutSession() {
        assertDoesNotThrow(() -> {
            authorizationService.canAccess(ActionType.REGISTER.name(), null);
        });
    }

    // PING không cần session
    @Test
    void canAccessShouldAllowPingWithoutSession() {
        assertDoesNotThrow(() -> {
            authorizationService.canAccess(ActionType.PING.name(), null);
        });
    }

    // Public actions vẫn được cho qua nếu session chưa login
    @Test
    void canAccessShouldAllowPublicActionsForAnonymousSession() {
        ClientSession session = anonymousSession();

        assertDoesNotThrow(() -> authorizationService.canAccess(ActionType.LOGIN.name(), session));
        assertDoesNotThrow(() -> authorizationService.canAccess(ActionType.REGISTER.name(), session));
        assertDoesNotThrow(() -> authorizationService.canAccess(ActionType.PING.name(), session));
    }

    // =========================================================
    // Login required actions
    // =========================================================

    // LOGOUT chưa login phải bị chặn NOT_AUTHENTICATED
    @Test
    void canAccessShouldDenyLogoutWhenNotLoggedIn() {
        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            authorizationService.canAccess(ActionType.LOGOUT.name(), anonymousSession());
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.NOT_AUTHENTICATED);
    }

    // Các action chỉ cần login thì BIDDER/SELLER/ADMIN đều được phép
    @Test
    void canAccessShouldAllowLoginRequiredActionsForAllLoggedInRoles() {
        List<ActionType> loginRequiredActions = List.of(
                ActionType.LOGOUT,
                ActionType.GET_ACTIVE_AUCTIONS,
                ActionType.GET_AUCTION_DETAIL,
                ActionType.GET_USER_PROFILE,
                ActionType.GET_AUCTION_BID_HISTORY
        );

        List<ClientSession> sessions = List.of(
                loggedInSession("bidder-1", UserRole.BIDDER),
                loggedInSession("seller-1", UserRole.SELLER),
                loggedInSession("admin-1", UserRole.ADMIN)
        );

        for (ActionType action : loginRequiredActions) {
            for (ClientSession session : sessions) {
                assertDoesNotThrow(() -> {
                    authorizationService.canAccess(action.name(), session);
                }, "Action should be allowed: " + action + " for role " + session.getRole());
            }
        }
    }

    // Login required action với session null phải bị chặn
    @Test
    void canAccessShouldDenyLoginRequiredActionWhenSessionIsNull() {
        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            authorizationService.canAccess(ActionType.GET_ACTIVE_AUCTIONS.name(), null);
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.NOT_AUTHENTICATED);
    }

    // Có userId nhưng chưa có role thì vẫn chưa được coi là login
    @Test
    void canAccessShouldDenyWhenSessionHasUserIdButNoRole() {
        ClientSession session = anonymousSession();
        session.setUserId("user-1");

        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            authorizationService.canAccess(ActionType.GET_USER_PROFILE.name(), session);
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.NOT_AUTHENTICATED);
    }

    // Có role nhưng chưa có userId thì vẫn chưa được coi là login
    @Test
    void canAccessShouldDenyWhenSessionHasRoleButNoUserId() {
        ClientSession session = anonymousSession();
        session.setRole(UserRole.BIDDER);

        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            authorizationService.canAccess(ActionType.GET_USER_PROFILE.name(), session);
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.NOT_AUTHENTICATED);
    }

    // =========================================================
    // Seller actions
    // =========================================================

    // SELLER được phép gọi các action của seller
    @Test
    void canAccessShouldAllowSellerActionsForSeller() {
        ClientSession seller = loggedInSession("seller-1", UserRole.SELLER);

        List<ActionType> sellerActions = List.of(
                ActionType.CREATE_ITEM,
                ActionType.UPDATE_ITEM,
                ActionType.GET_SELLER_ITEMS,
                ActionType.GET_ITEM_DETAIL,
                ActionType.CREATE_AUCTION,
                ActionType.SELLER_CANCEL_AUCTION,
                ActionType.SELLER_DELETE_ITEM
        );

        for (ActionType action : sellerActions) {
            assertDoesNotThrow(() -> {
                authorizationService.canAccess(action.name(), seller);
            }, "Seller should be allowed: " + action);
        }
    }

    // BIDDER không được gọi action seller
    @Test
    void canAccessShouldDenySellerActionsForBidder() {
        ClientSession bidder = loggedInSession("bidder-1", UserRole.BIDDER);

        List<ActionType> sellerOnlyActions = List.of(
                ActionType.CREATE_ITEM,
                ActionType.UPDATE_ITEM,
                ActionType.GET_SELLER_ITEMS,
                ActionType.CREATE_AUCTION,
                ActionType.SELLER_CANCEL_AUCTION,
                ActionType.SELLER_DELETE_ITEM
        );

        for (ActionType action : sellerOnlyActions) {
            AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
                authorizationService.canAccess(action.name(), bidder);
            }, "Bidder should be denied: " + action);

            assertAuthorizationError(exception, AuthorizationErrorCode.ROLE_ACCESS_DENIED);
        }
    }

    // ADMIN không được gọi các action seller-only
    @Test
    void canAccessShouldDenySellerOnlyActionsForAdmin() {
        ClientSession admin = loggedInSession("admin-1", UserRole.ADMIN);

        List<ActionType> sellerOnlyActions = List.of(
                ActionType.CREATE_ITEM,
                ActionType.UPDATE_ITEM,
                ActionType.GET_SELLER_ITEMS,
                ActionType.CREATE_AUCTION,
                ActionType.SELLER_CANCEL_AUCTION,
                ActionType.SELLER_DELETE_ITEM
        );

        for (ActionType action : sellerOnlyActions) {
            AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
                authorizationService.canAccess(action.name(), admin);
            }, "Admin should be denied seller-only action: " + action);

            assertAuthorizationError(exception, AuthorizationErrorCode.ROLE_ACCESS_DENIED);
        }
    }

    // GET_ITEM_DETAIL cho phép cả SELLER và ADMIN
    @Test
    void canAccessShouldAllowGetItemDetailForSellerAndAdmin() {
        ClientSession seller = loggedInSession("seller-1", UserRole.SELLER);
        ClientSession admin = loggedInSession("admin-1", UserRole.ADMIN);

        assertDoesNotThrow(() -> authorizationService.canAccess(ActionType.GET_ITEM_DETAIL.name(), seller));
        assertDoesNotThrow(() -> authorizationService.canAccess(ActionType.GET_ITEM_DETAIL.name(), admin));
    }

    // GET_ITEM_DETAIL không cho BIDDER
    @Test
    void canAccessShouldDenyGetItemDetailForBidder() {
        ClientSession bidder = loggedInSession("bidder-1", UserRole.BIDDER);

        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            authorizationService.canAccess(ActionType.GET_ITEM_DETAIL.name(), bidder);
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.ROLE_ACCESS_DENIED);
    }

    // =========================================================
    // Bidder actions
    // =========================================================

    // BIDDER được phép gọi các action bidder
    @Test
    void canAccessShouldAllowBidderActionsForBidder() {
        ClientSession bidder = loggedInSession("bidder-1", UserRole.BIDDER);

        List<ActionType> bidderActions = List.of(
                ActionType.DEPOSIT_MONEY,
                ActionType.WITHDRAW_MONEY,
                ActionType.PLACE_BID,
                ActionType.LIVE_ENTERED,
                ActionType.LIVE_EXITED,
                ActionType.AUCTION_SUBSCRIBED,
                ActionType.AUCTION_UNSUBSCRIBED,
                ActionType.GET_MY_BID_HISTORY,
                ActionType.SETUP_AUTO_BID,
                ActionType.CANCEL_AUTO_BID
        );

        for (ActionType action : bidderActions) {
            assertDoesNotThrow(() -> {
                authorizationService.canAccess(action.name(), bidder);
            }, "Bidder should be allowed: " + action);
        }
    }

    // SELLER không được gọi action bidder
    @Test
    void canAccessShouldDenyBidderActionsForSeller() {
        ClientSession seller = loggedInSession("seller-1", UserRole.SELLER);

        List<ActionType> bidderActions = List.of(
                ActionType.DEPOSIT_MONEY,
                ActionType.WITHDRAW_MONEY,
                ActionType.PLACE_BID,
                ActionType.LIVE_ENTERED,
                ActionType.LIVE_EXITED,
                ActionType.AUCTION_SUBSCRIBED,
                ActionType.AUCTION_UNSUBSCRIBED,
                ActionType.GET_MY_BID_HISTORY,
                ActionType.SETUP_AUTO_BID,
                ActionType.CANCEL_AUTO_BID
        );

        for (ActionType action : bidderActions) {
            AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
                authorizationService.canAccess(action.name(), seller);
            }, "Seller should be denied: " + action);

            assertAuthorizationError(exception, AuthorizationErrorCode.ROLE_ACCESS_DENIED);
        }
    }

    // ADMIN không được gọi action bidder
    @Test
    void canAccessShouldDenyBidderActionsForAdmin() {
        ClientSession admin = loggedInSession("admin-1", UserRole.ADMIN);

        List<ActionType> bidderActions = List.of(
                ActionType.DEPOSIT_MONEY,
                ActionType.WITHDRAW_MONEY,
                ActionType.PLACE_BID,
                ActionType.LIVE_ENTERED,
                ActionType.LIVE_EXITED,
                ActionType.AUCTION_SUBSCRIBED,
                ActionType.AUCTION_UNSUBSCRIBED,
                ActionType.GET_MY_BID_HISTORY,
                ActionType.SETUP_AUTO_BID,
                ActionType.CANCEL_AUTO_BID
        );

        for (ActionType action : bidderActions) {
            AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
                authorizationService.canAccess(action.name(), admin);
            }, "Admin should be denied bidder action: " + action);

            assertAuthorizationError(exception, AuthorizationErrorCode.ROLE_ACCESS_DENIED);
        }
    }

    // =========================================================
    // Admin actions
    // =========================================================

    // ADMIN được phép gọi các action admin
    @Test
    void canAccessShouldAllowAdminActionsForAdmin() {
        ClientSession admin = loggedInSession("admin-1", UserRole.ADMIN);

        List<ActionType> adminActions = List.of(
                ActionType.CMD_ADMIN_GET_USERS,
                ActionType.CMD_ADMIN_LOCK_USER,
                ActionType.CMD_ADMIN_CANCEL_AUCTION,
                ActionType.CMD_ADMIN_DELETE_ITEM,
                ActionType.CMD_ADMIN_GET_LOGS
        );

        for (ActionType action : adminActions) {
            assertDoesNotThrow(() -> {
                authorizationService.canAccess(action.name(), admin);
            }, "Admin should be allowed: " + action);
        }
    }

    // BIDDER không được gọi action admin
    @Test
    void canAccessShouldDenyAdminActionsForBidder() {
        ClientSession bidder = loggedInSession("bidder-1", UserRole.BIDDER);

        List<ActionType> adminActions = List.of(
                ActionType.CMD_ADMIN_GET_USERS,
                ActionType.CMD_ADMIN_LOCK_USER,
                ActionType.CMD_ADMIN_CANCEL_AUCTION,
                ActionType.CMD_ADMIN_DELETE_ITEM,
                ActionType.CMD_ADMIN_GET_LOGS
        );

        for (ActionType action : adminActions) {
            AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
                authorizationService.canAccess(action.name(), bidder);
            }, "Bidder should be denied admin action: " + action);

            assertAuthorizationError(exception, AuthorizationErrorCode.ROLE_ACCESS_DENIED);
        }
    }

    // SELLER không được gọi action admin
    @Test
    void canAccessShouldDenyAdminActionsForSeller() {
        ClientSession seller = loggedInSession("seller-1", UserRole.SELLER);

        List<ActionType> adminActions = List.of(
                ActionType.CMD_ADMIN_GET_USERS,
                ActionType.CMD_ADMIN_LOCK_USER,
                ActionType.CMD_ADMIN_CANCEL_AUCTION,
                ActionType.CMD_ADMIN_DELETE_ITEM,
                ActionType.CMD_ADMIN_GET_LOGS
        );

        for (ActionType action : adminActions) {
            AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
                authorizationService.canAccess(action.name(), seller);
            }, "Seller should be denied admin action: " + action);

            assertAuthorizationError(exception, AuthorizationErrorCode.ROLE_ACCESS_DENIED);
        }
    }

    // =========================================================
    // Response/event actions not configured
    // =========================================================

    // BID_UPDATE là action server gửi về client, client không được chủ động gọi lên
    @Test
    void canAccessShouldDenyBidUpdateAsClientRequest() {
        ClientSession bidder = loggedInSession("bidder-1", UserRole.BIDDER);

        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            authorizationService.canAccess(ActionType.BID_UPDATE.name(), bidder);
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.ACTION_UNAUTHORIZED);
    }

    // TIME_UPDATE là action server gửi về client, client không được chủ động gọi lên
    @Test
    void canAccessShouldDenyTimeUpdateAsClientRequest() {
        ClientSession bidder = loggedInSession("bidder-1", UserRole.BIDDER);

        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            authorizationService.canAccess(ActionType.TIME_UPDATE.name(), bidder);
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.ACTION_UNAUTHORIZED);
    }

    // STATUS_UPDATED là action server gửi về client, client không được chủ động gọi lên
    @Test
    void canAccessShouldDenyStatusUpdatedAsClientRequest() {
        ClientSession bidder = loggedInSession("bidder-1", UserRole.BIDDER);

        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            authorizationService.canAccess(ActionType.STATUS_UPDATED.name(), bidder);
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.ACTION_UNAUTHORIZED);
    }
}