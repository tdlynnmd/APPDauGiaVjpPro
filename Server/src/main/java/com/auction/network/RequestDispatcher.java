package com.auction.network;

import com.auction.controller.AuthController;
import com.auction.dto.LoginRequest;
import com.auction.dto.LoginResponse;
import com.auction.dto.SocketRequest;
import com.auction.manage.ConnectionManage;
import com.google.gson.Gson;

public class RequestDispatcher {
    private final Gson gson = new Gson();
    private final AuthController authController = new AuthController();

    // Bạn có thể thêm AuctionController, UserController ở đây...

    public void processRequest(String requestJson, ClientSession session) {
        try {
            // 1. Dịch chuỗi Json thành Object
            SocketRequest socketRequest = gson.fromJson(requestJson, SocketRequest.class);

            if (socketRequest == null || socketRequest.getAction() == null) {
                sendError(session, "BAD_REQUEST", "Request không hợp lệ");
                return;
            }

            // 2. CÔNG TẮC ĐIỀU HƯỚNG (Routing)
            switch (socketRequest.getAction()) {
                case "LOGIN":
                    handleLogin(socketRequest.getBody(), session);
                    break;

                case "PLACE_BID":
                    // auctionController.placeBid(socketRequest.getBody(), session);
                    break;

                // Thêm hàng tá case khác vào đây thoải mái mà không sợ làm rối luồng mạng

                default:
                    sendError(session, "UNSUPPORTED_ACTION", "Action không được hỗ trợ");
            }
        } catch (Exception e) {
            sendError(session, "SERVER_ERROR", "Lỗi xử lý hệ thống: " + e.getMessage());
        }
    }

    // --- Các hàm xử lý chi tiết (Delegation) ---

    private void handleLogin(String bodyJson, ClientSession session) {
        LoginRequest loginRequest = gson.fromJson(bodyJson, LoginRequest.class);

        // Gọi AuthController (nơi chứa AuthService)
        // Lưu ý: AuthController nên trả về DTO hoặc Response object
        LoginResponse response = authController.login(loginRequest);

        // Cực kỳ quan trọng: Nếu login thành công, phải GẮN ID vào session
        if (response.isSuccess()) {
            session.setUserId(response.getUserDto().getId());
            // Đăng ký vào tổng đài
            ConnectionManage.getInstance().registerConnection(response.getUserDto().getId(), session);
        }

        // Gửi trả kết quả cho Client thông qua session
        session.sendMessage(gson.toJson(response));
    }

    private void sendError(ClientSession session, String code, String message) {
        LoginResponse errorResponse = LoginResponse.failure(message, code);
        session.sendMessage(gson.toJson(errorResponse));
    }
}