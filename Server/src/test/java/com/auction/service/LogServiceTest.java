package com.auction.service;

import com.auction.dao.impl.LogDAOImpl;
import com.auction.dto.ActionLogDTO;
import com.auction.dto.PageDTO;
import com.auction.exception.ValidationErrorCode;
import com.auction.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogServiceTest {

    private LogService logService;
    private FakeLogDAO logDAO;

    @BeforeEach
    void setUp() throws Exception {
        logService = new LogService();

        logDAO = new FakeLogDAO();

        injectField(logService, "logDAO", logDAO);
    }

    // Inject fake LogDAO vào LogService để không gọi database thật
    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // Check đúng mã lỗi ValidationException
    private void assertValidationError(ValidationException exception, ValidationErrorCode expectedError) {
        assertEquals(expectedError.getCode(), exception.getErrorCode());
    }

    // Tạo log mẫu
    private ActionLogDTO sampleLog(String logId) {
        return new ActionLogDTO(
                logId,
                "admin-1",
                "BANNED user user-1",
                "USER",
                "user-1",
                LocalDateTime.now()
        );
    }

    /**
     * Fake DAO thay cho LogDAOImpl thật.
     *
     * LogService đang dùng LogDAOImpl cụ thể,
     * nên fake này extends LogDAOImpl để inject được vào field.
     */
    private static class FakeLogDAO extends LogDAOImpl {
        List<ActionLogDTO> logsToReturn = new ArrayList<>();
        long totalLogCount = 0;

        int lastLimit;
        int lastOffset;

        boolean findPaginatedLogsCalled = false;
        boolean getTotalLogCountCalled = false;

        @Override
        public List<ActionLogDTO> findPaginatedLogs(int limit, int offset) {
            findPaginatedLogsCalled = true;
            lastLimit = limit;
            lastOffset = offset;
            return logsToReturn;
        }

        @Override
        public long getTotalLogCount() {
            getTotalLogCountCalled = true;
            return totalLogCount;
        }

        @Override
        public void insertLog(Connection conn, String logId, String adminId, String actionDetail, String targetType, String targetId) throws SQLException {
            // Không dùng trong LogServiceTest
        }
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    @Test
    void getLogsForAdminDashboardShouldThrowWhenPageIsZero() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            logService.getLogsForAdminDashboard(0, 10);
        });

        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    @Test
    void getLogsForAdminDashboardShouldThrowWhenPageIsNegative() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            logService.getLogsForAdminDashboard(-1, 10);
        });

        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    @Test
    void getLogsForAdminDashboardShouldThrowWhenPageSizeIsZero() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            logService.getLogsForAdminDashboard(1, 0);
        });

        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    @Test
    void getLogsForAdminDashboardShouldThrowWhenPageSizeIsNegative() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            logService.getLogsForAdminDashboard(1, -5);
        });

        assertValidationError(exception, ValidationErrorCode.INVALID_PARAMETER);
    }

    // =========================================================
    // EMPTY RESULT
    // =========================================================

    @Test
    void getLogsForAdminDashboardShouldReturnEmptyPageWhenDaoReturnsEmptyList() {
        logDAO.logsToReturn = List.of();

        PageDTO<ActionLogDTO> result = logService.getLogsForAdminDashboard(1, 10);

        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
        assertEquals(1, result.getCurrentPage());
        assertEquals(0, result.getTotalPages());
        assertEquals(0, result.getTotalElements());

        assertTrue(logDAO.findPaginatedLogsCalled);
        assertFalse(logDAO.getTotalLogCountCalled);

        assertEquals(10, logDAO.lastLimit);
        assertEquals(0, logDAO.lastOffset);
    }

    @Test
    void getLogsForAdminDashboardShouldReturnEmptyPageWhenDaoReturnsNull() {
        logDAO.logsToReturn = null;

        PageDTO<ActionLogDTO> result = logService.getLogsForAdminDashboard(2, 5);

        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
        assertEquals(2, result.getCurrentPage());
        assertEquals(0, result.getTotalPages());
        assertEquals(0, result.getTotalElements());

        assertTrue(logDAO.findPaginatedLogsCalled);
        assertFalse(logDAO.getTotalLogCountCalled);

        assertEquals(5, logDAO.lastLimit);
        assertEquals(5, logDAO.lastOffset);
    }

    // =========================================================
    // PAGINATION
    // =========================================================

    @Test
    void getLogsForAdminDashboardShouldCalculateOffsetCorrectly() {
        logDAO.logsToReturn = List.of(sampleLog("log-1"));
        logDAO.totalLogCount = 100;

        logService.getLogsForAdminDashboard(3, 20);

        assertEquals(20, logDAO.lastLimit);
        assertEquals(40, logDAO.lastOffset);
    }

    @Test
    void getLogsForAdminDashboardShouldReturnPageWithLogsAndTotalInfo() {
        ActionLogDTO log1 = sampleLog("log-1");
        ActionLogDTO log2 = sampleLog("log-2");

        logDAO.logsToReturn = List.of(log1, log2);
        logDAO.totalLogCount = 25;

        PageDTO<ActionLogDTO> result = logService.getLogsForAdminDashboard(2, 10);

        assertNotNull(result);
        assertEquals(2, result.getData().size());

        assertSame(log1, result.getData().get(0));
        assertSame(log2, result.getData().get(1));

        assertEquals(2, result.getCurrentPage());
        assertEquals(3, result.getTotalPages());
        assertEquals(25, result.getTotalElements());

        assertTrue(logDAO.findPaginatedLogsCalled);
        assertTrue(logDAO.getTotalLogCountCalled);

        assertEquals(10, logDAO.lastLimit);
        assertEquals(10, logDAO.lastOffset);
    }

    @Test
    void getLogsForAdminDashboardShouldReturnOneTotalPageWhenTotalElementsLessThanPageSize() {
        logDAO.logsToReturn = List.of(sampleLog("log-1"));
        logDAO.totalLogCount = 7;

        PageDTO<ActionLogDTO> result = logService.getLogsForAdminDashboard(1, 10);

        assertEquals(1, result.getTotalPages());
        assertEquals(7, result.getTotalElements());
    }

    @Test
    void getLogsForAdminDashboardShouldCalculateTotalPagesWithCeil() {
        logDAO.logsToReturn = List.of(sampleLog("log-1"));
        logDAO.totalLogCount = 21;

        PageDTO<ActionLogDTO> result = logService.getLogsForAdminDashboard(1, 10);

        assertEquals(3, result.getTotalPages());
        assertEquals(21, result.getTotalElements());
    }

    @Test
    void getLogsForAdminDashboardShouldReturnOneTotalPageWhenTotalElementsIsZeroButLogsExist() {
        logDAO.logsToReturn = List.of(sampleLog("log-1"));
        logDAO.totalLogCount = 0;

        PageDTO<ActionLogDTO> result = logService.getLogsForAdminDashboard(1, 10);

        assertEquals(1, result.getTotalPages());
        assertEquals(0, result.getTotalElements());
    }
}