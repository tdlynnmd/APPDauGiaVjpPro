package com.auction.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auction.controller.*;
import com.auction.dto.*;
import com.auction.enums.ActionType;
import com.auction.exception.BaseException;
import com.auction.manage.ConnectionManage;
import com.auction.service.AuthorizationService;
import com.auction.utils.GsonProvider;
import com.google.gson.JsonSyntaxException;

import java.util.List;

/**
 * =========================================================================
 * RequestDispatcher - Trung tâm điều phối và biên dịch gói tin phía Server
 * =========================================================================
 */
public class RequestDispatcher {
    private static final Logger log = LoggerFactory.getLogger(RequestDispatcher.class);
    private static volatile RequestDispatcher instance;
    private final com.google.gson.Gson gson = GsonProvider.getGson();

    private final AuthController authController = new AuthController();
    private final AuctionController auctionController = new AuctionController();
    private final ItemController itemController = new ItemController();
    private final UserController userController = new UserController();
    private final AdminController adminController = new AdminController();
    private final AuthorizationService authorizationService = new AuthorizationService();

    private RequestDispatcher() {
        log.info("[RequestDispatcher] 🚀 Khởi tạo trung tâm điều phối hệ thống thành công.");
    }

    public static RequestDispatcher getInstance() {
        RequestDispatcher temp = instance;
        if (temp == null) {
            synchronized (RequestDispatcher.class) {
                temp = instance;
                if (temp == null) {
                    temp = instance = new RequestDispatcher();
                }
            }
        }
        return temp;
    }

