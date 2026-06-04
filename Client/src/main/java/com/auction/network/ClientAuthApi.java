package com.auction.network;

import com.auction.dto.LoginRequest;
import com.auction.dto.LogoutRequest;
import com.auction.dto.RegisterRequest;
import com.auction.dto.SocketRequest;
import com.auction.dto.SocketResponse;
import com.auction.enums.ActionType;
import com.auction.enums.UserRole;
import com.auction.service.ClientSocketService;
import com.auction.utils.GsonProvider;
import com.google.gson.JsonObject;

/**
 * API Client xử lý xác thực đăng nhập, đăng ký tài khoản.
 */
public class ClientAuthApi {
    private final com.google.gson.Gson gson = GsonProvider.getGson();

    public SocketResponse login(String usernameOrEmail, String password) {
        LoginRequest request = new LoginRequest(usernameOrEmail, password);
        return sendRequest("LOGIN", request);
    }

    public SocketResponse register(String username, String password, String email, UserRole role) {
        RegisterRequest request = new RegisterRequest(username, password, email, role);
        return sendRequest("REGISTER", request);
    }

    public SocketResponse logout(String userId) {
        LogoutRequest request = new LogoutRequest(userId);
        return sendRequest("LOGOUT", request);
    }

    /**
     * Parse body trong SocketResponse thành DTO cụ thể.
     *
     * Ví dụ:
     * LoginResultDTO result = authApi.parseBody(response, LoginResultDTO.class);
     */
    public <T> T parseBody(SocketResponse response, Class<T> bodyType) {
        if (response == null || response.getBody() == null || response.getBody().isJsonNull()) {
            return null;
        }

        return gson.fromJson(response.getBody(), bodyType);
    }

    /**
     * Hàm gửi request chung cho mọi action auth.
     */
    private SocketResponse sendRequest(String action, Object requestBody) {
        SocketRequest socketRequest = null;

        try {
            JsonObject body = gson.toJsonTree(requestBody).getAsJsonObject();
            socketRequest = new SocketRequest(ActionType.valueOf(action), body);

            return ClientSocketService.getInstance().sendRequest(socketRequest);

        } catch (Exception e) {
            e.printStackTrace();

            String requestId = socketRequest == null ? null : socketRequest.getRequestId();

            return SocketResponse.failure(
                    requestId,
                    ActionType.valueOf(action),
                    "Cannot connect to the server. Please check whether the server is running.",
                    "CONNECTION_ERROR"
            );
        }
    }
}
