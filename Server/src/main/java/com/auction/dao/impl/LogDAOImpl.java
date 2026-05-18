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
     * Cột created_at sẽ được Database tự động sinh thời gian thực (TIMESTAMP)
     */
    @Override
    public boolean insertLog(String logId, String adminId, String actionDetail, String targetType, String targetId) {
        String sql = "INSERT INTO action_logs (id, admin_id, action_detail, target_type, target_id) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, logId);
            stmt.setString(2, adminId);
            stmt.setString(3, actionDetail);
            stmt.setString(4, targetType);
            stmt.setString(5, targetId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi Insert Log: " + e.getMessage());
            return false;
        }
    }

    /**
     * TRUY VẤN DANH SÁCH LOG PHÂN TRANG (PAGINATION)
     * Luôn ưu tiên hiển thị các hành động mới thực hiện lên trên đầu (ORDER BY created_at DESC)
     */
    public List<ActionLogDTO> findPaginatedLogs(int limit, int offset) {
        String sql = "SELECT id, admin_id, action_detail, target_type, target_id, created_at " +
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
                    // Trích xuất mốc thời gian TIMESTAMP từ DB chuyển sang LocalDateTime của Java
                    java.time.LocalDateTime time = rs.getTimestamp("created_at").toLocalDateTime();

                    // Khởi tạo DTO đóng gói trực tiếp tài nguyên
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
     * 🔥 HÀM BỔ SUNG PHỤC VỤ PHÂN TRANG: Đếm tổng số dòng log trong hệ thống
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