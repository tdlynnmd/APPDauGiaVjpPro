package com.auction.network;
/**
 Là bo điều phối request phía Server

    Nhiệm vụ
  Nhận JSON từ ClientHandler.
  Chuyển JSON thành SocketRequest.
  Nhìn action.
  Gọi đúng handler: LOGIN, REGISTER, LOGOUT.
  Gửi response về Client qua ClientSession.
 */

import com.auction.controller.AuthController;
import com.auction.dto.*;
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

                case "REGISTER":
                    handleRegister(socketRequest.getBody(), session);

                case "LOGOUT":
                    handleLogout(socketRequest.getBody(), session);

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

    //// --- Các hàm xử lý chi tiết (Delegation) ---


    /**
      Xử lý LOGIN.

      Luồng:
      1. Parse body thành LoginRequest.
      2. Gọi AuthController.login().
      3. Nếu login thành công, gắn userId vào ClientSession.
      4. Đăng ký connection vào ConnectionManage.
      5. Gửi LoginResponse về Client.
     */
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

    private void handleRegister(String bodyJson, ClientSession session){
        /**
          Xử lý REGISTER.

          Luồng:
          1. Parse body thành RegisterRequest.
          2. Gọi AuthController.register().
          3. Gửi RegisterResponse về Client.

          Lưu ý:
          - Đăng ký thành công chưa tự động login.
          - Vì vậy không gọi session.setUserId() ở đây.
         */
        RegisterRequest registerRequest = gson.fromJson(bodyJson, RegisterRequest.class);
        RegisterResponse response = authController.register(registerRequest);
        session.sendMessage(gson.toJson(response));
    }

    private void handleLogout(String bodyJson, ClientSession session) {
        /**
          Xử lý LOGOUT.

          Luồng:
          1. Parse body thành LogoutRequest.
          2. Ưu tiên lấy userId từ ClientSession phía Server.
          3. Nếu session chưa có userId thì lấy dự phòng từ LogoutRequest.
          4. Gọi AuthController.logout() để kiểm tra userId hợp lệ.
          5. Nếu thành công, xóa session khỏi ConnectionManage.
          6. Set session.userId = null.
          7. Gửi LogoutResponse về Client.
         */
        LogoutRequest logoutRequest = gson.fromJson(bodyJson, LogoutRequest.class);

        // Ưu tiên lấy userId từ session phía Server vì đây là dữ liệu Server đang quản lý.
        String userId = session.getUserId();

        if (userId == null) {
            LogoutResponse response = LogoutResponse.failure(
                    "User is not logged in on this session.",
                    "USER_NOT_LOGGED_IN"
            );
            session.sendMessage(gson.toJson(response));
            return;
        }

        LogoutResponse response = authController.logout(userId);

        if (response.isSuccess()) {
            // Xóa đúng session hiện tại khỏi danh sách online.
            if (session.getUserId() != null) {
                ConnectionManage.getInstance().removeConnection(session.getUserId(), session);
            }

            // Sau logout, socket này không còn gắn với user nào nữa.
            session.setUserId(null);
        }

        session.sendMessage(gson.toJson(response));
    }


    private void sendError(ClientSession session, String code, String message) {
        LoginResponse errorResponse = LoginResponse.failure(message, code);
        session.sendMessage(gson.toJson(errorResponse));
    }
}