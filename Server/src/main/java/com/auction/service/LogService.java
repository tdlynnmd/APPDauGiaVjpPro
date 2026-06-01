package com.auction.service;

import com.auction.dao.impl.LogDAOImpl;
import com.auction.dto.ActionLogDTO;
import com.auction.dto.PageDTO;
import com.auction.exception.ValidationErrorCode;
import com.auction.exception.ValidationException;

import java.util.ArrayList;
import java.util.List;

public class LogService {
    private final LogDAOImpl logDAO = new LogDAOImpl();

    /**
     * Returns paginated audit logs with stable pagination metadata for the admin UI.
     */
    public PageDTO<ActionLogDTO> getLogsForAdminDashboard(int page, int pageSize) {
        if (page <= 0 || pageSize <= 0) {
            throw new ValidationException(ValidationErrorCode.INVALID_PARAMETER, "Page index and frame boundary size must be positive.");
        }

        int offset = (page - 1) * pageSize;
        List<ActionLogDTO> logs = logDAO.findPaginatedLogs(pageSize, offset);
        List<ActionLogDTO> safeLogs = logs == null ? new ArrayList<>() : logs;

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
