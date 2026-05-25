package com.auction.network;

import com.auction.controller.AuctionController;
import com.auction.controller.AuthController;
import com.auction.controller.ItemController;
import com.auction.dto.*;
import com.auction.enums.ActionType;
import com.auction.exception.BaseException;
import com.auction.manage.ConnectionManage;
import com.auction.service.AuthorizationService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * RequestDispatcher là bộ điều phối request phía Server.
 * Nhiệm vụ:
 * - Nhận JSON từ ClientHandler.
 * - Parse JSON thành SocketRequest.
 * - Kiểm tra action hợp lệ.
 * - Kiểm tra phân quyền.
 * - Gọi đúng handler theo action.
 * - Trả kết quả về Client bằng SocketResponse.
 */
public class RequestDispatcher {
    private final Gson gson = new Gson();

    private final AuthController authController = new AuthController();
    private final AuctionController auctionController = new AuctionController();
    private final ItemController itemController = new ItemController();
    private final AuthorizationService authorizationService = new AuthorizationService();

    // 🔥 TỐI ƯU HIỆU NĂNG NHÁNH: Tạo Thread Pool riêng chuyên trách các tác vụ nặng (Database, Synchronized Block).
    // Giúp giải phóng ngay lập tức Luồng Mạng của ClientHandler quay lại đọc luồng byte tiếp theo.
    private final ExecutorService workerPool = Executors.newFixedThreadPool(16);

    /**
     * Điểm vào chính khi Server nhận được một dòng JSON từ Client.
     */
    public void processRequest(String requestJson, ClientSession session) {
        // 🔥 ĐẨY SANG LUỒNG WORKER BẤT ĐỒNG BỘ: Chặn đứng nguy cơ nghẽn hàng đợi mạng của từng Client đơn lẻ
        workerPool.submit(() -> {
            SocketRequest socketRequest = null;

            try {
                socketRequest = gson.fromJson(requestJson, SocketRequest.class);

                if (socketRequest == null || isBlank(socketRequest.getAction().toString())) {
                    sendFailure(session, null, null,
                            "Request không hợp lệ.",
                            "BAD_REQUEST");
                    return;
                }

                ActionType actionType = parseAction(socketRequest.getAction().toString());
                String action = socketRequest.getAction().toString();

                if (actionType == null) {
                    sendFailure(session, socketRequest,
                            "Action không được hỗ trợ.",
                            "UNSUPPORTED_ACTION");
                    return;
                }

                // Kiểm tra phân quyền cưỡng chế dựa trên Exception
                authorizationService.canAccess(action, session);

                switch (actionType) {
                    case LOGIN:
                        handleLogin(socketRequest, session);
                        break;

                    case REGISTER:
                        handleRegister(socketRequest, session);
                        break;

                    case LOGOUT:
                        handleLogout(socketRequest, session);
                        break;

                    case CREATE_ITEM:
                        handleCreateItem(socketRequest, session);
                        break;

                    case UPDATE_ITEM:
                        handleUpdateItem(socketRequest, session);
                        break;

                    case DELETE_ITEM:
                        handleDeleteItem(socketRequest, session);
                        break;

                    case GET_SELLER_ITEMS:
                        handleGetSellerItems(socketRequest, session);
                        break;

                    case GET_ITEM_DETAIL:
                        handleGetItemDetail(socketRequest, session);
                        break;

                    case GET_ACTIVE_AUCTIONS:
                        handleGetActiveAuctions(socketRequest, session);
                        break;

                    case GET_AUCTION_DETAIL:
                        handleGetAuctionDetail(socketRequest, session);
                        break;

                    case CREATE_AUCTION:
                        handleCreateAuction(socketRequest, session);
                        break;

                    case PLACE_BID:
                        handlePlaceBid(socketRequest, session);
                        break;

                    // ================================================================
                    // 🔥 ĐỒNG BỘ 4 HÀM MẠNG MỚI ĐÍCH DANH 1:1 THEO KIẾN TRÚC MỚI
                    // ================================================================
                    case LIVE_ENTERED:
                        // Người dùng mở màn hình chi tiết, cắm socket xem live
                        handleLiveEntered(socketRequest, session);
                        break;

                    case LIVE_EXITED:
                        // Người dùng đóng màn hình chi tiết, tháo socket live
                        handleLiveExited(socketRequest, session);
                        break;

                    case AUCTION_SUBSCRIBED:
                        // Người dùng ấn nút "Theo dõi" nền lưu trữ DB/RAM vĩnh viễn
                        handleAuctionSubscribed(socketRequest, session);
                        break;

                    case AUCTION_UNSUBSCRIBED:
                        // Người dùng ấn nút "Hủy theo dõi" giải phóng DB/RAM
                        handleAuctionUnsubscribed(socketRequest, session);
                        break;

                    case CANCEL_AUCTION:
                        handleCancelAuction(socketRequest, session);
                        break;

                    default:
                        sendFailure(session, socketRequest,
                                "Action không được hỗ trợ.",
                                "UNSUPPORTED_ACTION");
                        break;
                }

            } catch (JsonSyntaxException e) {
                sendFailure(session, socketRequest, "JSON request không hợp lệ.", "INVALID_JSON");

            } catch (BaseException e) {
                // 🔥 HỨNG TẬP TRUNG TOÀN HỆ THỐNG LỖI NGHIỆP VỤ
                System.err.println("[Central Guard] 🚨 Lỗi nghiệp vụ xảy ra lúc [" + e.getTimestamp()
                        + "] | Code: " + e.getErrorCode() + " | Chi tiết: " + e.getMessage());
                sendFailure(session, socketRequest, e.getMessage(), e.getErrorCode());

            } catch (Exception e) {
                // Chốt chặn cuối bảo vệ vòng lặp Server không bao giờ crash/sập kết nối ngoài ý muốn
                System.err.println("[Fatal System Error] 💥 Sự cố hệ thống nghiêm trọng:");
                e.printStackTrace();
                sendFailure(session, socketRequest,
                        "Lỗi xử lý hệ thống.",
                        "SERVER_ERROR");
            }
        });
    }

