package com.auction.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auction.dao.UserDAO;
import com.auction.dao.LogDAO;
import com.auction.dao.impl.UserDAOImpl;
import com.auction.dao.impl.LogDAOImpl;
import com.auction.dto.*;
import com.auction.enums.ActionType;
import com.auction.enums.UserStatus;
import com.auction.exception.AuthErrorCode;
import com.auction.exception.AuthenticationException;
import com.auction.exception.ValidationErrorCode;
import com.auction.exception.ValidationException;
import com.auction.exception.WalletErrorCode;
import com.auction.exception.WalletException;
import com.auction.manage.ConnectionManage;
import com.auction.manage.UserManage;
import com.auction.models.User.*;
import com.auction.utils.GsonProvider;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserDAO userDAO = new UserDAOImpl(); // Đổi sang Interface UserDAO cho chuẩn Loose Coupling
    private final LogDAO logDAO = new LogDAOImpl();
    private final UserManage userManage = UserManage.getInstance();
    private final ConnectionManage connectionManage = ConnectionManage.getInstance();
    private static final com.google.gson.Gson gson = GsonProvider.getGson();
    private static final ScheduledExecutorService FORCE_LOGOUT_SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "force-logout-scheduler");
                thread.setDaemon(true);
                return thread;
            });

    // =========================================================================
    // 1. PHÂN HỆ NGHIỆP VỤ TÀI CHÍNH (ĐỒNG BỘ RAM & DATABASE TUYỆT ĐỐI)
    // =========================================================================

    /**
     * NẠP TIỀN VÀO TÀI KHOẢN
     */
    public void depositMoney(String bidderId, double amount) {
        if (bidderId == null || bidderId.trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.MISSING_REQUIRED_FIELD, "Target identification handle is required.");
        }
        if (amount <= 0) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Deposit volume transaction constraint must be positive.");
        }

        synchronized (bidderId.trim().intern()) {
            try (Connection conn = com.auction.config.DatabaseConnection.getConnection()) {

                boolean isSavedDB = userDAO.addAvailableBalance(conn, bidderId, amount);
                if (!isSavedDB) {
                    throw new WalletException(WalletErrorCode.TRANSACTION_FAILED, "Failed to inject balance amount statement into persistence layer.");
                }

            } catch (SQLException e) {
                throw new WalletException(WalletErrorCode.TRANSACTION_FAILED, "Database link failed at depositMoney: " + e.getMessage());
            }

            User ramUser = getOrLoadUser(bidderId);
            if (ramUser instanceof Bidder bidder) {
                bidder.setAvailableBalance(bidder.getAvailableBalance() + amount);
            }
        }
    }

    /**
     * RÚT TIỀN TỪ TÀI KHOẢN
     */
    public void withdrawMoney(String bidderId, double amount) {
        if (bidderId == null || bidderId.trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.MISSING_REQUIRED_FIELD, "Target identity handle is required.");
        }
        if (amount <= 0) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Withdrawal criteria amount must be positive.");
        }

        synchronized (bidderId.trim().intern()) {

            User ramUser = getOrLoadUser(bidderId);
            if (ramUser == null) {
                throw new AuthenticationException(AuthErrorCode.USER_NOT_FOUND);
            }

            if (ramUser instanceof Bidder bidder) {
                if (bidder.getAvailableBalance() < amount) {
                    throw new WalletException(WalletErrorCode.INSUFFICIENT_BALANCE);
                }

                if (bidder.getStatus() == UserStatus.BANNED) {
                    throw new WalletException(WalletErrorCode.TRANSACTION_FAILED, "Account is currently banned.");
                }
            }

            // 🔥 SỬA: Mở kết nối try-with-resources chủ động ở tầng Service và truyền conn vào DAO
            try (Connection conn = com.auction.config.DatabaseConnection.getConnection()) {

                boolean isDeductDB = userDAO.withdrawAvailableBalance(conn, bidderId, amount); // Truyền conn đã mở
                if (!isDeductDB) {
                    throw new WalletException(WalletErrorCode.TRANSACTION_FAILED, "Balance debit extraction logic rejected at atomic transaction scope.");
                }

            } catch (SQLException e) {
                throw new WalletException(WalletErrorCode.TRANSACTION_FAILED, "Database link failed at withdrawMoney: " + e.getMessage());
            }

            if (ramUser instanceof Bidder bidderLive) {
                bidderLive.setAvailableBalance(bidderLive.getAvailableBalance() - amount);
            }
        }
    }

    // =========================================================================
    // 2. PHÂN HỆ QUẢN LÝ CỦA ADMIN
    // =========================================================================

    /**
     * TÍNH NĂNG MÀN HÌNH QUẢN LÝ CỦA ADMIN
     */
    public PageDTO<UserDTO> getAdminUserDashboard(int page, int pageSize) {
        if (page <= 0 || pageSize <= 0) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Frame page metrics parameters must be positive.");
        }

        int offset = (page - 1) * pageSize;
        // Luồng đọc danh sách (SELECT) độc lập, giữ nguyên cấu trúc gọi an toàn
        List<User> dbUsers = userDAO.findPaginated(pageSize, offset);
        List<UserDTO> dtoDashboardList = new ArrayList<>();

        for (User user : dbUsers) {
            User ramUser = userManage.getUser(user.getId());
            UserDTO dto = getUserDTO(user, ramUser);

            if (dto != null) {
                dtoDashboardList.add(dto);
            }
        }
        // Đưa logic tính toán phân trang từ Controller về đây
        long totalUsers = userDAO.countTotalUsers();
        int totalPages = (int) Math.ceil((double) totalUsers / pageSize);

        return new PageDTO<>(dtoDashboardList, page, totalPages, totalUsers);
    }

    /**
     * LẤY THÔNG TIN PROFILE DƯỚI DẠNG DTO ĐA HÌNH
     */
    public UserDTO getUserProfile(String userId) {
        User user = getOrLoadUser(userId);
        if (user == null) {
            throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "User not found.");
        }
        User ramUser = userManage.getUser(userId);
        return getUserDTO(user, ramUser);
    }

    @Nullable
    private static UserDTO getUserDTO(User user, User ramUser) {
        UserStatus currentStatus = (ramUser != null) ? ramUser.getStatus() : user.getStatus();

        return switch (user) {
            case Bidder b -> new BidderDTO(
                    b.getId(), b.getUsername(), b.getEmail(), b.getUserRole(), currentStatus,
                    b.getAvailableBalance(), b.getFrozenBalance(), b.getJoinedAuctionIds()
            );
            case Seller s -> new SellerDTO(
                    s.getId(), s.getUsername(), s.getEmail(), s.getUserRole(), currentStatus,
                    s.getAvailableBalance(), s.getFrozenBalance(), s.getRating()
            );
            case Admin a -> new AdminDTO(
                    a.getId(), a.getUsername(), a.getEmail(), a.getUserRole(), currentStatus
            );
            default -> null;
        };
    }

    /**
     * TÍNH NĂNG KHÓA TÀI KHOẢN TỨC THÌ CỦA ADMIN (Đã tối ưu Polite Close)
     */
    public void lockUserAccount(String adminId, String userId, UserStatus targetStatus) {
        lockUserAccount(adminId, userId, targetStatus, null);
    }

    public void lockUserAccount(String adminId, String userId, UserStatus targetStatus, String reason) {
        if (userId == null || userId.trim().isEmpty() || targetStatus == null || adminId == null || adminId.trim().isEmpty()) {
            throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Required parameter constraints for locking operation are missing.");
        }

        if (targetStatus != UserStatus.BANNED) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Target state specification error. Restriction level must be BANNED.");
        }

        final String cleanUserId = userId.trim();
        final String cleanReason = normalizeAdminReason(reason);

        synchronized (cleanUserId.intern()) {
            // [ĐOẠN CODE TRANSACTION DATABASE - GIỮ NGUYÊN HOÀN TOÀN CỦA BẠN]
            Connection conn = null;
            try {
                conn = com.auction.config.DatabaseConnection.getConnection();
                conn.setAutoCommit(false);

                boolean isUpdatedDB = userDAO.updateStatus(conn, cleanUserId, targetStatus.name());
                if (!isUpdatedDB) {
                    throw new AuthenticationException(AuthErrorCode.USER_NOT_FOUND);
                }

                String logId = UUID.randomUUID().toString();
                String actionDetail = "Admin changed user account status to: " + targetStatus.name()
                        + ". Reason: " + cleanReason;
                logDAO.insertLog(conn, logId, adminId, actionDetail, "USER", cleanUserId);

                conn.commit();
                log.info("[DB Transaction] ✅ Khóa tài khoản và ghi Audit Log thành công.");

            } catch (Exception e) {
                if (conn != null) {
                    try { conn.rollback(); } catch (SQLException ex) { log.error("[DB Transaction] Rollback thất bại: {}", ex.getMessage(), ex); }
                }
                if (e instanceof com.auction.exception.BaseException) throw (com.auction.exception.BaseException) e;
                throw new AuthenticationException(AuthErrorCode.USER_NOT_FOUND);
            } finally {
                if (conn != null) {
                    try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { log.error("[DB Transaction] Đóng kết nối thất bại: {}", ex.getMessage(), ex); }
                }
            }

            // 3. Đồng bộ hóa lên đối tượng trạng thái RAM live sau khi Transaction DB hạ cánh an toàn
            User ramUser = userManage.getUser(cleanUserId);
            if (ramUser != null) {
                ramUser.setStatus(targetStatus);
            }

            // =========================================================================
            // 🔥 TỐI ƯU THEO CÁCH 2: PHỐI HỢP CLIENT - SERVER (POLITE CLOSE)
            // =========================================================================
            if (connectionManage.isUserOnline(cleanUserId)) {

                // Bước 1: Gửi thông điệp cảnh báo cho Client biết.
                // Client nhận được chuỗi này phải hiện Dialog thông báo lập tức và tự đóng socket phía nó.
                connectionManage.sendMessageToUser(cleanUserId,
                        buildForceLogoutMessage(cleanUserId, cleanReason, targetStatus));

                // Bước 2: Tạo một luồng chạy ẩn, hoãn lại 300ms để bọc hậu (Chốt chặn tối cao)
                // Việc hoãn ẩn này giúp hàm thoát ra ngay lập tức, Admin không bị treo màn hình đợi.
                FORCE_LOGOUT_SCHEDULER.schedule(() -> {
                    try {
                        // Nếu sau 300ms mà Client vẫn chưa tự ngắt kết nối (hoặc cố tình lỳ ra)
                        if (connectionManage.isUserOnline(cleanUserId)) {
                            log.warn("[Security Guard] 🚨 Client không tự đóng, tiến hành cưỡng chế rút phích cắm: {}", cleanUserId);
                            connectionManage.forceDisconnectUser(cleanUserId);
                        }

                        // Dọn dẹp dứt điểm bộ nhớ RAM Cache của User này
                        userManage.deleteUser(cleanUserId);
                        log.info("[Security Guard] 🎯 Đã dọn dẹp sạch sẽ session và bộ nhớ của user bị ban: {}", cleanUserId);

                    } catch (Exception e) {
                        log.error("[Security Guard] Lỗi khi thực thi bọc hậu ngắt socket: {}", e.getMessage(), e);
                    }
                }, 300, TimeUnit.MILLISECONDS); // 300ms là quá đủ cho gói tin TCP truyền đi thành công
            }
        }
    }

    private String normalizeAdminReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            return "No reason provided.";
        }
        return reason.trim();
    }

    private String buildForceLogoutMessage(String userId, String reason, UserStatus targetStatus) {
        Map<String, Object> payload = Map.of(
                "userId", userId,
                "newStatus", targetStatus.name(),
                "reason", reason
        );
        SocketResponse response = SocketResponse.event(
                ActionType.FORCE_LOGOUT,
                "Your account has been locked by an administrator.",
                payload
        );
        return gson.toJson(response);
    }

    /**
     * Quản lý cơ chế nạp bộ đệm (Cache-Aside Pattern) cho User
     */
    private User getOrLoadUser(String userId) {
        User user = userManage.getUser(userId);
        if (user == null) {
            user = userDAO.findById(userId).orElse(null);
            if (user != null && user.getStatus() != UserStatus.BANNED) {
                userManage.addUser(user);
            }
        }
        return user;
    }
}
