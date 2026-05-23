package com.auction.network;

import com.auction.dto.*;
import com.auction.enums.ActionType;
import com.auction.service.ClientSocketService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * ClientAuctionApi giờ sẽ tạo request còn việc ửi đi và nhận sẽ la của ClientSocketService
 *
 * Vai trò:
 * - Tạo Request DTO đúng với từng action đấu giá.
 * - Bọc Request DTO vào SocketRequest.
 * - Cung cấp helper để parse response.body thành DTO cụ thể.
 *
 * Lưu ý:
 * - Class này không xử lý giao diện.
 * - Class này không kiểm tra phân quyền.
 * - Class này không xử lý nghiệp vụ đấu giá.
 * - Server mới là nơi kiểm tra quyền và xử lý nghiệp vụ thật.
 */
public class ClientAuctionApi {
    // Thay thế dòng khởi tạo cũ bằng GsonBuilder để cấu hình chuyển đổi LocalDateTime (Tránh lỗi InaccessibleObjectException trên Java mới)
    private final Gson gson = new com.google.gson.GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class, (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, typeOfT, context) -> {
                try {
                    // Thử parse theo định dạng chuẩn ISO (ví dụ: 2026-05-22T05:15:30)
                    return java.time.LocalDateTime.parse(json.getAsString(), java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (Exception e) {
                    // Dự phòng nếu Server trả về định dạng chuỗi custom tùy biến có dấu cách (ví dụ: "yyyy-MM-dd HH:mm:ss")
                    java.time.format.DateTimeFormatter backupFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    return java.time.LocalDateTime.parse(json.getAsString(), backupFormatter);
                }
            })
            .registerTypeAdapter(java.time.LocalDateTime.class, (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, typeOfSrc, context) ->
                    new com.google.gson.JsonPrimitive(src.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            )
            .create();

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
     * Gửi request hủy một phiên đấu giá.
     */
    public SocketResponse cancelAuction(String auctionId, String reason) {
        CancelAuctionRequest request = new CancelAuctionRequest(auctionId, reason);
        return sendRequest(ActionType.CANCEL_AUCTION, request);
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
        // Nếu Server trả body null, Client không cố parse để tránh NullPointerException.
        if (response == null || response.getBody() == null || response.getBody().isJsonNull()) {
            return null;
        }

        // Gson chuyển JsonElement trong response.body thành class cụ thể mà Controller cần dùng.
        return gson.fromJson(response.getBody(), bodyType);
    }

    /**
     * Parse response.body thành danh sách AuctionSummaryDTO.
     *
     * Vì List<T> bị Java type erasure, không thể dùng List.class để parse chính xác.
     * Do đó cần TypeToken<List<AuctionSummaryDTO>>.
     */
    public List<AuctionSummaryDTO> parseAuctionSummaryList(SocketResponse response) {
        // Nếu response không có body, trả danh sách rỗng để UI vẫn xử lý an toàn.
        if (response == null || response.getBody() == null || response.getBody().isJsonNull()) {
            return List.of();
        }

        // TypeToken giúp Gson biết đây là List<AuctionSummaryDTO>, không phải List<Object>.
        Type listType = new TypeToken<List<AuctionSummaryDTO>>() {}.getType();

        // Parse JsonElement body thành List<AuctionSummaryDTO>.
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
        /*
         * socketRequest duoc khai bao ngoai try de neu xay ra exception,
         * catch van lay duoc requestId va action tra ve trong SocketResponse.failure.
         */
        SocketRequest socketRequest = null;

        try {
            /*
             * requestBody co the la DTO nhu PlaceBidRequest, CreateAuctionRequest,
             * hoac JsonObject rong voi action khong can body.
             * toJsonObject() chuan hoa tat ca thanh JsonObject.
             */
            JsonObject body = toJsonObject(requestBody);

            /*
             * SocketRequest la phong bi chung Client gui len Server.
             * actionType.name() tao action chuan theo enum, vi du "PLACE_BID".
             * body la du lieu chi tiet cua action do.
             */
            socketRequest = new SocketRequest(ActionType.valueOf(actionType.name()), body);

            /*
             * Khong doc socket truc tiep trong API nua.
             * ClientSocketService la noi duy nhat doc message tu Server,
             * nen no co the tach RESPONSE cho request va EVENT realtime cho UI.
             */
            return ClientSocketService.getInstance().sendRequest(socketRequest);

        } catch (Exception e) {
            /*
             * Neu loi ket noi, loi parse JSON, hoac loi socket bat ky,
             * khong throw len UI ma doi thanh SocketResponse.failure.
             */
            e.printStackTrace();

            /*
             * Neu loi xay ra truoc khi tao SocketRequest, requestId se la null.
             * Neu da tao duoc SocketRequest, giu requestId de de debug.
             */
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
