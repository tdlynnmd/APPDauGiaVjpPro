package com.auction.controller;

import com.auction.dto.*;
import com.auction.exception.ValidationErrorCode;
import com.auction.exception.ValidationException;
import com.auction.network.ClientSession;
import com.auction.service.BidTransactionService;
import com.auction.service.UserService;

/**
 * =========================================================================
 * UserController - Bộ điều phối quản lý người dùng phía Server (Đã tối ưu)
 * =========================================================================
 * Vai trò:
 * - Tiếp nhận DTO sạch và định danh an toàn từ tầng mạng (RequestDispatcher).
 * - Kiểm tra tính hợp lệ cơ bản của dữ liệu đầu vào.
 * - Điều phối xử lý xuống tầng UserService và trả về DTO nguyên bản.
 */
public class UserController {
    private final UserService userService = new UserService(); // Chỉ tương tác duy nhất với Service
    private final BidTransactionService bidTransactionService = new BidTransactionService(); // Dành cho các chức năng liên quan đến giao dịch đấu giá
    /**
     * Lấy thông tin profile của người dùng hiện tại
     */
    public UserDTO getUserProfile(String userId) {
        return userService.getUserProfile(userId);
    }

    /**
     * Nạp tiền vào tài khoản
     */
    public UserDTO depositMoney(String userId, DepositRequest request) {
        if (request == null) {
            throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Yêu cầu không hợp lệ.");
        }
        if (request.getAmount() <= 0) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Deposit amount must be greater than 0.");
        }

        userService.depositMoney(userId, request.getAmount());
        return userService.getUserProfile(userId);
    }

    /**
     * Rút tiền từ tài khoản
     */
    public UserDTO withdrawMoney(String userId, WithdrawRequest request) {
        if (request == null) {
            throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Yêu cầu không hợp lệ.");
        }
        if (request.getAmount() <= 0) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Withdrawal amount must be greater than 0.");
        }

        userService.withdrawMoney(userId, request.getAmount());
        return userService.getUserProfile(userId);
    }

    /**
     * Tải danh sách lịch sử đi đấu giá cá nhân của chính người dùng hiện tại.
     * Action tương ứng: GET_MY_BID_HISTORY
     * Bảo mật: Truyền bidderId sạch đã bốc từ ClientSession ở RequestDispatcher
     */
    public PageDTO<BidTransactionDTO> getMyBidHistory(String bidderId, GetBidderHistoryRequest request) {
        // 1. Kiểm tra tham số phân trang cơ bản
        if (request.getPage() <= 0 || request.getPageSize() <= 0) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Page and pageSize must be positive.");
        }

        // 2. Điều phối xuống Service chuyên trách đọc lịch sử cá nhân
        return bidTransactionService.getBidderHistoryPaged(
                bidderId,
                request.getPage(),
                request.getPageSize()
        );
    }

    /**
     * Cập nhật thông tin Profile cá nhân (username, email)
     */
    public UserDTO updateProfile(String userId, UpdateProfileRequest request) {
        if (request == null) {
            throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Yêu cầu không hợp lệ.");
        }
        return userService.updateProfile(userId, request);
    }

    /**
     * Cập nhật mật khẩu an toàn và ngắt kết nối các thiết bị khác
     */
    public void updatePassword(String userId, UpdatePasswordRequest request, ClientSession currentSession) {
        if (request == null) {
            throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Yêu cầu không hợp lệ.");
        }
        userService.updatePassword(userId, request, currentSession);
    }
}