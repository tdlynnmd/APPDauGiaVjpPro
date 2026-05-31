package com.auction.service;

import com.auction.enums.ActionType;
import com.auction.enums.UserRole;
import com.auction.exception.AuthorizationErrorCode;
import com.auction.exception.AuthorizationException;
import com.auction.network.ClientSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationServiceTest {

    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService();
    }

    // Tạo session đã đăng nhập: phải có cả userId và role
    private ClientSession loggedInSession(String userId, UserRole role) {
        ClientSession session = new ClientSession(null, new PrintWriter(System.out));
        session.setUserId(userId);
        session.setRole(role);
        return session;
    }

    // Session chưa đăng nhập: userId và role đều null
    private ClientSession notLoggedInSession() {
        return new ClientSession(null, new PrintWriter(System.out));
    }

    // Check đúng mã lỗi AuthorizationException
    private void assertAuthorizationError(
            AuthorizationException exception,
            AuthorizationErrorCode expectedError
    ) {
        assertEquals(expectedError.getCode(), exception.getErrorCode());
    }

    // =========================================================
    // ACTION NULL / BLANK
    // =========================================================

    @Test
    void canAccessShouldThrowWhenActionIsNull() {
        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            authorizationService.canAccess(null, null);
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.ACTION_UNAUTHORIZED);
    }

    @Test
    void canAccessShouldThrowWhenActionIsEmpty() {
        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            authorizationService.canAccess("", null);
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.ACTION_UNAUTHORIZED);
    }

    @Test
    void canAccessShouldThrowWhenActionIsBlank() {
        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            authorizationService.canAccess("   ", null);
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.ACTION_UNAUTHORIZED);
    }

    // =========================================================
    // PUBLIC ACTIONS: LOGIN / REGISTER
    // =========================================================

    @Test
    void canAccessShouldAllowLoginWithoutSession() {
        assertDoesNotThrow(() -> {
            authorizationService.canAccess(ActionType.LOGIN.name(), null);
        });
    }

    @Test
    void canAccessShouldAllowRegisterWithoutSession() {
        assertDoesNotThrow(() -> {
            authorizationService.canAccess(ActionType.REGISTER.name(), null);
        });
    }

    @Test
    void canAccessShouldAllowLoginWithNotLoggedInSession() {
        ClientSession session = notLoggedInSession();

        assertDoesNotThrow(() -> {
            authorizationService.canAccess(ActionType.LOGIN.name(), session);
        });
    }

    // =========================================================
    // LOGIN REQUIRED ACTIONS
    // =========================================================

    @Test
    void canAccessShouldThrowNotAuthenticatedWhenLogoutWithNullSession() {
        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            authorizationService.canAccess(ActionType.LOGOUT.name(), null);
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.NOT_AUTHENTICATED);
    }

    @Test
    void canAccessShouldThrowNotAuthenticatedWhenLogoutWithNotLoggedInSession() {
        ClientSession session = notLoggedInSession();

        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            authorizationService.canAccess(ActionType.LOGOUT.name(), session);
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.NOT_AUTHENTICATED);
    }

    @Test
    void canAccessShouldAllowLogoutWhenBidderLoggedIn() {
        ClientSession session = loggedInSession("bidder-1", UserRole.BIDDER);

        assertDoesNotThrow(() -> {
            authorizationService.canAccess(ActionType.LOGOUT.name(), session);
        });
    }

    @Test
    void canAccessShouldAllowGetActiveAuctionsWhenUserLoggedIn() {
        ClientSession session = loggedInSession("seller-1", UserRole.SELLER);

        assertDoesNotThrow(() -> {
            authorizationService.canAccess(ActionType.GET_ACTIVE_AUCTIONS.name(), session);
        });
    }

    @Test
    void canAccessShouldAllowGetAuctionDetailWhenUserLoggedIn() {
        ClientSession session = loggedInSession("admin-1", UserRole.ADMIN);

        assertDoesNotThrow(() -> {
            authorizationService.canAccess(ActionType.GET_AUCTION_DETAIL.name(), session);
        });
    }

    @Test
    void canAccessShouldThrowNotAuthenticatedForAuctionDetailWhenNotLoggedIn() {
        ClientSession session = notLoggedInSession();

        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            authorizationService.canAccess(ActionType.GET_AUCTION_DETAIL.name(), session);
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.NOT_AUTHENTICATED);
    }

    // =========================================================
    // SELLER / ADMIN ITEM PERMISSIONS
    // =========================================================

    @Test
    void canAccessShouldAllowSellerToCreateItem() {
        ClientSession session = loggedInSession("seller-1", UserRole.SELLER);

        assertDoesNotThrow(() -> {
            authorizationService.canAccess(ActionType.CREATE_ITEM.name(), session);
        });
    }

    @Test
    void canAccessShouldAllowAdminToCreateItem() {
        ClientSession session = loggedInSession("admin-1", UserRole.ADMIN);

        assertDoesNotThrow(() -> {
            authorizationService.canAccess(ActionType.CREATE_ITEM.name(), session);
        });
    }

    @Test
    void canAccessShouldDenyBidderToCreateItem() {
        ClientSession session = loggedInSession("bidder-1", UserRole.BIDDER);

        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            authorizationService.canAccess(ActionType.CREATE_ITEM.name(), session);
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.ROLE_ACCESS_DENIED);
    }

    @Test
    void canAccessShouldAllowSellerToUpdateItem() {
        ClientSession session = loggedInSession("seller-1", UserRole.SELLER);

        assertDoesNotThrow(() -> {
            authorizationService.canAccess(ActionType.UPDATE_ITEM.name(), session);
        });
    }

    @Test
    void canAccessShouldDenyBidderToUpdateItem() {
        ClientSession session = loggedInSession("bidder-1", UserRole.BIDDER);

        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            authorizationService.canAccess(ActionType.UPDATE_ITEM.name(), session);
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.ROLE_ACCESS_DENIED);
    }

    @Test
    void canAccessShouldAllowSellerToGetSellerItems() {
        ClientSession session = loggedInSession("seller-1", UserRole.SELLER);

        assertDoesNotThrow(() -> {
            authorizationService.canAccess(ActionType.GET_SELLER_ITEMS.name(), session);
        });
    }

    // =========================================================
    // CREATE AUCTION / CANCEL AUCTION
    // =========================================================

    @Test
    void canAccessShouldAllowSellerToCreateAuction() {
        ClientSession session = loggedInSession("seller-1", UserRole.SELLER);

        assertDoesNotThrow(() -> {
            authorizationService.canAccess(ActionType.CREATE_AUCTION.name(), session);
        });
    }

    @Test
    void canAccessShouldAllowAdminToCreateAuction() {
        ClientSession session = loggedInSession("admin-1", UserRole.ADMIN);

        assertDoesNotThrow(() -> {
            authorizationService.canAccess(ActionType.CREATE_AUCTION.name(), session);
        });
    }

    @Test
    void canAccessShouldDenyBidderToCreateAuction() {
        ClientSession session = loggedInSession("bidder-1", UserRole.BIDDER);

        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            authorizationService.canAccess(ActionType.CREATE_AUCTION.name(), session);
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.ROLE_ACCESS_DENIED);
    }

    @Test
    void canAccessShouldAllowAdminToCancelAuction() {
        ClientSession session = loggedInSession("admin-1", UserRole.ADMIN);

        assertDoesNotThrow(() -> {
            authorizationService.canAccess(ActionType.CANCEL_AUCTION.name(), session);
        });
    }

    @Test
    void canAccessShouldDenySellerToCancelAuction() {
        ClientSession session = loggedInSession("seller-1", UserRole.SELLER);

        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            authorizationService.canAccess(ActionType.CANCEL_AUCTION.name(), session);
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.ROLE_ACCESS_DENIED);
    }

    // =========================================================
    // BIDDER PERMISSIONS
    // =========================================================

    @Test
    void canAccessShouldAllowBidderToPlaceBid() {
        ClientSession session = loggedInSession("bidder-1", UserRole.BIDDER);

        assertDoesNotThrow(() -> {
            authorizationService.canAccess(ActionType.PLACE_BID.name(), session);
        });
    }

    @Test
    void canAccessShouldDenySellerToPlaceBid() {
        ClientSession session = loggedInSession("seller-1", UserRole.SELLER);

        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            authorizationService.canAccess(ActionType.PLACE_BID.name(), session);
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.ROLE_ACCESS_DENIED);
    }

    @Test
    void canAccessShouldAllowBidderToLiveEntered() {
        ClientSession session = loggedInSession("bidder-1", UserRole.BIDDER);

        assertDoesNotThrow(() -> {
            authorizationService.canAccess(ActionType.LIVE_ENTERED.name(), session);
        });
    }

    @Test
    void canAccessShouldAllowBidderToLiveExited() {
        ClientSession session = loggedInSession("bidder-1", UserRole.BIDDER);

        assertDoesNotThrow(() -> {
            authorizationService.canAccess(ActionType.LIVE_EXITED.name(), session);
        });
    }

    @Test
    void canAccessShouldAllowBidderToSubscribeAuction() {
        ClientSession session = loggedInSession("bidder-1", UserRole.BIDDER);

        assertDoesNotThrow(() -> {
            authorizationService.canAccess(ActionType.AUCTION_SUBSCRIBED.name(), session);
        });
    }

    @Test
    void canAccessShouldDenySellerToSubscribeAuction() {
        ClientSession session = loggedInSession("seller-1", UserRole.SELLER);

        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            authorizationService.canAccess(ActionType.AUCTION_SUBSCRIBED.name(), session);
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.ROLE_ACCESS_DENIED);
    }

    // =========================================================
    // UNCONFIGURED ACTION
    // =========================================================

    @Test
    void canAccessShouldThrowWhenActionIsNotConfigured() {
        ClientSession session = loggedInSession("bidder-1", UserRole.BIDDER);

        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            authorizationService.canAccess(ActionType.BID_UPDATE.name(), session);
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.ACTION_UNAUTHORIZED);
    }

    @Test
    void canAccessShouldThrowWhenActionIsUnknownString() {
        ClientSession session = loggedInSession("admin-1", UserRole.ADMIN);

        AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
            authorizationService.canAccess("UNKNOWN_ACTION", session);
        });

        assertAuthorizationError(exception, AuthorizationErrorCode.ACTION_UNAUTHORIZED);
    }
}