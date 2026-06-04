package com.auction.network;

import com.auction.dto.ActionLogDTO;
import com.auction.dto.AuctionSummaryDTO;
import com.auction.dto.CancelAuctionRequest;
import com.auction.dto.DeleteItemRequest;
import com.auction.dto.GetAuditLogsRequest;
import com.auction.dto.GetUserDashboardRequest;
import com.auction.dto.ItemSummaryDTO;
import com.auction.dto.LockUserAccountRequest;
import com.auction.dto.PageDTO;
import com.auction.dto.SocketRequest;
import com.auction.dto.SocketResponse;
import com.auction.dto.UserDTO;
import com.auction.enums.ActionType;
import com.auction.enums.UserStatus;
import com.auction.service.ClientSocketService;
import com.auction.utils.GsonProvider;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * API Client xử lý các lệnh điều hành, kiểm duyệt dành cho Quản trị viên (Admin).
 */
public class ClientAdminApi {
    private final Gson gson = GsonProvider.getGson();

    /**
     * CMD_ADMIN_GET_USERS
     *
     * Lay danh sach user cho man hinh Admin Dashboard.
     * Server tra ve SocketResponse.body = PageDTO<UserDTO>.
     */
    public SocketResponse getUsers(int page, int pageSize) {
        GetUserDashboardRequest request = new GetUserDashboardRequest(page, pageSize);
        return sendRequest(ActionType.CMD_ADMIN_GET_USERS, request);
    }

    /**
     * CMD_ADMIN_LOCK_USER
     *
     * Khoa tai khoan user vi pham.
     * Client chi gui userId + reason.
     * targetStatus hien tai duoc chot la BANNED.
     */
    public SocketResponse lockUser(String userId, String reason) {
        LockUserAccountRequest request = new LockUserAccountRequest(
                userId,
                UserStatus.BANNED,
                reason
        );

        return sendRequest(ActionType.CMD_ADMIN_LOCK_USER, request);
    }

    /**
     * CMD_ADMIN_GET_LOGS
     *
     * Lay audit log cho Admin.
     * Server tra ve SocketResponse.body = PageDTO<ActionLogDTO>.
     */
    public SocketResponse getLogs(int page, int pageSize) {
        GetAuditLogsRequest request = new GetAuditLogsRequest(page, pageSize);
        return sendRequest(ActionType.CMD_ADMIN_GET_LOGS, request);
    }

    /**
     * CMD_ADMIN_GET_ITEMS
     *
     * Lay danh sach toan bo items cho Admin Dashboard.
     * Server tra ve SocketResponse.body = PageDTO<ItemSummaryDTO>.
     * Tai su dung GetUserDashboardRequest vi chi co page + pageSize.
     */
    public SocketResponse getItems(int page, int pageSize) {
        GetUserDashboardRequest request = new GetUserDashboardRequest(page, pageSize);
        return sendRequest(ActionType.CMD_ADMIN_GET_ITEMS, request);
    }

    /**
     * CMD_ADMIN_GET_AUCTIONS
     *
     * Lay danh sach toan bo auctions cho Admin Dashboard.
     * Server tra ve SocketResponse.body = PageDTO<AuctionSummaryDTO>.
     * Tai su dung GetUserDashboardRequest vi chi co page + pageSize.
     */
    public SocketResponse getAuctions(int page, int pageSize) {
        GetUserDashboardRequest request = new GetUserDashboardRequest(page, pageSize);
        return sendRequest(ActionType.CMD_ADMIN_GET_AUCTIONS, request);
    }

    /**
     * CMD_ADMIN_CANCEL_AUCTION
     *
     * Admin cuong che huy mot phien dau gia vi pham.
     * userId truyen null vi Server lay adminId tu ClientSession, khong tin vao body client gui len.
     */
    public SocketResponse cancelAuction(String auctionId, String reason) {
        CancelAuctionRequest request = new CancelAuctionRequest(
                auctionId,
                reason,
                null
        );

        return sendRequest(ActionType.CMD_ADMIN_CANCEL_AUCTION, request);
    }

