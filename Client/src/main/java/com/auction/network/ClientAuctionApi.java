package com.auction.network;

import com.auction.dto.*;
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
 * API Client xử lý các kết nối nghiệp vụ liên quan đến đấu giá phía Client.
 */
public class ClientAuctionApi {
    private final Gson gson = GsonProvider.getGson();

    /**
     * Gửi request lấy danh sách các phiên đấu giá đang hoạt động.
     *
     * Action:
     * - GET_ACTIVE_AUCTIONS
     *
     * Server trả về:
     * - SocketResponse.body = List<AuctionSummaryDTO>
     */
    public SocketResponse getActiveAuctions() {
        return sendRequest(ActionType.GET_ACTIVE_AUCTIONS, new JsonObject());
    }
    /**
     * Gửi request lấy chi tiết một phiên đấu giá.
     *
     * Client chỉ cần gửi auctionId.
     * Server sẽ dùng auctionId để tìm AuctionDetailDTO.
     */
    public SocketResponse getAuctionDetail(String auctionId) {
        GetAuctionDetailRequest request = new GetAuctionDetailRequest(auctionId);
        return sendRequest(ActionType.GET_AUCTION_DETAIL, request);
    }

    /**
     * Gửi request tạo phiên đấu giá.
     *
     * Client không gửi sellerId.
     * Server sẽ lấy sellerId từ ClientSession để tránh giả mạo người bán.
     */
    public SocketResponse createAuction(String itemId, double stepPrice, String startTime, String endTime) {
        CreateAuctionRequest request = new CreateAuctionRequest(
                endTime,
                itemId,
                startTime,
                stepPrice
        );

        return sendRequest(ActionType.CREATE_AUCTION, request);
    }

    /**
     * Gửi request đặt giá vào một phiên đấu giá.
     */
    public SocketResponse placeBid(String auctionId, double amount) {
        PlaceBidRequest request = new PlaceBidRequest(auctionId, amount);
        return sendRequest(ActionType.PLACE_BID, request);
    }
    public SocketResponse setupAutoBid(String auctionId, double maxBid, double increment) {
        SetupAutoBidRequest request = new SetupAutoBidRequest(auctionId, maxBid, increment);
        return sendRequest(ActionType.SETUP_AUTO_BID, request);
    }

    public SocketResponse cancelAutoBid(String auctionId) {
        CancelAutoBidRequest request = new CancelAutoBidRequest(auctionId);
        return sendRequest(ActionType.CANCEL_AUTO_BID, request);
    }
    /**
     * Gửi request đăng ký nhận realtime update của một phiên đấu giá.
     */
    public SocketResponse subscribeAuction(String auctionId) {
        AuctionSubscriptionRequest request = new AuctionSubscriptionRequest(auctionId);
        return sendRequest(ActionType.AUCTION_SUBSCRIBED, request);
    }

    /**
     * Gửi request hủy đăng ký nhận realtime update của một phiên đấu giá.
     */
    public SocketResponse unsubscribeAuction(String auctionId) {
        AuctionSubscriptionRequest request = new AuctionSubscriptionRequest(auctionId);
        return sendRequest(ActionType.AUCTION_UNSUBSCRIBED, request);
    }
    /**
     * Client vao man live bidding cua mot phien dau gia.
     *
     * Vai tro:
     * - Gui action LIVE_ENTERED len server.
     * - Server se them user vao live room.
     * - Server co the broadcast lai so nguoi dang xem cho cac client trong room.
     */
    public SocketResponse enterLiveRoom(String auctionId) {
        AuctionSubscriptionRequest request = new AuctionSubscriptionRequest(auctionId);
        return sendRequest(ActionType.LIVE_ENTERED, request);
    }

    /**
     * Client roi khoi man live bidding cua mot phien dau gia.
     *
     * Vai tro:
     * - Gui action LIVE_EXITED len server.
     * - Server se xoa user khoi live room.
     * - Client sau do khong nen tiep tuc nhan event realtime cua room nay.
     */
    public SocketResponse exitLiveRoom(String auctionId) {
        AuctionSubscriptionRequest request = new AuctionSubscriptionRequest(auctionId);
        return sendRequest(ActionType.LIVE_EXITED, request);
    }

