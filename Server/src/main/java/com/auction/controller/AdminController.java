package com.auction.controller;

import com.auction.dto.*;
import com.auction.enums.UserRole;
import com.auction.exception.ValidationErrorCode;
import com.auction.exception.ValidationException;
import com.auction.manage.AuctionManage;
import com.auction.models.Auction.Auction;
import com.auction.service.UserService;
import com.auction.service.AuctionService;
import com.auction.service.ItemService;
import com.auction.service.LogService;

/**
 * AdminController - Bộ điều phối nhóm chức năng quản trị cấp cao (Đã chuẩn hóa)
 */
public class AdminController {

    private final UserService userService = new UserService();
    private final AuctionService auctionService = AuctionService.getInstance();
    private final ItemService itemService = new ItemService();
    private final LogService logService = new LogService();

    /**
     * Áp dụng Dependency Injection qua Constructor (Sẵn sàng cho cả Unit Test / Mocking)
     */
    public AdminController() {
    }

    /**
     * 1. Tải danh sách người dùng phân trang
     * CMD_ADMIN_GET_USERS
     */
    public PageDTO<UserDTO> getUsersDashboard(GetUserDashboardRequest request) {
        if (request == null) throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Yêu cầu không hợp lệ.");
        return userService.getAdminUserDashboard(request.getPage(), request.getPageSize());
    }

    /**
     * 2. Cưỡng chế khóa tài khoản người dùng vi phạm
     * CMD_ADMIN_LOCK_USER
     * @param adminId Bốc từ ClientSession bảo mật, không tin vào JSON Client gửi lên
     */
    public void lockUserAccount(String adminId, LockUserAccountRequest request) {
        if (request == null) throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Yêu cầu không hợp lệ.");
        userService.lockUserAccount(adminId, request.getUserId(), request.getTargetStatus(), request.getReason());
    }

    /**
     * 3. Cưỡng chế hủy phiên đấu giá bất hợp pháp
     * CMD_ADMIN_CANCEL_AUCTION
     * @param adminId Bốc từ ClientSession bảo mật
     */
    public void cancelAuction(String adminId, CancelAuctionRequest request) {
        if (request == null) throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Yêu cầu không hợp lệ.");
        // 🔥 ĐÃ SỬA: adminId truyền từ ngoài vào, DTO bên trong chỉ chứa thông tin phòng đấu giá và lý do
        auctionService.cancelAuction(request.getAuctionId(), adminId, UserRole.ADMIN, request.getReason());
    }

    /**
     * 4. Cưỡng chế hạ tải/gỡ bỏ vật phẩm vi phạm
     * CMD_ADMIN_DELETE_ITEM
     * @param adminId Bốc từ ClientSession bảo mật
     */
    /**
     * 4. Cưỡng chế hạ tải/gỡ bỏ vật phẩm vi phạm
     * CMD_ADMIN_DELETE_ITEM
     */
    public void deleteItem(String adminId, DeleteItemRequest request) {
        if (request == null) throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Yêu cầu không hợp lệ.");
        String itemId = request.getItemId();
        // 1. Dùng RAM cache để tìm nhanh xem có phiên đấu giá nào đang hoạt động liên kết với sản phẩm này không
        Auction activeAuction = AuctionManage.getInstance().getAllActive().stream()
                .filter(a -> a.getItemId().equals(itemId))
                .findFirst()
                .orElse(null);
        // 2. Nếu phát hiện có phiên liên đới đang hoạt động, tiến hành cưỡng chế hủy phiên trước
        if (activeAuction != null) {
            String cancelReason = "Hủy tự động do vật phẩm bị Admin gỡ bỏ khỏi sàn. Lý do: " + request.getReason();

            // Hàm này sẽ tự động: Hoàn tiền đóng băng cho Bidder dẫn đầu, ghi Audit Log hủy phiên,
            // đổi trạng thái phiên thành CANCELED, và gửi thông báo Real-time cho mọi client trong phòng.
            auctionService.cancelAuction(activeAuction.getId(), adminId, UserRole.ADMIN, cancelReason);

            System.out.println("[Admin Controller] ⚠️ Đã cưỡng chế hủy phiên đấu giá liên đới: " + activeAuction.getId());
        }
        // 3. Sau khi phòng đấu giá đã được dọn dẹp sạch sẽ, tiến hành chuyển trạng thái Item sang BANNED
        // Hàm này sẽ ghi đè trạng thái ACTIVE (do cancelAuction đặt lại lúc nãy) thành BANNED dưới DB,
        // ghi Audit Log xóa sản phẩm, và xóa khỏi RAM cache.
        itemService.deleteItemByAdmin(itemId, adminId, request.getReason());
    }

    /**
     * 5. Tải danh sách nhật ký kiểm toán hệ thống (Audit Logs)
     * CMD_ADMIN_GET_LOGS
     */
    public PageDTO<ActionLogDTO> getAuditLogs(GetAuditLogsRequest request) {
        if (request == null) throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Yêu cầu không hợp lệ.");
        return logService.getLogsForAdminDashboard(request.getPage(), request.getPageSize());
    }
}
