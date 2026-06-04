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
import com.auction.service.AdminQueryService;

/**
 * Bộ điều khiển tiếp nhận các yêu cầu quản trị cao cấp (Admin) từ mạng.
 */
public class AdminController {

    private final UserService userService = new UserService();
    private final AuctionService auctionService = AuctionService.getInstance();
    private final ItemService itemService = new ItemService();
    private final LogService logService = new LogService();
    private final AdminQueryService adminQueryService = new AdminQueryService();

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
        Auction activeAuction = AuctionManage.getInstance().getAllActive().stream()
                .filter(a -> a.getItemId().equals(itemId))
                .findFirst()
                .orElse(null);
        if (activeAuction != null) {
            String cancelReason = "Hủy tự động do vật phẩm bị Admin gỡ bỏ khỏi sàn. Lý do: " + request.getReason();

            auctionService.cancelAuction(activeAuction.getId(), adminId, UserRole.ADMIN, cancelReason);

            System.out.println("[Admin Controller] ⚠️ Đã cưỡng chế hủy phiên đấu giá liên đới: " + activeAuction.getId());
        }
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

    /**
     * 6. Tải danh sách toàn bộ Items phân trang cho Admin Dashboard
     * CMD_ADMIN_GET_ITEMS
     */
    public PageDTO<ItemSummaryDTO> getItemsDashboard(GetUserDashboardRequest request) {
        if (request == null) throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Yêu cầu không hợp lệ.");
        return adminQueryService.getAdminItemDashboard(request.getPage(), request.getPageSize());
    }

    /**
     * 7. Tải danh sách toàn bộ Auctions phân trang cho Admin Dashboard
     * CMD_ADMIN_GET_AUCTIONS
     */
    public PageDTO<AuctionSummaryDTO> getAuctionsDashboard(GetUserDashboardRequest request) {
        if (request == null) throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Yêu cầu không hợp lệ.");
        return adminQueryService.getAdminAuctionDashboard(request.getPage(), request.getPageSize());
    }
}
