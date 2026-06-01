package com.auction.enums;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ActionTypeTest {

    // Lấy toàn bộ tên action hiện có
    private Set<String> actionNames() {
        return Arrays.stream(ActionType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());
    }

    // Auth actions phải tồn tại
    @Test
    void actionTypeShouldContainAuthActions() {
        Set<String> names = actionNames();

        assertTrue(names.contains("LOGIN"));
        assertTrue(names.contains("REGISTER"));
        assertTrue(names.contains("LOGOUT"));
        assertTrue(names.contains("PING"));
    }

    // Item/Seller actions phải tồn tại
    @Test
    void actionTypeShouldContainItemAndSellerActions() {
        Set<String> names = actionNames();

        assertTrue(names.contains("CREATE_ITEM"));
        assertTrue(names.contains("UPDATE_ITEM"));
        assertTrue(names.contains("SELLER_DELETE_ITEM"));
        assertTrue(names.contains("GET_SELLER_ITEMS"));
        assertTrue(names.contains("GET_ITEM_DETAIL"));
    }

    // Auction actions phải tồn tại
    @Test
    void actionTypeShouldContainAuctionActions() {
        Set<String> names = actionNames();

        assertTrue(names.contains("GET_ACTIVE_AUCTIONS"));
        assertTrue(names.contains("GET_AUCTION_DETAIL"));
        assertTrue(names.contains("CREATE_AUCTION"));
        assertTrue(names.contains("PLACE_BID"));
        assertTrue(names.contains("SELLER_CANCEL_AUCTION"));
        assertTrue(names.contains("GET_AUCTION_BID_HISTORY"));
    }

    // Live/realtime actions phải tồn tại
    @Test
    void actionTypeShouldContainRealtimeActions() {
        Set<String> names = actionNames();

        assertTrue(names.contains("BID_UPDATE"));
        assertTrue(names.contains("TIME_UPDATE"));
        assertTrue(names.contains("STATUS_UPDATED"));
        assertTrue(names.contains("FORCE_LOGOUT"));
        assertTrue(names.contains("AUCTION_SUBSCRIBED"));
        assertTrue(names.contains("AUCTION_UNSUBSCRIBED"));
        assertTrue(names.contains("LIVE_ENTERED"));
        assertTrue(names.contains("LIVE_EXITED"));
    }

    // User wallet/profile actions phải tồn tại
    @Test
    void actionTypeShouldContainUserActions() {
        Set<String> names = actionNames();

        assertTrue(names.contains("GET_USER_PROFILE"));
        assertTrue(names.contains("DEPOSIT_MONEY"));
        assertTrue(names.contains("WITHDRAW_MONEY"));
        assertTrue(names.contains("GET_MY_BID_HISTORY"));
    }

    // Admin actions phải tồn tại
    @Test
    void actionTypeShouldContainAdminActions() {
        Set<String> names = actionNames();

        assertTrue(names.contains("CMD_ADMIN_GET_USERS"));
        assertTrue(names.contains("CMD_ADMIN_LOCK_USER"));
        assertTrue(names.contains("CMD_ADMIN_GET_LOGS"));
        assertTrue(names.contains("CMD_ADMIN_CANCEL_AUCTION"));
        assertTrue(names.contains("CMD_ADMIN_DELETE_ITEM"));
    }

    // Auto-bid actions mới phải tồn tại
    @Test
    void actionTypeShouldContainAutoBidActions() {
        Set<String> names = actionNames();

        assertTrue(names.contains("SETUP_AUTO_BID"));
        assertTrue(names.contains("CANCEL_AUTO_BID"));
    }

    // valueOf phải parse đúng action hợp lệ
    @Test
    void valueOfShouldParseValidAction() {
        assertEquals(ActionType.LOGIN, ActionType.valueOf("LOGIN"));
        assertEquals(ActionType.PLACE_BID, ActionType.valueOf("PLACE_BID"));
        assertEquals(ActionType.SETUP_AUTO_BID, ActionType.valueOf("SETUP_AUTO_BID"));
        assertEquals(ActionType.CMD_ADMIN_DELETE_ITEM, ActionType.valueOf("CMD_ADMIN_DELETE_ITEM"));
    }

    // valueOf action sai phải ném IllegalArgumentException
    @Test
    void valueOfShouldThrowWhenActionIsUnknown() {
        assertThrows(IllegalArgumentException.class, () -> {
            ActionType.valueOf("UNKNOWN_ACTION");
        });
    }

    // valueOf phân biệt hoa thường
    @Test
    void valueOfShouldBeCaseSensitive() {
        assertThrows(IllegalArgumentException.class, () -> {
            ActionType.valueOf("login");
        });
    }

    // Enum không được có tên trùng
    @Test
    void actionTypeShouldNotContainDuplicateNames() {
        ActionType[] values = ActionType.values();

        Set<String> names = actionNames();

        assertEquals(values.length, names.size());
    }

    // Số lượng action hiện tại của enum ActionType
    @Test
    void actionTypeShouldHaveExpectedMinimumCount() {
        assertTrue(ActionType.values().length >= 34);
    }
}