package com.auction.network;

import com.auction.controller.AuctionController;
import com.auction.controller.AuthController;
import com.auction.dto.LoginRequest;
import com.auction.dto.LoginResultDTO;
import com.auction.dto.LogoutRequest;
import com.auction.dto.RegisterRequest;
import com.auction.dto.SocketRequest;
import com.auction.dto.SocketResponse;
import com.auction.dto.UserDTO;
import com.auction.enums.ActionType;
import com.auction.exception.AuthenticationException;
import com.auction.manage.ConnectionManage;
import com.auction.service.AuthorizationService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import static com.auction.enums.ActionType.*;

/**
 * RequestDispatcher là bộ điều phối request phía Server.
 *
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
    private final AuthorizationService authorizationService = new AuthorizationService();

    /**
     * Điểm vào chính khi Server nhận được một dòng JSON từ Client.
     */
    public void processRequest(String requestJson, ClientSession session) {
        SocketRequest socketRequest = null;

        try {
            socketRequest = gson.fromJson(requestJson, SocketRequest.class);

            if (socketRequest == null || isBlank(socketRequest.getAction())) {
                sendFailure(session, null, null,
                        "Request không hợp lệ.",
                        "BAD_REQUEST");
                return;
            }

            ActionType actionType = parseAction(socketRequest.getAction());
            String action = socketRequest.getAction();


            if (!authorizationService.canAccess(action, session)) {
                sendFailure(session, socketRequest,
                        "Bạn không có quyền sử dụng chức năng này.",
                        "FORBIDDEN");
                return;
            }

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

                case SUBSCRIBE_AUCTION:
                    handleSubscribeAuction(socketRequest, session);
                    break;

                case UNSUBSCRIBE_AUCTION:
                    handleUnsubscribeAuction(socketRequest, session);
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

        } catch (Exception e) {
            sendFailure(session, socketRequest,
                    "Lỗi xử lý hệ thống.",
                    "SERVER_ERROR");
        }
    }

    /**
     * Xử lý LOGIN.
     *
     * Thành công:
     * - Gắn userId, role vào ClientSession.
     * - Đăng ký connection vào ConnectionManage.
     * - Trả SocketResponse.body = LoginResultDTO.
     *
     * Thất bại:
     * - Trả SocketResponse.failure.
     */
    private void handleLogin(SocketRequest socketRequest, ClientSession session) {
        try {
            LoginRequest loginRequest = gson.fromJson(socketRequest.getBody(), LoginRequest.class);

            LoginResultDTO result = authController.login(loginRequest);
            UserDTO user = result.getUser();

            session.setUserId(user.getId());
            session.setRole(user.getRole());

            ConnectionManage.getInstance().registerConnection(user.getId(), session);

            sendSuccess(session, socketRequest, "Đăng nhập thành công.", result);

        } catch (AuthenticationException e) {
            sendFailure(session, socketRequest, e.getMessage(), e.getErrorCode());

        } catch (Exception e) {
            sendFailure(session, socketRequest,
                    "Đăng nhập thất bại do lỗi hệ thống.",
                    "LOGIN_ERROR");
        }
    }

    /**
     * Xử lý REGISTER.
     *
     * Thành công:
     * - Trả SocketResponse.body = UserDTO.
     *
     * Thất bại:
     * - Trả SocketResponse.failure.
     */
    private void handleRegister(SocketRequest socketRequest, ClientSession session) {
        try {
            RegisterRequest registerRequest = gson.fromJson(socketRequest.getBody(), RegisterRequest.class);

            UserDTO user = authController.register(registerRequest);

            sendSuccess(session, socketRequest, "Đăng ký thành công.", user);

        } catch (AuthenticationException e) {
            sendFailure(session, socketRequest, e.getMessage(), e.getErrorCode());

        } catch (Exception e) {
            sendFailure(session, socketRequest,
                    "Đăng ký thất bại do lỗi hệ thống.",
                    "REGISTER_ERROR");
        }
    }

    /**
     * Xử lý LOGOUT.
     *
     * Server ưu tiên lấy userId từ ClientSession.
     * Không tin userId client gửi lên, vì client có thể giả mạo.
     */
    private void handleLogout(SocketRequest socketRequest, ClientSession session) {
        try {
            LogoutRequest logoutRequest = gson.fromJson(socketRequest.getBody(), LogoutRequest.class);

            String userId = session.getUserId();

            if (userId == null) {
                sendFailure(session, socketRequest,
                        "User is not logged in on this session.",
                        "USER_NOT_LOGGED_IN");
                return;
            }

            authController.logout(userId);

            ConnectionManage.getInstance().removeConnection(userId, session);
            session.clearLoginInfo();

            sendSuccess(session, socketRequest, "Đăng xuất thành công.", null);

        } catch (AuthenticationException e) {
            sendFailure(session, socketRequest, e.getMessage(), e.getErrorCode());

        } catch (Exception e) {
            sendFailure(session, socketRequest,
                    "Đăng xuất thất bại do lỗi hệ thống.",
                    "LOGOUT_ERROR");
        }
    }

    /**
     * Lấy danh sách các phiên đấu giá đang hoạt động.
     */
    private void handleGetActiveAuctions(SocketRequest socketRequest, ClientSession session) {
        try {
            Object result = auctionController.getActiveAuctions();
            sendSuccess(session, socketRequest, "Lấy danh sách phiên đấu giá thành công.", result);

        } catch (Exception e) {
            sendFailure(session, socketRequest,
                    "Không thể lấy danh sách phiên đấu giá.",
                    "GET_ACTIVE_AUCTIONS_ERROR");
        }
    }

    /**
     * Lấy chi tiết một phiên đấu giá.
     */
    private void handleGetAuctionDetail(SocketRequest socketRequest, ClientSession session) {
        try {
            Object result = auctionController.getAuctionDetail(socketRequest.getBody());
            sendSuccess(session, socketRequest, "Lấy chi tiết phiên đấu giá thành công.", result);

        } catch (Exception e) {
            sendFailure(session, socketRequest,
                    "Không thể lấy chi tiết phiên đấu giá.",
                    "GET_AUCTION_DETAIL_ERROR");
        }
    }

    /**
     * Tạo phiên đấu giá mới.
     */
    private void handleCreateAuction(SocketRequest socketRequest, ClientSession session) {
        try {
            Object result = auctionController.createAuction(socketRequest.getBody(), session);
            sendSuccess(session, socketRequest, "Tạo phiên đấu giá thành công.", result);

        } catch (Exception e) {
            sendFailure(session, socketRequest,
                    "Không thể tạo phiên đấu giá.",
                    "CREATE_AUCTION_ERROR");
        }
    }

    /**
     * Đặt giá vào một phiên đấu giá.
     */
    private void handlePlaceBid(SocketRequest socketRequest, ClientSession session) {
        try {
            Object result = auctionController.placeBid(socketRequest.getBody(), session);
            sendSuccess(session, socketRequest, "Đặt giá thành công.", result);

        } catch (Exception e) {
            sendFailure(session, socketRequest,
                    "Không thể đặt giá.",
                    "PLACE_BID_ERROR");
        }
    }

    /**
     * Đăng ký nhận realtime update của một phiên đấu giá.
     */
    private void handleSubscribeAuction(SocketRequest socketRequest, ClientSession session) {
        try {
            Object result = auctionController.subscribeAuction(socketRequest.getBody(), session);
            sendSuccess(session, socketRequest, "Đăng ký theo dõi phiên đấu giá thành công.", result);

        } catch (Exception e) {
            sendFailure(session, socketRequest,
                    "Không thể đăng ký theo dõi phiên đấu giá.",
                    "SUBSCRIBE_AUCTION_ERROR");
        }
    }

    /**
     * Hủy đăng ký nhận realtime update của một phiên đấu giá.
     */
    private void handleUnsubscribeAuction(SocketRequest socketRequest, ClientSession session) {
        try {
            Object result = auctionController.unsubscribeAuction(socketRequest.getBody(), session);
            sendSuccess(session, socketRequest, "Hủy theo dõi phiên đấu giá thành công.", result);

        } catch (Exception e) {
            sendFailure(session, socketRequest,
                    "Không thể hủy theo dõi phiên đấu giá.",
                    "UNSUBSCRIBE_AUCTION_ERROR");
        }
    }

    /**
     * Hủy một phiên đấu giá.
     */
    private void handleCancelAuction(SocketRequest socketRequest, ClientSession session) {
        try {
            Object result = auctionController.cancelAuction(socketRequest.getBody(), session);
            sendSuccess(session, socketRequest, "Hủy phiên đấu giá thành công.", result);

        } catch (Exception e) {
            sendFailure(session, socketRequest,
                    "Không thể hủy phiên đấu giá.",
                    "CANCEL_AUCTION_ERROR");
        }
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
        String action = request == null ? null : request.getAction();

        sendFailure(session, requestId, action, message, errorCode);
    }

    private void sendFailure(ClientSession session, String requestId, String action,
                             String message, String errorCode) {
        SocketResponse response = SocketResponse.failure(
                requestId,
                action,
                message,
                errorCode
        );

        session.sendMessage(gson.toJson(response));
    }

    /**
     * Chuyển action string trong SocketRequest sang ActionType.
     * Nếu Client gửi action không nằm trong enum, trả null để dispatcher báo lỗi rõ ràng.
     */
    private ActionType parseAction(String action) {
        try {
            return ActionType.valueOf(action.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Không dùng String.isBlank() để tránh lỗi nếu IDE compile nhầm language level thấp.
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}