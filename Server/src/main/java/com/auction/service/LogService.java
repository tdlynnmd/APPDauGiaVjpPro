package com.auction.service;

import com.auction.dao.UserDAO;
import com.auction.dao.ItemDAO;
import com.auction.dao.impl.UserDAOImpl;
import com.auction.dao.impl.ItemDAOImpl;
import com.auction.dao.impl.LogDAOImpl;
import com.auction.dto.ActionLogDTO;
import com.auction.dto.PageDTO;
import com.auction.exception.ValidationErrorCode;
import com.auction.exception.ValidationException;
import com.auction.models.User.User;
import com.auction.models.Item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * Dịch vụ lưu trữ và ghi nhận nhật ký kiểm toán (Audit Logs) hệ thống.
 */
public class LogService {
    private final LogDAOImpl logDAO = new LogDAOImpl();
    private final UserDAO userDAO = new UserDAOImpl();
    private final ItemDAO itemDAO = new ItemDAOImpl();

    public PageDTO<ActionLogDTO> getLogsForAdminDashboard(int page, int pageSize) {
        if (page <= 0 || pageSize <= 0) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Page index and frame boundary size must be positive.");
        }

        int offset = (page - 1) * pageSize;
        List<ActionLogDTO> logs = logDAO.findPaginatedLogs(pageSize, offset);
        List<ActionLogDTO> safeLogs = logs == null ? new ArrayList<>() : logs;

        for (ActionLogDTO logDto : safeLogs) {
            if (logDto.getAdminId() != null) {
                userDAO.findById(logDto.getAdminId()).ifPresent(user -> {
                    logDto.setAdminName(user.getUsername());
                });
            }
            if (logDto.getTargetId() != null && logDto.getTargetType() != null) {
                String type = logDto.getTargetType().toUpperCase();
                if ("USER".equals(type)) {
                    userDAO.findById(logDto.getTargetId()).ifPresent(user -> {
                        logDto.setTargetName(user.getUsername());
                    });
                } else if ("ITEM".equals(type)) {
                    itemDAO.findById(logDto.getTargetId()).ifPresent(item -> {
                        logDto.setTargetName(item.getName());
                    });
                }
            }
        }

        long totalElements = logDAO.getTotalLogCount();
        int totalPages;
        if (totalElements > 0) {
            totalPages = (int) Math.ceil((double) totalElements / pageSize);
        } else {
            totalPages = safeLogs.isEmpty() ? 0 : 1;
        }

        return new PageDTO<>(safeLogs, page, totalPages, totalElements);
    }
}