    /**
     * Xử lý LOGIN.
     */
    private void handleLogin(SocketRequest socketRequest, ClientSession session) {
        LoginRequest loginRequest = gson.fromJson(socketRequest.getBody(), LoginRequest.class);

        LoginResultDTO result = authController.login(loginRequest);
        UserDTO user = result.getUser();

        session.setUserId(user.getId());
        session.setRole(user.getRole());

        ConnectionManage.getInstance().registerConnection(user.getId(), session);

        sendSuccess(session, socketRequest, "Đăng nhập thành công.", result);
    }

    /**
     * Xử lý REGISTER.
     */
    private void handleRegister(SocketRequest socketRequest, ClientSession session) {
        RegisterRequest registerRequest = gson.fromJson(socketRequest.getBody(), RegisterRequest.class);

        UserDTO user = authController.register(registerRequest);

        sendSuccess(session, socketRequest, "Đăng ký thành công.", user);
    }

    /**
     * Xử lý LOGOUT.
     */
    private void handleLogout(SocketRequest socketRequest, ClientSession session) {
        String userId = session.getUserId();

        if (userId == null) {
            sendFailure(session, socketRequest,
                    "User is not logged in on this session.",
                    "AUT_PERM_000");
            return;
        }

        authController.logout(userId);
        sendSuccess(session, socketRequest, "Đăng xuất thành công.", null);
        session.close();
    }

    /**
     * Routes item creation through the item controller so the client never calls service/DAO directly.
     */
    private void handleCreateItem(SocketRequest socketRequest, ClientSession session) {
        Object result = itemController.createItem(socketRequest.getBody(), session);
        sendSuccess(session, socketRequest, "Tao san pham thanh cong.", result);
    }

    /**
     * Routes item update and returns the refreshed detail DTO to the client.
     */
    private void handleUpdateItem(SocketRequest socketRequest, ClientSession session) {
        Object result = itemController.updateItem(socketRequest.getBody(), session);
        sendSuccess(session, socketRequest, "Cap nhat san pham thanh cong.", result);
    }

    /**
     * Routes item deletion as a status change instead of letting the client touch persistence.
     */
    private void handleDeleteItem(SocketRequest socketRequest, ClientSession session) {
        itemController.deleteItem(socketRequest.getBody(), session);
        sendSuccess(session, socketRequest, "Xoa san pham thanh cong.", null);
    }

    /**
     * Returns the item list owned by the current seller session.
     */
    private void handleGetSellerItems(SocketRequest socketRequest, ClientSession session) {
        Object result = itemController.getSellerItems(session);
        sendSuccess(session, socketRequest, "Lay danh sach san pham thanh cong.", result);
    }

    /**
     * Returns a detail DTO for the requested item.
     */
    private void handleGetItemDetail(SocketRequest socketRequest, ClientSession session) {
        Object result = itemController.getItemDetail(socketRequest.getBody(), session);
        sendSuccess(session, socketRequest, "Lay chi tiet san pham thanh cong.", result);
    }

