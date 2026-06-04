package com.auction.controller;

import com.auction.dto.CreateItemRequest;
import com.auction.dto.DeleteItemRequest;
import com.auction.dto.GetItemDetailRequest;
import com.auction.dto.ItemDetailDTO;
import com.auction.dto.ItemSummaryDTO;
import com.auction.dto.UpdateItemRequest;
import com.auction.enums.ItemStatus;
import com.auction.enums.UserRole;
import com.auction.exception.AuthorizationErrorCode;
import com.auction.exception.AuthorizationException;
import com.auction.exception.ValidationErrorCode;
import com.auction.exception.ValidationException;
import com.auction.network.ClientSession;
import com.auction.service.ItemService;
import com.auction.utils.GsonProvider;

import java.util.List;

/**
 * Bộ điều khiển tiếp nhận các yêu cầu liên quan đến quản lý sản phẩm đấu giá.
 */
public class ItemController {
    private final com.google.gson.Gson gson = GsonProvider.getGson();
    private final ItemService itemService = new ItemService();

    public ItemDetailDTO createItem(String bodyJson, ClientSession session) {
        CreateItemRequest request = parseBody(bodyJson, CreateItemRequest.class);
        String sellerId = requireLoggedInUserId(session);

        requireText(request.getNormalizedItemType(), "itemType must not be empty.");
        return itemService.addItem(
                request.getNormalizedItemType(),
                request.toItemDataMap(sellerId)
        );
    }

    public ItemDetailDTO updateItem(String bodyJson, ClientSession session) {
        UpdateItemRequest request = parseBody(bodyJson, UpdateItemRequest.class);
        requireText(request.getItemId(), "itemId must not be empty.");
        requireText(request.getNormalizedItemType(), "itemType must not be empty.");

        ItemDetailDTO currentItem = itemService.getDetailedItem(request.getItemId());
        requireItemOwnerOrAdmin(currentItem, session);

        return itemService.updateItemInfo(
                request.getItemId(),
                request.getNormalizedItemType(),
                request.toUpdateDataMap()
        );
    }

    public void deleteItem(String bodyJson, ClientSession session) {
        DeleteItemRequest request = parseBody(bodyJson, DeleteItemRequest.class);
        requireText(request.getItemId(), "itemId must not be empty.");

        ItemDetailDTO currentItem = itemService.getDetailedItem(request.getItemId());
        requireItemOwnerOrAdmin(currentItem, session);

        itemService.deleteItem(request.getItemId());
    }

    public List<ItemSummaryDTO> getSellerItems(ClientSession session) {
        String sellerId = requireLoggedInUserId(session);
        return itemService.getSellerItems(sellerId);
    }

    public ItemDetailDTO getItemDetail(String bodyJson, ClientSession session) {
        GetItemDetailRequest request = parseBody(bodyJson, GetItemDetailRequest.class);
        requireText(request.getItemId(), "itemId must not be empty.");

        ItemDetailDTO item = itemService.getDetailedItem(request.getItemId());
        requireItemOwnerOrAdmin(item, session);
        return item;
    }

    private <T> T parseBody(String bodyJson, Class<T> requestType) {
        if (isBlank(bodyJson)) {
            throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Request body must not be empty.");
        }

        T request = gson.fromJson(bodyJson, requestType);
        if (request == null) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Request body is invalid.");
        }
        return request;
    }

    private String requireLoggedInUserId(ClientSession session) {
        if (session == null || isBlank(session.getUserId())) {
            throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "User is not logged in.");
        }
        return session.getUserId();
    }

    private void requireItemOwnerOrAdmin(ItemDetailDTO item, ClientSession session) {
        String userId = requireLoggedInUserId(session);
        if (session.getRole() == UserRole.ADMIN) {
            return;
        }

        if (!userId.equals(item.getSellerId())) {
            throw new AuthorizationException(AuthorizationErrorCode.RESOURCE_OWNERSHIP_VIOLATION);
        }
    }

    private void requireText(String value, String message) {
        if (isBlank(value)) {
            throw new ValidationException(ValidationErrorCode.MISSING_REQUIRED_FIELD, message);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
