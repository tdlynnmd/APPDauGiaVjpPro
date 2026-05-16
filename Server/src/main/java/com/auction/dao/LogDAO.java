package com.auction.dao;

public interface LogDAO {
    public boolean insertLog(String logId, String adminId, String actionDetail, String targetType, String targetId);
}