    /**
     * Điểm vào chính xử lý chuỗi JSON thô nhận được từ Socket Client.
     */
    public void processRequest(String requestJson, ClientSession session) {
        session.getSessionExecutor().submit(() -> {
            SocketRequest socketRequest = null;
            try {
                socketRequest = gson.fromJson(requestJson, SocketRequest.class);

                if (socketRequest == null || isBlank(socketRequest.getAction().toString())) {
                    sendFailure(session, null, null, "Request không hợp lệ.", "BAD_REQUEST");
                    return;
                }

                ActionType actionType = parseAction(socketRequest.getAction().toString());
                String action = socketRequest.getAction().toString();

                if (actionType == null) {
                    sendFailure(session, socketRequest, "Action không được hỗ trợ.", "UNSUPPORTED_ACTION");
                    return;
                }

                // 🔥 RATE LIMITER: Chặn sớm nếu Client gửi quá nhiều request trong 1 giây
                // PING được miễn rate limit để đảm bảo keep-alive luôn hoạt động
                if (actionType != ActionType.PING && session.isRateLimited()) {
                    sendFailure(session, socketRequest, "Bạn đang gửi quá nhiều yêu cầu. Vui lòng chờ.", "TOO_MANY_REQUESTS");
                    return;
                }

                // Kiểm tra phân quyền dựa trên Action chuỗi và Session
                authorizationService.canAccess(action, session);

                switch (actionType) {
                    // 🔥 PING KEEP-ALIVE: Trả PONG ngay lập tức, không cần xử lý nghiệp vụ
                    case PING -> sendSuccess(session, socketRequest, "PONG", null);

                    case LOGIN -> handleLogin(socketRequest, session);
                    case REGISTER -> handleRegister(socketRequest, session);
                    case LOGOUT -> handleLogout(socketRequest, session);

                    // PHÂN HỆ VẬT PHẨM (End-user)
                    case CREATE_ITEM -> handleCreateItem(socketRequest, session);
                    case UPDATE_ITEM -> handleUpdateItem(socketRequest, session);
                    case GET_SELLER_ITEMS -> handleGetSellerItems(socketRequest, session);
                    case GET_ITEM_DETAIL -> handleGetItemDetail(socketRequest, session);
                    case SELLER_DELETE_ITEM -> handleDeleteItem(socketRequest, session);

                    // PHÂN HỆ ĐẤU GIÁ LIVE (End-user)
                    case GET_ACTIVE_AUCTIONS -> handleGetActiveAuctions(socketRequest, session);
                    case GET_AUCTION_DETAIL -> handleGetAuctionDetail(socketRequest, session);
                    case CREATE_AUCTION -> handleCreateAuction(socketRequest, session);
                    case PLACE_BID -> handlePlaceBid(socketRequest, session);
                    case SETUP_AUTO_BID -> handleSetupAutoBid(socketRequest, session);
                    case CANCEL_AUTO_BID -> handleCancelAutoBid(socketRequest, session);
                    case LIVE_ENTERED -> handleLiveEntered(socketRequest, session);
                    case LIVE_EXITED -> handleLiveExited(socketRequest, session);
                    case AUCTION_SUBSCRIBED -> handleAuctionSubscribed(socketRequest, session);
                    case AUCTION_UNSUBSCRIBED -> handleAuctionUnsubscribed(socketRequest, session);
                    case SELLER_CANCEL_AUCTION -> handleCancelAuction(socketRequest, session);
                    case GET_SELLER_AUCTIONS -> handleGetSellerAuctions(socketRequest, session);
                    case UPDATE_AUCTION -> handleUpdateAuction(socketRequest, session);

                    // ================================================================
                    // 🛡️ PHÂN HỆ NGHIỆP VỤ USER (UserController)
                    // ================================================================
                    case GET_USER_PROFILE -> handleGetUserProfile(socketRequest, session);
                    case UPDATE_PROFILE -> handleUpdateProfile(socketRequest, session);
                    case UPDATE_PASSWORD -> handleUpdatePassword(socketRequest, session);
                    case DEPOSIT_MONEY -> handleDepositMoney(socketRequest, session);
                    case WITHDRAW_MONEY -> handleWithdrawMoney(socketRequest, session);

                    // ================================================================
                    // 🛠️ PHÂN HỆ QUẢN TRỊ CAO CẤP (AdminController)
                    // ================================================================
                    case CMD_ADMIN_GET_USERS -> handleAdminGetUsers(socketRequest, session);
                    case CMD_ADMIN_LOCK_USER -> handleAdminLockUser(socketRequest, session);
                    case CMD_ADMIN_CANCEL_AUCTION -> handleAdminCancelAuction(socketRequest, session);
                    case CMD_ADMIN_DELETE_ITEM -> handleAdminDeleteItem(socketRequest, session);
                    case CMD_ADMIN_GET_LOGS -> handleAdminGetLogs(socketRequest, session);

                    // Nhánh xử lý lịch sử phòng đấu giá (Ai cũng xem được)
                    case GET_AUCTION_BID_HISTORY -> handleGetAuctionBidHistory(socketRequest, session);

                    // Nhánh xử lý lịch sử cá nhân (Bảo mật - Rút bidderId từ session ra truyền vào)
                    case GET_MY_BID_HISTORY -> handleGetMyBidHistory(socketRequest, session);

                    default -> sendFailure(session, socketRequest, "Action không được hỗ trợ.", "UNSUPPORTED_ACTION");
                }

            } catch (JsonSyntaxException e) {
                sendFailure(session, socketRequest, "JSON request không hợp lệ.", "INVALID_JSON");
            } catch (BaseException e) {
                log.warn("[Central Guard] 🚨 Lỗi nghiệp vụ xảy ra lúc [{}] | Code: {} | Chi tiết: {}", 
                        e.getTimestamp(), e.getErrorCode(), e.getMessage());
                sendFailure(session, socketRequest, e.getMessage(), e.getErrorCode());
            } catch (Exception e) {
                log.error("[Fatal System Error] 💥 Sự cố hệ thống nghiêm trọng:", e);
                sendFailure(session, socketRequest, "Lỗi xử lý hệ thống.", "SERVER_ERROR");
            }
        });
    }

    private void handleGetMyBidHistory(SocketRequest socketRequest, ClientSession session){
        String bidderId = session.getUserId(); // An toàn tuyệt đối
        GetBidderHistoryRequest request = gson.fromJson(socketRequest.getBody(), GetBidderHistoryRequest.class);
        PageDTO<BidTransactionDTO> result = userController.getMyBidHistory(bidderId, request);
        sendSuccess(session, socketRequest, "Tải lịch sử đấu giá cá nhân thành công.", result);
    }

    private void handleGetAuctionBidHistory(SocketRequest socketRequest, ClientSession session){
        GetAuctionBidsRequest request = gson.fromJson(socketRequest.getBody(), GetAuctionBidsRequest.class);
        PageDTO<BidTransactionDTO> result = auctionController.getAuctionBidHistory(request);
        sendSuccess(session, socketRequest, "Tải lịch sử đặt giá thành công.", result);
    }



