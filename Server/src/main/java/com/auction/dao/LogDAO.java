package com.auction.dao;

public interface LogDAO {
    void insertLog(String logId, String adminId, String actionDetail, String targetType, String targetId);

    long getTotalLogCount();
}
