package com.auction.dao.impl;

import com.auction.config.DatabaseConnection;
import com.auction.dao.LogDAO;

import java.sql.*;

public class LogDAOImpl implements LogDAO {

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
        } catch (SQLException e) { return false; }
    }
}