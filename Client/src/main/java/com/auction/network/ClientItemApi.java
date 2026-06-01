package com.auction.network;

import com.auction.dto.CreateItemRequest;
import com.auction.dto.DeleteItemRequest;
import com.auction.dto.GetItemDetailRequest;
import com.auction.dto.ItemDetailDTO;
import com.auction.dto.ItemSummaryDTO;
import com.auction.dto.SocketRequest;
import com.auction.dto.SocketResponse;
import com.auction.dto.UpdateItemRequest;
import com.auction.enums.ActionType;
import com.auction.service.ClientSocketService;
import com.auction.utils.GsonProvider;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ClientItemApi la lop API phia Client cho nhom chuc nang quan ly item cua Seller/Admin.
 *
 * Vai tro:
 * - Tao dung request DTO da chot trong common.
 * - Dong goi request vao SocketRequest voi ActionType tuong ung.
 * - Gui request qua ClientSocketService, khong doc socket truc tiep.
 * - Parse SocketResponse.body ve ItemDetailDTO hoac List<ItemSummaryDTO> cho JavaFX Controller dung.
 *
 * Luu y:
 * - Class nay khong xu ly UI.
 * - Class nay khong kiem tra phan quyen.
 * - Class nay khong xu ly nghiep vu item.
 * - Server ItemController/ItemService moi la noi validate va xu ly nghiep vu that.
 */
public class ClientItemApi {
    private final Gson gson = GsonProvider.getGson();

    /**
     * CREATE_ITEM
     *
     * Client gui CreateItemRequest.
     * Server tra SocketResponse.body = ItemDetailDTO cua item vua tao.
     */
    public SocketResponse createItem(CreateItemRequest request) {
        return sendRequest(ActionType.CREATE_ITEM, request);
    }

    /**
     * UPDATE_ITEM
     *
     * Client gui UpdateItemRequest.
     * Server tra SocketResponse.body = ItemDetailDTO sau khi cap nhat.
     */
    public SocketResponse updateItem(UpdateItemRequest request) {
        return sendRequest(ActionType.UPDATE_ITEM, request);
    }

    /**
     * DELETE_ITEM
     *
     * Client gui DeleteItemRequest.
     * Server an item bang status INACTIVE va tra SocketResponse.body = null.
     */
    public SocketResponse deleteItem(String itemId, String reason) {
        String userId = com.auction.service.SessionManager.getInstance().getCurrentUserId();
        DeleteItemRequest request = new DeleteItemRequest(itemId, reason, userId);
        return sendRequest(ActionType.SELLER_DELETE_ITEM, request);
    }

    /**
     * GET_SELLER_ITEMS
     *
     * Client khong can gui sellerId.
     * Server lay sellerId tu ClientSession va tra SocketResponse.body = List<ItemSummaryDTO>.
     */
    public SocketResponse getSellerItems() {
        return sendRequest(ActionType.GET_SELLER_ITEMS, new JsonObject());
    }

    /**
     * GET_ITEM_DETAIL
     *
     * Client gui itemId.
     * Server tra SocketResponse.body = ItemDetailDTO neu user co quyen xem item do.
     */
    public SocketResponse getItemDetail(String itemId) {
        GetItemDetailRequest request = new GetItemDetailRequest(itemId);
        return sendRequest(ActionType.GET_ITEM_DETAIL, request);
    }

    /**
     * Helper parse body don le, dung cho ItemDetailDTO va cac DTO item sau nay.
     */
    public <T> T parseBody(SocketResponse response, Class<T> bodyType) {
        if (response == null || !response.isSuccess() || response.getBody() == null || response.getBody().isJsonNull()) {
            return null;
        }

        return gson.fromJson(response.getBody(), bodyType);
    }

    /**
     * Parse response.body cua CREATE_ITEM, UPDATE_ITEM, GET_ITEM_DETAIL.
     */
    public ItemDetailDTO parseItemDetail(SocketResponse response) {
        return parseBody(response, ItemDetailDTO.class);
    }

    /**
     * Parse response.body cua GET_SELLER_ITEMS.
     */
    public List<ItemSummaryDTO> parseItemSummaryList(SocketResponse response) {
        if (response == null || !response.isSuccess() || response.getBody() == null || response.getBody().isJsonNull()) {
            return List.of();
        }

        Type listType = new TypeToken<List<ItemSummaryDTO>>() {}.getType();
        return gson.fromJson(response.getBody(), listType);
    }

    /**
     * Ham gui request chung cho moi action item.
     *
     * Luong chuan:
     * - DTO item -> JsonObject.
     * - JsonObject -> SocketRequest.
     * - SocketRequest -> ClientSocketService.
     * - ClientSocketService tra SocketResponse co requestId tuong ung.
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
     * Chuan hoa request body truoc khi dua vao SocketRequest.
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
}
