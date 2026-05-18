package com.auction.service;

import com.auction.dao.impl.LogDAOImpl;
import com.auction.dto.ActionLogDTO;
import com.auction.dto.PageDTO; // 🔥 Nạp lớp PageDTO dùng chung

import java.util.List;

public class LogService {
    private final LogDAOImpl logDAO = new LogDAOImpl();

    /**
     * Lấy danh sách lịch sử hệ thống đóng gói trọn gói dạng PageDTO dùng chung.
     * Phục vụ hoàn hảo cho Socket truyền tin và JavaFX vẽ giao diện có nút lật trang.
     *
     * @param page Trang muốn xem (Bắt đầu từ 1)
     * @param pageSize Số dòng hiển thị trên một trang
     */
    public PageDTO<ActionLogDTO> getLogsForAdminDashboard(int page, int pageSize) {
        if (page <= 0 || pageSize <= 0) return null;

        // 1. Tính toán vị trí dịch dòng (Offset) cho SQL
        int offset = (page - 1) * pageSize;

        // 2. Kéo danh sách log của riêng phân đoạn trang này lên
        List<ActionLogDTO> logs = logDAO.findPaginatedLogs(pageSize, offset);

        // 3. Lấy tổng số lượng dòng log đang có trong toàn bộ Database
        long totalElements = logDAO.getTotalLogCount();

        // 4. Tính toán tổng số trang dựa trên kích thước trang (pageSize)
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);

        // 5. Đóng gói vào đối tượng Generic PageDTO trả về cho luồng mạng
        return new PageDTO<>(logs, page, totalPages, totalElements);
    }
}