    // =========================================================================
    // 🛡️ HANDLERS CHO USERCONTROLLER (Đã đồng bộ tham số sạch)
    // =========================================================================

    private void handleGetUserProfile(SocketRequest socketRequest, ClientSession session) {
        String userId = session.getUserId(); // Bốc userId an toàn tại tầng mạng

        UserDTO profile = userController.getUserProfile(userId);
        sendSuccess(session, socketRequest, "Lấy thông tin profile thành công.", profile);
    }

    private void handleUpdateProfile(SocketRequest socketRequest, ClientSession session) {
        String userId = session.getUserId();
        UpdateProfileRequest request = gson.fromJson(socketRequest.getBody(), UpdateProfileRequest.class);
        UserDTO updatedUser = userController.updateProfile(userId, request);
        sendSuccess(session, socketRequest, "Cập nhật thông tin cá nhân thành công.", updatedUser);
    }

    private void handleUpdatePassword(SocketRequest socketRequest, ClientSession session) {
        String userId = session.getUserId();
        UpdatePasswordRequest request = gson.fromJson(socketRequest.getBody(), UpdatePasswordRequest.class);
        userController.updatePassword(userId, request, session);
        sendSuccess(session, socketRequest, "Thay đổi mật khẩu thành công. Các thiết bị khác đã bị đăng xuất.", null);
    }

    private void handleDepositMoney(SocketRequest socketRequest, ClientSession session) {
        String userId = session.getUserId();
        // Tầng mạng lo trọn gói việc dịch gói tin JSON sang DTO
        DepositRequest request = gson.fromJson(socketRequest.getBody(), DepositRequest.class);

        UserDTO updatedProfile = userController.depositMoney(userId, request);
        sendSuccess(session, socketRequest, "Nạp tiền vào tài khoản thành công.", updatedProfile);
    }

    private void handleWithdrawMoney(SocketRequest socketRequest, ClientSession session) {
        String userId = session.getUserId();
        WithdrawRequest request = gson.fromJson(socketRequest.getBody(), WithdrawRequest.class);

        UserDTO updatedProfile = userController.withdrawMoney(userId, request);
        sendSuccess(session, socketRequest, "Rút tiền từ tài khoản thành công.", updatedProfile);
    }

    //AdminController

    private void handleAdminGetUsers(SocketRequest socketRequest, ClientSession session) {
        GetUserDashboardRequest request = gson.fromJson(socketRequest.getBody(), GetUserDashboardRequest.class);
        PageDTO<UserDTO> dashboard = adminController.getUsersDashboard(request);
        sendSuccess(session, socketRequest, "Tải danh sách người dùng thành công.", dashboard);
    }

    private void handleAdminLockUser(SocketRequest socketRequest, ClientSession session) {
        String adminId = session.getUserId();
        LockUserAccountRequest request = gson.fromJson(socketRequest.getBody(), LockUserAccountRequest.class);
        adminController.lockUserAccount(adminId, request);
        sendSuccess(session, socketRequest, "Cưỡng chế khóa tài khoản người dùng thành công.", null);
    }

    private void handleAdminCancelAuction(SocketRequest socketRequest, ClientSession session) {
        String adminId = session.getUserId();
        CancelAuctionRequest request = gson.fromJson(socketRequest.getBody(), CancelAuctionRequest.class);
        adminController.cancelAuction(adminId, request);
        sendSuccess(session, socketRequest, "Cưỡng chế hủy phiên đấu giá thành công.", null);
    }

    private void handleAdminDeleteItem(SocketRequest socketRequest, ClientSession session) {
        String adminId = session.getUserId();
        DeleteItemRequest request = gson.fromJson(socketRequest.getBody(), DeleteItemRequest.class);
        adminController.deleteItem(adminId, request);
        sendSuccess(session, socketRequest, "Gỡ bỏ vật phẩm vi phạm thành công.", null);
    }

    private void handleAdminGetLogs(SocketRequest socketRequest, ClientSession session) {
        GetAuditLogsRequest request = gson.fromJson(socketRequest.getBody(), GetAuditLogsRequest.class);
        PageDTO<ActionLogDTO> logs = adminController.getAuditLogs(request);
        sendSuccess(session, socketRequest, "Tải danh sách nhật ký kiểm toán hệ thống thành công.", logs);
    }

