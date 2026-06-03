package com.auction.network;

import com.auction.dto.*;
import com.auction.enums.ActionType;
import com.auction.service.ClientSocketService;
import com.auction.utils.GsonProvider;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * ClientBidHistoryApi la lop API phia Client cho chuc nang lich su dat gia.
 *
 * Lien he voi backend:
 * - ActionType.GET_MY_BID_HISTORY duoc Server RequestDispatcher tiep nhan.
 * - Server lay bidderId tu ClientSession phia Server, khong tin bidderId tu Client gui len.
 * - UserController goi BidTransactionService.
 * - BidTransactionService tra ve PageDTO<BidTransactionDTO>.
 *
 * Vai tro cua class nay:
 * - Tao request phan trang cho lich su bid cua user hien tai.
 * - Gui SocketRequest qua ClientSocketService.
 * - Parse SocketResponse.body ve PageDTO<BidTransactionDTO>.
 */
public class ClientBidHistoryApi {
    private final Gson gson = GsonProvider.getGson();

    /**
     * Goi backend de lay lich su dat gia cua Bidder dang dang nhap.
     *
     * Client chi gui page va pageSize.
     * Client khong gui bidderId vi Server da co userId trong session socket.
     */
    public SocketResponse getMyBidHistory(int page, int pageSize) {
        GetBidderHistoryRequest request = new GetBidderHistoryRequest(page, pageSize);
        return sendRequest(ActionType.GET_MY_BID_HISTORY, request);
    }
    /**
     * Lay lich su dat gia cua mot phien dau gia cu the.
     *
     * Controller chart se truyen auctionId, page va pageSize.
     * Server tra ve PageDTO<BidTransactionDTO> cua dung phien do.
     */
    public SocketResponse getAuctionBidHistory(String auctionId, int page, int pageSize) {
        GetAuctionBidsRequest request = new GetAuctionBidsRequest(auctionId, page, pageSize);
        return sendRequest(ActionType.GET_AUCTION_BID_HISTORY, request);
    }

    /**
     * Parse response lich su bid theo phien thanh PageDTO.
     *
     * Neu response loi/rong thi tra page rong de chart khong bi crash UI.
     */
    public PageDTO<BidTransactionDTO> parseAuctionBidHistoryPage(SocketResponse response) {
        if (hasNoUsableBody(response)) {
            return new PageDTO<>(List.of(), 1, 0, 0);
        }

        Type pageType = new TypeToken<PageDTO<BidTransactionDTO>>() {}.getType();
        return gson.fromJson(response.getBody(), pageType);
    }

    /**
     * Parse response.body thanh PageDTO<BidTransactionDTO>.
     *
     * Can TypeToken vi PageDTO<T> co generic type.
     * Neu response rong/loi thi tra page rong de Controller xu ly an toan.
     */
    public PageDTO<BidTransactionDTO> parseMyBidHistoryPage(SocketResponse response) {
        if (hasNoUsableBody(response)) {
            return new PageDTO<>(List.of(), 1, 0, 0);
        }

        Type pageType = new TypeToken<PageDTO<BidTransactionDTO>>() {}.getType();
        return gson.fromJson(response.getBody(), pageType);
    }

    /**
     * Gui request chung cho cac action lich su bid.
     *
     * Luong chay:
     * - DTO request -> JsonObject.
     * - JsonObject -> SocketRequest.
     * - SocketRequest -> ClientSocketService.
     * - ClientSocketService tra ve SocketResponse co requestId tuong ung.
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
     * Kiem tra response co body parse duoc hay khong.
     */
    private boolean hasNoUsableBody(SocketResponse response) {
        return response == null
                || !response.isSuccess()
                || response.getBody() == null
                || response.getBody().isJsonNull();
    }
}