    /**
     * Gửi request hủy một phiên đấu giá.
     */
    public SocketResponse cancelAuction(String auctionId, String reason,String userId) {
        CancelAuctionRequest request = new CancelAuctionRequest(auctionId, reason, userId);
        return sendRequest(ActionType.SELLER_CANCEL_AUCTION, request);
    }

    /**
     * Parse response.body thành DTO cụ thể.
     *
     * Dùng khi body là một object đơn:
     * - AuctionDetailDTO
     * - Boolean
     * - DTO khác sau này
     */
    public <T> T parseBody(SocketResponse response, Class<T> bodyType) {
        if (response == null || response.getBody() == null || response.getBody().isJsonNull()) {
            return null;
        }

        return gson.fromJson(response.getBody(), bodyType);
    }

    /**
     * Parse response.body thành danh sách AuctionSummaryDTO.
     *
     * Vì List<T> bị Java type erasure, không thể dùng List.class để parse chính xác.
     * Do đó cần TypeToken<List<AuctionSummaryDTO>>.
     */
    public List<AuctionSummaryDTO> parseAuctionSummaryList(SocketResponse response) {
        if (response == null || response.getBody() == null || response.getBody().isJsonNull()) {
            return List.of();
        }

        Type listType = new TypeToken<List<AuctionSummaryDTO>>() {}.getType();

        return gson.fromJson(response.getBody(), listType);
    }

    /**
     * Parse response.body thành AuctionDetailDTO.
     */
    public AuctionDetailDTO parseAuctionDetail(SocketResponse response) {
        return parseBody(response, AuctionDetailDTO.class);
    }

    /**
     * Ham gui request chung cho moi action dau gia.
     *
     * Tat ca method phia tren deu gom ve day de tranh lap logic:
     * - tao SocketRequest
     * - chuan hoa request body thanh JsonObject
     * - giao request cho ClientSocketService gui sang Server
     * - nhan dung RESPONSE co requestId trung voi request vua gui
     */
    private SocketResponse sendRequest(ActionType actionType, Object requestBody) {
        
        SocketRequest socketRequest = null;

        try {
            
            JsonObject body = toJsonObject(requestBody);

            socketRequest = new SocketRequest(ActionType.valueOf(actionType.name()), body);

            return ClientSocketService.getInstance().sendRequest(socketRequest);

        } catch (Exception e) {
            
            e.printStackTrace();

            String requestId = socketRequest == null ? null : socketRequest.getRequestId();

            return SocketResponse.failure(
                    requestId,
                    ActionType.valueOf(actionType.name()),
                    "Cannot connect to the server. Please check whether the server is running.",
                    "CONNECTION_ERROR"
            );
        }
    }
    /**
     * GET_SELLER_AUCTIONS
     * Chuc nang: Seller lay danh sach cac phien dau gia do chinh minh tao.
     * Client khong gui sellerId, server lay sellerId tu session.
     */
    public SocketResponse getSellerAuctions() {
        return sendRequest(ActionType.GET_SELLER_AUCTIONS, new JsonObject());
    }

    /**
     * UPDATE_AUCTION
     * Chuc nang: Seller cap nhat phien dau gia chua chay.
     * Chi cap nhat stepPrice, startTime, endTime; server se check owner va status OPEN.
     */
    public SocketResponse updateAuction(String auctionId, double stepPrice,
                                        LocalDateTime startTime, LocalDateTime endTime) {
        UpdateAuctionRequest request = new UpdateAuctionRequest(
                auctionId,
                stepPrice,
                startTime,
                endTime
        );

        return sendRequest(ActionType.UPDATE_AUCTION, request);
    }

    /**
     * Convert request body thành JsonObject.
     *
     * Nếu requestBody đã là JsonObject thì dùng trực tiếp.
     * If requestBody là DTO thì convert bằng Gson.
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
