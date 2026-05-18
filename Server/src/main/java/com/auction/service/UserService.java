package com.auction.service;

import com.auction.dao.LogDAO;
import com.auction.dao.impl.UserDAOImpl;
import com.auction.dao.impl.LogDAOImpl;
import com.auction.dto.*;
import com.auction.enums.UserStatus;
import com.auction.exception.AuthenticationException;
import com.auction.manage.ConnectionManage;
import com.auction.manage.UserManage;
import com.auction.models.User.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserService {
    // Sử dụng trực tiếp triển khai cụ thể để gọi tính năng phân trang nâng cao
    private final UserDAOImpl userDAO = new UserDAOImpl();
    private final LogDAO logDAO = new LogDAOImpl();
    private final UserManage userManage = UserManage.getInstance();
    private final ConnectionManage connectionManage = ConnectionManage.getInstance();

    // =========================================================================
    // 1. PHÂN HỆ NGHIỆP VỤ TÀI CHÍNH (ĐỒNG BỘ RAM & DATABASE)
    // =========================================================================

    /**
     * NẠP TIỀN VÀO TÀI KHOẢN (Chỉ áp dụng cho Ví của Bidder)
     * Luồng: Cộng Database (Available) -> Tìm RAM Live -> Tăng số dư khả dụng trên RAM
     */
    public boolean depositMoney(String bidderId, double amount) {
        if (bidderId == null || amount <= 0) {
            System.err.println("❌ Lỗi: Tham số nạp tiền không hợp lệ.");
            return false;
        }

        // Bước 1: Ghi nhận tăng số dư khả dụng xuống Database thông qua DAO
        boolean isSavedDB = userDAO.addAvailableBalance(bidderId, amount);

        if (isSavedDB) {
            // Bước 2: Đồng bộ lập tức lên thực thể Live trên RAM (Nếu user đang online)
            User ramUser = userManage.getUser(bidderId);
            if (ramUser instanceof Bidder bidder) {
                // BẢO VỆ ĐA LUỒNG: Khóa đối tượng để tránh xung đột khi vừa bid vừa nạp tiền
                synchronized (bidder) {
                    bidder.setAvailableBalance(bidder.getAvailableBalance() + amount);
                }
            }
            System.out.println("✅ [Wallet] Nạp tiền thành công +" + amount + " cho Bidder ID: " + bidderId);
            return true;
        }

        System.err.println("❌ Lỗi: Không thể ghi nhận giao dịch nạp tiền vào cơ sở dữ liệu.");
        return false;
    }

    /**
     * RÚT TIỀN TỪ TÀI KHOẢN (Chỉ áp dụng cho Ví của Bidder)
     * Luồng: Kiểm tra số dư khả dụng trên RAM -> Trừ Database -> Cập nhật trừ số dư trên RAM
     */
    public boolean withdrawMoney(String bidderId, double amount) {
        if (bidderId == null || amount <= 0) return false;

        // Bước 1: Kiểm tra nhanh trên bộ nhớ RAM xem thực thể Live có đủ tiền rút không
        User ramUser = userManage.getUser(bidderId);
        if (ramUser instanceof Bidder bidder) {
            if (bidder.getAvailableBalance() < amount) {
                System.err.println("❌ Thất bại: Số dư khả dụng trên RAM không đủ để thực hiện lệnh rút.");
                return false;
            }
        }

        // Bước 2: Thực thi lệnh trừ tiền an toàn với điều kiện Atomic Update tại Database
        boolean isDeductDB = userDAO.withdrawAvailableBalance(bidderId, amount);

        if (isDeductDB) {
            // Bước 3: Đồng bộ ngay lập tức trừ tiền trên đối tượng RAM Live để hiển thị giao diện mới
            if (ramUser instanceof Bidder bidderLive) {
                synchronized (bidderLive) {
                    bidderLive.setAvailableBalance(bidderLive.getAvailableBalance() - amount);
                }
            }
            System.out.println("✅ [Wallet] Rút tiền thành công -" + amount + " từ ví Bidder ID: " + bidderId);
            return true;
        }

        System.err.println("❌ Thất bại: Lỗi hệ thống hoặc số dư khả dụng tại Database không đủ để trừ.");
        return false;
    }

    // =========================================================================
    // 2. PHÂN HỆ QUẢN LÝ CỦA ADMIN (PHÂN TRANG & KHÓA TÀI KHOẢN COERCION)
    // =========================================================================

    /**
     * TÍNH NĂNG MÀN HÌNH QUẢN LÝ CỦA ADMIN (TỐI ƯU CHỐNG QUÁ TẢI)
     * Trả về danh sách chuẩn List<UserDTO> tận dụng tính Đa hình (Polymorphism) đổ lên TableView JavaFX
     *
     * @param page Số trang hiện tại (Bắt đầu từ trang 1)
     * @param pageSize Số lượng dòng hiển thị tối đa trên một trang (Ví dụ: 20, 50 dòng)
     */
    public List<UserDTO> getAdminUserDashboard(int page, int pageSize) {
        int offset = (page - 1) * pageSize;

        // Kéo phân khúc dữ liệu giới hạn từ DB lên thông qua hàm phân trang của DAO
        List<User> dbUsers = userDAO.findPaginated(pageSize, offset);
        List<UserDTO> dtoDashboardList = new ArrayList<>();

        for (User user : dbUsers) {
            // Kiểm tra trạng thái hoạt động thực tế (Ưu tiên RAM trước, DB sau)
            User ramUser = userManage.getUser(user.getId());
            UserStatus currentStatus = (ramUser != null) ? ramUser.getStatus() : user.getStatus();

            // Khởi tạo các DTO con theo đúng nguyên mẫu Constructor bạn đã khai báo
            UserDTO dto = switch (user) {
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

            if (dto != null) {
                dtoDashboardList.add(dto);
            }
        }
        return dtoDashboardList;
    }

    /**
     * TÍNH NĂNG KHÓA TÀI KHOẢN TỨC THÌ CỦA ADMIN
     * Luồng: Đổi Status DB -> Đổi Status RAM Cache -> Ngắt kết nối mạng Socket ngay lập tức
     *
     * @param userId ID người dùng cần xử lý
     * @param targetStatus Trạng thái mới (BANNED hoặc SUSPENDED)
     */
    public boolean lockUserAccount(String adminId, String userId, UserStatus targetStatus) throws AuthenticationException {
        if (userId == null || targetStatus == null || adminId == null) return false;

        if (targetStatus != UserStatus.BANNED) {
            System.err.println("❌ Thất bại: Trạng thái thiết lập khóa không hợp lệ.");
            return false;
        }

        // 1. Cập nhật trạng thái xuống DB
        boolean isUpdatedDB = userDAO.updateStatus(userId, targetStatus.name());

        if (isUpdatedDB) {
            // 2. Đồng bộ RAM Cache
            User ramUser = userManage.getUser(userId);
            if (ramUser != null) {
                synchronized (ramUser) {
                    ramUser.setStatus(targetStatus);
                }
            }

            // 3. Cưỡng chế ngắt mạng Socket real-time
            if (connectionManage.isUserOnline(userId)) {
                connectionManage.sendMessageToUser(userId, "FORCE_LOGOUT_REASON: ACCOUNT_LOCKED");
                connectionManage.forceDisconnectUser(userId);
                userManage.deleteUser(userId);
            }

            // 🔥 BƯỚC 4: GHI LOG AUDIT TRAIL XUỐNG DATABASE
            String logId = UUID.randomUUID().toString();
            String actionDetail = "Admin thay đổi trạng thái tài khoản người dùng sang: " + targetStatus.name();
            logDAO.insertLog(logId, adminId, actionDetail, "USER", userId);

            System.out.println("✅ [Admin] Đã khóa tài khoản và ghi nhật ký hệ thống thành công.");
            return true;
        }
        return false;
    }
}