    //AuthController
    private void handleLogin(SocketRequest socketRequest, ClientSession session) {
        LoginRequest loginRequest = gson.fromJson(socketRequest.getBody(), LoginRequest.class);
        LoginResultDTO result = authController.login(loginRequest);
        UserDTO user = result.getUser();
        session.setUserId(user.getId());
        session.setRole(user.getRole());
        session.setUsername(user.getUsername());
        ConnectionManage.getInstance().registerConnection(user.getId(), session);
        sendSuccess(session, socketRequest, "Đăng nhập thành công.", result);
    }

    private void handleRegister(SocketRequest socketRequest, ClientSession session) {
        RegisterRequest registerRequest = gson.fromJson(socketRequest.getBody(), RegisterRequest.class);
        UserDTO user = authController.register(registerRequest);
        sendSuccess(session, socketRequest, "Đăng ký thành công.", user);
    }

    private void handleLogout(SocketRequest socketRequest, ClientSession session) {
        String userId = session.getUserId();
        if (userId == null) {
            sendFailure(session, socketRequest, "User is not logged in on this session.", "AUT_PERM_000");
            return;
        }
        authController.logout(userId);
        sendSuccess(session, socketRequest, "Đăng xuất thành công.", null);
        session.close();
    }

    //ItemController

    /**
     * Routes item deletion as a status change instead of letting the client touch persistence.
     */
    private void handleDeleteItem(SocketRequest socketRequest, ClientSession session) {
        itemController.deleteItem(socketRequest.getBody(), session);
        sendSuccess(session, socketRequest, "Xoa san pham thanh cong.", null);
    }

    private void handleCreateItem(SocketRequest socketRequest, ClientSession session) {
        Object result = itemController.createItem(socketRequest.getBody(), session);
        sendSuccess(session, socketRequest, "Tao san pham thanh cong.", result);
    }

    private void handleUpdateItem(SocketRequest socketRequest, ClientSession session) {
        Object result = itemController.updateItem(socketRequest.getBody(), session);
        sendSuccess(session, socketRequest, "Cap nhat san pham thanh cong.", result);
    }

    private void handleGetSellerItems(SocketRequest socketRequest, ClientSession session) {
        Object result = itemController.getSellerItems(session);
        sendSuccess(session, socketRequest, "Lay danh sach san pham thanh cong.", result);
    }

    private void handleGetItemDetail(SocketRequest socketRequest, ClientSession session) {
        Object result = itemController.getItemDetail(socketRequest.getBody(), session);
        sendSuccess(session, socketRequest, "Lay chi tiet san pham thanh cong.", result);
    }

    //AuctionController
    private void handleGetActiveAuctions(SocketRequest socketRequest, ClientSession session) {
        List<AuctionSummaryDTO> result = auctionController.getActiveAuctions();
        sendSuccess(session, socketRequest, "Lấy danh sách thành công.", result);
    }

    private void handleGetAuctionDetail(SocketRequest socketRequest, ClientSession session) {
        GetAuctionDetailRequest request = gson.fromJson(socketRequest.getBody(), GetAuctionDetailRequest.class);
        AuctionDetailDTO result = auctionController.getAuctionDetail(request);
        sendSuccess(session, socketRequest, "Lấy chi tiết thành công.", result);
    }

    private void handleCreateAuction(SocketRequest socketRequest, ClientSession session) {
        String sellerId = session.getUserId();
        CreateAuctionRequest request = gson.fromJson(socketRequest.getBody(), CreateAuctionRequest.class);
        auctionController.createAuction(sellerId, request);
        sendSuccess(session, socketRequest, "Tạo phiên đấu giá thành công.", null);
    }

    private void handlePlaceBid(SocketRequest socketRequest, ClientSession session) {
        String bidderId = session.getUserId();
        PlaceBidRequest request = gson.fromJson(socketRequest.getBody(), PlaceBidRequest.class);
        auctionController.placeBid(bidderId, request);
        sendSuccess(session, socketRequest, "Đặt giá thành công.", null);
    }

    private void handleSetupAutoBid(SocketRequest socketRequest, ClientSession session) {
        String bidderId = session.getUserId();
        SetupAutoBidRequest request = gson.fromJson(socketRequest.getBody(), SetupAutoBidRequest.class);
        auctionController.setupAutoBid(bidderId, request);
        sendSuccess(session, socketRequest, "Thiết lập tự động đấu giá thành công.", null);
    }