    /**
     * Lấy danh sách các phiên đấu giá đang hoạt động.
     */
    private void handleGetActiveAuctions(SocketRequest socketRequest, ClientSession session) {
        Object result = auctionController.getActiveAuctions();
        sendSuccess(session, socketRequest, "Lấy danh sách phiên đấu giá thành công.", result);
    }

    /**
     * Lấy chi tiết một phiên đấu giá.
     */
    private void handleGetAuctionDetail(SocketRequest socketRequest, ClientSession session) {
        Object result = auctionController.getAuctionDetail(socketRequest.getBody());
        sendSuccess(session, socketRequest, "Lấy chi tiết phiên đấu giá thành công.", result);
    }

    /**
     * Tạo phiên đấu giá mới.
     */
    private void handleCreateAuction(SocketRequest socketRequest, ClientSession session) {
        auctionController.createAuction(socketRequest.getBody(), session);
        sendSuccess(session, socketRequest, "Tạo phiên đấu giá thành công.", null);
    }

    /**
     * Đặt giá vào một phiên đấu giá.
     */
    private void handlePlaceBid(SocketRequest socketRequest, ClientSession session) {
        auctionController.placeBid(socketRequest.getBody(), session);
        sendSuccess(session, socketRequest, "Đặt giá thành công.", null);
    }

    /**
     * 🔥 REFACTOR MỚI: Tiếp nhận yêu cầu cắm Socket mở màn hình trực tuyến (LIVE_ENTERED).
     * Trả về dữ liệu DTO thô của phòng để Client kịp vẽ UI ngay lập tức.
     */
    private void handleLiveEntered(SocketRequest socketRequest, ClientSession session) {
        Object result = auctionController.joinLiveRoom(socketRequest.getBody(), session);
        sendSuccess(session, socketRequest, "Kết nối phòng trực tuyến thành công.", result);
    }

    /**
     * 🔥 REFACTOR MỚI: Tiếp nhận yêu cầu rút Socket khi đóng màn hình trực tuyến (LIVE_EXITED).
     */
    private void handleLiveExited(SocketRequest socketRequest, ClientSession session) {
        auctionController.leaveLiveRoom(socketRequest.getBody(), session);
        sendSuccess(session, socketRequest, "Thoát phòng trực tuyến thành công.", null);
    }

    /**
     * 🔥 REFACTOR MỚI: Tiếp nhận nút bấm "Theo dõi" nền lưu DB/RAM vĩnh viễn (AUCTION_SUBSCRIBED).
     */
    private void handleAuctionSubscribed(SocketRequest socketRequest, ClientSession session) {
        auctionController.joinAuction(socketRequest.getBody(), session);
        sendSuccess(session, socketRequest, "Đăng ký theo dõi phiên đấu giá thành công.", null);
    }

    /**
     * 🔥 REFACTOR MỚI: Tiếp nhận nút bấm "Hủy theo dõi" dọn rác DB/RAM + Socket (AUCTION_UNSUBSCRIBED).
     */
    private void handleAuctionUnsubscribed(SocketRequest socketRequest, ClientSession session) {
        auctionController.leaveAuction(socketRequest.getBody(), session);
        sendSuccess(session, socketRequest, "Hủy theo dõi phiên đấu giá thành công.", null);
    }

    /**
     * Hủy một phiên đấu giá.
     */
    private void handleCancelAuction(SocketRequest socketRequest, ClientSession session) {
        auctionController.cancelAuction(socketRequest.getBody(), session);
        sendSuccess(session, socketRequest, "Hủy phiên đấu giá thành công.", null);
    }

    private void sendSuccess(ClientSession session, SocketRequest request, String message, Object body) {
        SocketResponse response = SocketResponse.success(
                request.getRequestId(),
                request.getAction(),
                message,
                body
        );

        session.sendMessage(gson.toJson(response));
    }

    private void sendFailure(ClientSession session, SocketRequest request, String message, String errorCode) {
        String requestId = request == null ? null : request.getRequestId();
        String action = request == null ? null : (request.getAction() == null ? "UNKNOWN" : request.getAction().toString());

        sendFailure(session, requestId, action, message, errorCode);
    }

    private void sendFailure(ClientSession session, String requestId, String action,
                             String message, String errorCode) {
        ActionType actionType = parseAction(action);

        if (actionType == null) {
            actionType = ActionType.LOGIN;
        }

        SocketResponse response = SocketResponse.failure(
                requestId,
                actionType,
                message,
                errorCode
        );

        session.sendMessage(gson.toJson(response));
    }

    private ActionType parseAction(String action) {
        if (action == null) return null;
        try {
            return ActionType.valueOf(action.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
