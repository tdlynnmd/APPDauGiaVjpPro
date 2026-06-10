package com.auction.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO lưu trữ nhật ký thao tác kiểm duyệt của quản trị viên (Admin).
 */
public class ActionLogDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String logId;
    private String adminId;
    private String actionDetail;
    private String targetType;
    private String targetId;
    private LocalDateTime timestamp;

    private String adminName;
    private String targetName;

    public ActionLogDTO(String logId, String adminId, String actionDetail, String targetType, String targetId, LocalDateTime timestamp) {
        this.logId = logId;
        this.adminId = adminId;
        this.actionDetail = actionDetail;
        this.targetType = targetType;
        this.targetId = targetId;
        this.timestamp = timestamp;
    }

    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }

    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }

    public String getActionDetail() { return actionDetail; }
    public void setActionDetail(String actionDetail) { this.actionDetail = actionDetail; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
}