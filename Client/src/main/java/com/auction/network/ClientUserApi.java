package com.auction.network;

import com.auction.dto.DepositRequest;
import com.auction.dto.SocketRequest;
import com.auction.dto.SocketResponse;
import com.auction.dto.UserDTO;
import com.auction.dto.WithdrawRequest;
import com.auction.dto.UpdateProfileRequest;
import com.auction.dto.UpdatePasswordRequest;
import com.auction.enums.ActionType;
import com.auction.service.ClientSocketService;
import com.auction.utils.GsonProvider;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * API Client xử lý các kết nối nghiệp vụ liên quan đến tài khoản người dùng và ví tiền.
 */
public class ClientUserApi {
    private final Gson gson = GsonProvider.getGson();

    /**
     * GET_USER_PROFILE
     *
     * Lấy thông tin user hiện tại từ Server.
     * Backend sẽ dựa vào session socket hiện tại để biết user là ai.
     *
     * Response thành công:
     * - SocketResponse.body = UserDTO
     * - UserDTO.availableBalance: tiền khả dụng
     * - UserDTO.frozenBalance: tiền đang bị đóng băng trong phiên đấu giá
     */
    public SocketResponse getUserProfile() {
        return sendRequest(ActionType.GET_USER_PROFILE, new JsonObject());
    }

    /**
     * UPDATE_PROFILE
     *
     * Gửi yêu cầu cập nhật thông tin cá nhân.
     */
    public SocketResponse updateProfile(String username, String email) {
        UpdateProfileRequest request = new UpdateProfileRequest(username, email);
        return sendRequest(ActionType.UPDATE_PROFILE, request);
    }

    /**
     * UPDATE_PASSWORD
     *
     * Gửi yêu cầu thay đổi mật khẩu.
     */
    public SocketResponse updatePassword(String oldPassword, String newPassword) {
        UpdatePasswordRequest request = new UpdatePasswordRequest(oldPassword, newPassword);
        return sendRequest(ActionType.UPDATE_PASSWORD, request);
    }

    /**
     * DEPOSIT_MONEY
     *
     * Gửi yêu cầu nạp tiền.
     * Backend validate amount > 0 và kiểm tra quyền BIDDER.
     */
    public SocketResponse depositMoney(double amount) {
        DepositRequest request = new DepositRequest(amount);
        return sendRequest(ActionType.DEPOSIT_MONEY, request);
    }

    /**
     * WITHDRAW_MONEY
     *
     * Gửi yêu cầu rút tiền.
     * Backend validate:
     * - amount > 0
     * - user phải là BIDDER
     * - số dư khả dụng phải đủ
     * - tài khoản không bị khóa
     */
    public SocketResponse withdrawMoney(double amount) {
        WithdrawRequest request = new WithdrawRequest(amount);
        return sendRequest(ActionType.WITHDRAW_MONEY, request);
    }

    /**
     * Parse response.body thành UserDTO.
     *
     * Dùng sau:
     * - getUserProfile()
     * - depositMoney(...)
     * - withdrawMoney(...)
     *
     * Nếu response lỗi hoặc không có body thì trả null để Controller tự hiển thị lỗi.
     */
    public UserDTO parseUser(SocketResponse response) {
        if (hasNoUsableBody(response)) {
            return null;
        }

        return gson.fromJson(response.getBody(), UserDTO.class);
    }

    /**
     * Hàm gửi request chung cho các action ví.
     *
     * Luồng chuẩn:
     * - DTO -> JsonObject
     * - JsonObject -> SocketRequest
     * - SocketRequest -> ClientSocketService
     * - ClientSocketService chờ đúng SocketResponse có requestId tương ứng
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
     * Chuẩn hóa request body trước khi gửi sang Server.
     *
     * Có action không cần body thật, ví dụ GET_USER_PROFILE,
     * nên trường hợp null sẽ được chuyển thành JsonObject rỗng.
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
     * Kiểm tra response có thể parse body hay không.
     */
    private boolean hasNoUsableBody(SocketResponse response) {
        return response == null
                || !response.isSuccess()
                || response.getBody() == null
                || response.getBody().isJsonNull();
    }
}