    /**
     * CMD_ADMIN_DELETE_ITEM
     *
     * Admin cuong che go/ban item vi pham.
     * userId truyen null vi Server lay adminId tu ClientSession.
     */
    public SocketResponse deleteItem(String itemId, String reason) {
        DeleteItemRequest request = new DeleteItemRequest(
                itemId,
                reason,
                null
        );

        return sendRequest(ActionType.CMD_ADMIN_DELETE_ITEM, request);
    }

    /**
     * Parse response.body cua CMD_ADMIN_GET_USERS.
     *
     * Khong dung PageDTO.class truc tiep vi Java bi type erasure.
     * TypeToken giup Gson biet data trong PageDTO la UserDTO.
     */
    public PageDTO<UserDTO> parseUserPage(SocketResponse response) {
        if (hasNoUsableBody(response)) {
            return new PageDTO<>(List.of(), 1, 0, 0);
        }

        Type pageType = new TypeToken<PageDTO<UserDTO>>() {}.getType();
        return gson.fromJson(response.getBody(), pageType);
    }

    /**
     * Parse response.body cua CMD_ADMIN_GET_LOGS.
     *
     * ActionLogDTO co timestamp LocalDateTime, nen dung GsonProvider de parse dung format ISO.
     */
    public PageDTO<ActionLogDTO> parseLogPage(SocketResponse response) {
        if (hasNoUsableBody(response)) {
            return new PageDTO<>(List.of(), 1, 0, 0);
        }

        Type pageType = new TypeToken<PageDTO<ActionLogDTO>>() {}.getType();
        return gson.fromJson(response.getBody(), pageType);
    }

    /**
     * Parse response.body cua CMD_ADMIN_GET_ITEMS.
     *
     * Bam sat y het parseUserPage nhung dung TypeToken<PageDTO<ItemSummaryDTO>>.
     */
    public PageDTO<ItemSummaryDTO> parseItemPage(SocketResponse response) {
        if (hasNoUsableBody(response)) {
            return new PageDTO<>(List.of(), 1, 0, 0);
        }

        Type pageType = new TypeToken<PageDTO<ItemSummaryDTO>>() {}.getType();
        return gson.fromJson(response.getBody(), pageType);
    }

    /**
     * Parse response.body cua CMD_ADMIN_GET_AUCTIONS.
     *
     * Bam sat y het parseLogPage nhung dung TypeToken<PageDTO<AuctionSummaryDTO>>.
     * AuctionSummaryDTO co LocalDateTime, nen dung GsonProvider de parse dung.
     */
    public PageDTO<AuctionSummaryDTO> parseAuctionPage(SocketResponse response) {
        if (hasNoUsableBody(response)) {
            return new PageDTO<>(List.of(), 1, 0, 0);
        }

        Type pageType = new TypeToken<PageDTO<AuctionSummaryDTO>>() {}.getType();
        return gson.fromJson(response.getBody(), pageType);
    }

    /**
     * Gui request chung cho moi action Admin.
     *
     * Luong chuan:
     * - DTO -> JsonObject.
     * - JsonObject -> SocketRequest.
     * - SocketRequest -> ClientSocketService.
     * - ClientSocketService tra ve dung SocketResponse co requestId tuong ung.
     */
    private SocketResponse sendRequest(ActionType actionType, Object requestBody) {
        SocketRequest socketRequest = null;

        try {
            JsonObject body = toJsonObject(requestBody);
            socketRequest = new SocketRequest(actionType, body);

            return ClientSocketService.getInstance().sendRequest(socketRequest);

        } catch (Exception e) {
            e.printStackTrace();

            String requestId = socketRequest == null ? null : socketRequest.getRequestId();

            return SocketResponse.failure(
                    requestId,
                    actionType,
                    "Cannot connect to the server. Please check whether the server is running.",
                    "CONNECTION_ERROR"
            );
        }
    }

    /**
     * Chuan hoa body truoc khi dua vao SocketRequest.
     */
    private JsonObject toJsonObject(Object requestBody) {
        if (requestBody == null) {
            return new JsonObject();
        }

        if (requestBody instanceof JsonObject) {
            return (JsonObject) requestBody;
        }

        return gson.toJsonTree(requestBody).getAsJsonObject();
    }

    /**
     * Kiem tra response co parse duoc body hay khong.
     */
    private boolean hasNoUsableBody(SocketResponse response) {
        return response == null
                || !response.isSuccess()
                || response.getBody() == null
                || response.getBody().isJsonNull();
    }
}