package com.auction.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {
    private static HikariConfig config = new HikariConfig();
    private static HikariDataSource ds;

    static {
        config.setJdbcUrl("jdbc:mysql://localhost:3306/clb_db");
        config.setUsername("root");
        config.setPassword("Son22092007@");

        // Cấu hình tối ưu cho Connection Pool
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setMaximumPoolSize(10); // Tối đa 10 kết nối cùng lúc

        ds = new HikariDataSource(config);
    }

    private DatabaseConnection() {} // Chống khởi tạo từ bên ngoài

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
}
