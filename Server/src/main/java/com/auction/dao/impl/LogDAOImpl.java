package com.auction.dao.impl;

import com.auction.config.DatabaseConnection;
import com.auction.dao.LogDAO;
import com.auction.dto.ActionLogDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LogDAOImpl implements LogDAO {

    /**
     * GHI LOG HÀNH ĐỘNG MỚI CỦA ADMIN
     * 🔥 SỬA: Nhận Connection từ ngoài truyền vào và ném ngoại lệ lên tầng điều phối (Service)
     */
    @Override
    public void insertLog(Connection conn, String logId, String adminId, String actionDetail, String targetType, String targetId) throws SQLException {
        String sql = "INSERT INTO action_logs (id, admin_id, action_detail, target_type, target_id) VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), ?, ?, UUID_TO_BIN(?, 1))";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, logId);
            stmt.setString(2, adminId);
            stmt.setString(3, actionDetail);
            stmt.setString(4, targetType);
            stmt.setString(5, targetId);

            stmt.executeUpdate();
        }
    }

    /**
     * TRUY VẤN DANH SÁCH LOG PHÂN TRANG (PAGINATION)
     * Hàm ĐỌC (SELECT) độc lập, tự mở connection nên giữ lại try-catch cục bộ an toàn
     */
    @Override
    public List<ActionLogDTO> findPaginatedLogs(int limit, int offset) {
        String sql = "SELECT BIN_TO_UUID(id, 1) AS id, BIN_TO_UUID(admin_id, 1) AS admin_id, action_detail, target_type, BIN_TO_UUID(target_id, 1) AS target_id, created_at " +
                "FROM action_logs " +
                "ORDER BY created_at DESC " +
                "LIMIT ? OFFSET ?";

        List<ActionLogDTO> logList = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            stmt.setInt(2, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    java.time.LocalDateTime time = rs.getTimestamp("created_at").toLocalDateTime();

                    ActionLogDTO dto = new ActionLogDTO(
                            rs.getString("id"),
                            rs.getString("admin_id"),
                            rs.getString("action_detail"),
                            rs.getString("target_type"),
                            rs.getString("target_id"),
                            time
                    );
                    logList.add(dto);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi truy vấn phân trang Action Log: " + e.getMessage());
        }
        return logList;
    }

    /**
     * Đếm tổng số dòng log trong hệ thống
     * Hàm ĐỌC (SELECT) độc lập, tự mở connection nên giữ lại try-catch cục bộ an toàn
     */
    @Override
    public long getTotalLogCount() {
        String sql = "SELECT COUNT(*) FROM action_logs";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi đếm tổng số log: " + e.getMessage());
        }
        return 0;
    }
}