    private void handleCancelAutoBid(SocketRequest socketRequest, ClientSession session) {
        String bidderId = session.getUserId();
        CancelAutoBidRequest request = gson.fromJson(socketRequest.getBody(), CancelAutoBidRequest.class);
        auctionController.cancelAutoBid(bidderId, request);
        sendSuccess(session, socketRequest, "Hủy tự động đấu giá thành công.", null);
    }

    private void handleLiveEntered(SocketRequest socketRequest, ClientSession session) {
        String bidderId = session.getUserId();
        AuctionSubscriptionRequest request = gson.fromJson(socketRequest.getBody(), AuctionSubscriptionRequest.class);
        AuctionDetailDTO result = auctionController.joinLiveRoom(bidderId, request, session);
        sendSuccess(session, socketRequest, "Vào phòng live thành công.", result);
    }

    private void handleLiveExited(SocketRequest socketRequest, ClientSession session) {
        String bidderId = session.getUserId();
        AuctionSubscriptionRequest request = gson.fromJson(socketRequest.getBody(), AuctionSubscriptionRequest.class);
        auctionController.leaveLiveRoom(bidderId, request, session);
        sendSuccess(session, socketRequest, "Rời phòng live thành công.", null);
    }

    private void handleAuctionSubscribed(SocketRequest socketRequest, ClientSession session) {
        String bidderId = session.getUserId();
        AuctionSubscriptionRequest request = gson.fromJson(socketRequest.getBody(), AuctionSubscriptionRequest.class);
        auctionController.joinAuction(bidderId, request);
        sendSuccess(session, socketRequest, "Theo dõi phiên thành công.", null);
    }

    private void handleAuctionUnsubscribed(SocketRequest socketRequest, ClientSession session) {
        String bidderId = session.getUserId();
        AuctionSubscriptionRequest request = gson.fromJson(socketRequest.getBody(), AuctionSubscriptionRequest.class);
        auctionController.leaveAuction(bidderId, request, session);
        sendSuccess(session, socketRequest, "Hủy theo dõi phiên thành công.", null);
    }

    private void handleCancelAuction(SocketRequest socketRequest, ClientSession session) {
        String sellerId = session.getUserId();
        CancelAuctionRequest request = gson.fromJson(socketRequest.getBody(), CancelAuctionRequest.class);
        auctionController.cancelAuctionBySeller(sellerId, request);
        sendSuccess(session, socketRequest, "Hủy phiên đấu giá thành công.", null);
    }

    private void handleGetSellerAuctions(SocketRequest socketRequest, ClientSession session) {
        String sellerId = session.getUserId();
        List<AuctionSummaryDTO> result = auctionController.getSellerAuctions(sellerId);
        sendSuccess(session, socketRequest, "Lấy danh sách phiên đấu giá thành công.", result);
    }

    private void handleUpdateAuction(SocketRequest socketRequest, ClientSession session) {
        String sellerId = session.getUserId();
        UpdateAuctionRequest request = gson.fromJson(socketRequest.getBody(), UpdateAuctionRequest.class);
        auctionController.updateAuction(sellerId, request);
        sendSuccess(session, socketRequest, "Cập nhật phiên đấu giá thành công.", null);
    }

    // =========================================================================
    // UTILS METHODS
    // =========================================================================
    private void sendSuccess(ClientSession session, SocketRequest request, String message, Object body) {
        SocketResponse response = SocketResponse.success(request.getRequestId(), request.getAction(), message, body);
        session.sendMessage(gson.toJson(response));
    }

    private void sendFailure(ClientSession session, SocketRequest request, String message, String errorCode) {
        String requestId = request == null ? null : request.getRequestId();
        String action = request == null ? null : (request.getAction() == null ? "UNKNOWN" : request.getAction().toString());
        sendFailure(session, requestId, action, message, errorCode);
    }

    private void sendFailure(ClientSession session, String requestId, String action, String message, String errorCode) {
        ActionType actionType = parseAction(action);
        if (actionType == null) actionType = ActionType.LOGIN;
        SocketResponse response = SocketResponse.failure(requestId, actionType, message, errorCode);
        session.sendMessage(gson.toJson(response));
    }

    private ActionType parseAction(String action) {
        if (action == null) return null;
        try { return ActionType.valueOf(action.trim()); } catch (IllegalArgumentException e) { return null; }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}