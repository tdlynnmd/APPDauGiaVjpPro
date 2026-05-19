package com.auction.network;

import com.auction.dto.*;
import com.auction.enums.ActionType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.List;

/**
 * ClientAuctionApi gửi các request liên quan đến đấu giá từ Client sang Server.
 *
 * Vai trò:
 * - Tạo Request DTO đúng với từng action đấu giá.
 * - Bọc Request DTO vào SocketRequest.
 * - Gửi request qua socket.
 * - Nhận SocketResponse từ Server.
 * - Cung cấp helper để parse response.body thành DTO cụ thể.
 *
 * Lưu ý:
 * - Class này không xử lý giao diện.
 * - Class này không kiểm tra phân quyền.
 * - Class này không xử lý nghiệp vụ đấu giá.
 * - Server mới là nơi kiểm tra quyền và xử lý nghiệp vụ thật.
 */
public class ClientAuctionApi {
    private final Gson gson = new Gson();

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
        return sendRequest(ActionType.SUBSCRIBE_AUCTION, request);
    }

    /**
     * Gửi request hủy đăng ký nhận realtime update của một phiên đấu giá.
     */
    public SocketResponse unsubscribeAuction(String auctionId) {
        AuctionSubscriptionRequest request = new AuctionSubscriptionRequest(auctionId);
        return sendRequest(ActionType.UNSUBSCRIBE_AUCTION, request);
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
     * Hàm gửi request chung cho mọi action đấu giá.
     *
     * Tất cả method phía trên đều gom về đây để tránh lặp logic:
     * - lấy socket
     * - tạo SocketRequest
     * - gửi JSON
     * - đọc JSON response
     * - parse thành SocketResponse
     */
    private SocketResponse sendRequest(ActionType actionType, Object requestBody) {
        /*
         * socketRequest được khai báo ngoài try để nếu xảy ra exception,
         * catch vẫn lấy được requestId và action trả về trong SocketResponse.failure.
         */
        SocketRequest socketRequest = null;

        try {
            /*
             * ClientNetworkManager là nơi giữ kết nối socket duy nhất tới Server.
             * Các API như AuthApi/AuctionApi đều dùng chung connection này.
             */
            ClientNetworkManager network = ClientNetworkManager.getInstance();

            PrintWriter writer = network.getWriter();   //writer dùng để gửi 1 dòng JSON sang Server.
            BufferedReader reader = network.getReader();    //reader dùng để đọc 1 dòng JSON response Server trả về.

            /*
             * requestBody có thể là DTO như PlaceBidRequest, CreateAuctionRequest,
             * hoặc JsonObject rỗng với action không cần body.
             * toJsonObject() chuẩn hóa tất cả thành JsonObject.
             */
            JsonObject body = toJsonObject(requestBody);

            /*
             * SocketRequest là phong bì chung Client gửi lên Server.
             * actionType.name() tạo action chuẩn theo enum, ví dụ "PLACE_BID".
             * body là dữ liệu chi tiết của action đó.
             */
            socketRequest = new SocketRequest(actionType.name(), body);
            writer.println(gson.toJson(socketRequest));     //Chuyển SocketRequest thành JSON string rồi gửi qua socket.

            /*
             * Chờ Server xử lý và trả về một dòng JSON.
             * Server hiện trả theo format SocketResponse.
             */
            String responseJson = reader.readLine();

            /*
             * Nếu Server đóng kết nối hoặc không trả gì, responseJson sẽ null/rỗng.
             * Khi đó vẫn trả SocketResponse.failure để Controller UI có message hiển thị.
             */
            if (responseJson == null || responseJson.trim().isEmpty()) {
                return SocketResponse.failure(
                        socketRequest.getRequestId(),
                        actionType.name(),
                        "The server did not return any data.",
                        "EMPTY_RESPONSE"
                );
            }

            /*
             * Parse JSON response thành SocketResponse.
             * Lúc này Client chưa parse body ngay, vì mỗi action có body khác nhau.
             */
            return gson.fromJson(responseJson, SocketResponse.class);

        } catch (Exception e) {
            /*
             * Nếu lỗi kết nối, lỗi parse JSON, hoặc lỗi socket bất kỳ,
             * không throw lên UI mà đổi thành SocketResponse.failure.
             */
            e.printStackTrace();

            /*
             * Nếu lỗi xảy ra trước khi tạo SocketRequest, requestId sẽ là null.
             * Nếu đã tạo được SocketRequest, giữ requestId để dễ debug.
             */
            String requestId = socketRequest == null ? null : socketRequest.getRequestId();

            return SocketResponse.failure(
                    requestId,
                    actionType.name(),
                    "Cannot connect to the server. Please check whether the server is running.",
                    "CONNECTION_ERROR"
            );
        }
    }


    /**
     * Convert request body thành JsonObject.
     *
     * Nếu requestBody đã là JsonObject thì dùng trực tiếp.
     * Nếu requestBody là DTO thì convert bằng Gson.
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
