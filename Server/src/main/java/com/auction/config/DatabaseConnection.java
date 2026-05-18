package com.auction.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final HikariDataSource dataSource;
    private static final String DEFAULT_JDBC_URL = "jdbc:mysql://localhost:3306/vnu_auction_system";
    private static final String DEFAULT_USERNAME = "root";
    private static final String DEFAULT_PASSWORD = "Son22092007@";
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    static {
        try {
            // Chuyển config thành biến cục bộ trong khối static
            HikariConfig config = new HikariConfig();

            // 1. Lấy thông tin credentials và URL từ env hoặc default
            String jdbcUrl = resolveJdbcUrl();
            String username = resolveDbUsername();
            String password = resolveDbPassword();

            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);

            // 2. Cấu hình tối ưu cho Connection Pool
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            // Cấu hình SSL nghiêm ngặt cho Azure MySQL
            config.addDataSourceProperty("useSSL", "true");
            config.addDataSourceProperty("sslMode", "REQUIRED");
            config.addDataSourceProperty("allowPublicKeyRetrieval", "true");
            config.addDataSourceProperty("serverTimezone", "UTC");

            // Cấu hình kích thước Pool
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(5);
            config.setConnectionTimeout(30000); // 30 seconds
            config.setIdleTimeout(600000); // 10 minutes
            config.setMaxLifetime(1800000); // 30 minutes

            // 3. Khởi tạo DataSource duy nhất
            dataSource = new HikariDataSource(config);

        } catch (Exception e) {
            System.err.println("[DatabaseConnection] ❌ Lỗi khởi tạo database connection pool:");
            System.err.println("[DatabaseConnection] " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Không thể khởi tạo database connection pool", e);
        }
    }

    private DatabaseConnection() {} // Anti-instantiation

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // Thêm hàm đóng pool an toàn khi ứng dụng tắt (Shutdown Hook)
    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("[DatabaseConnection] ℹ️ Database connection pool đã đóng.");
        }
    }

    private static String resolveJdbcUrl() {
        String directUrl = firstNonBlank(
                dotenv.get("AUCTION_DB_URL"),
                dotenv.get("DB_URL")
        );
        if (directUrl != null) {
            if (directUrl.contains("<") || directUrl.contains(">")) {
                System.err.println("[DatabaseConnection] ⚠️ CẢNH BÁO: AUCTION_DB_URL vẫn chứa placeholder '<>'");
            }
            return directUrl;
        }

        String host = firstNonBlank(dotenv.get("AUCTION_DB_HOST"), dotenv.get("DB_HOST"));
        String port = firstNonBlank(dotenv.get("AUCTION_DB_PORT"), dotenv.get("DB_PORT"));
        String name = firstNonBlank(dotenv.get("AUCTION_DB_NAME"), dotenv.get("DB_NAME"));

        if (host == null || name == null) {
            System.out.println("[DatabaseConnection] ℹ️ Không tìm thấy cấu hình env, sử dụng default local URL");
            return DEFAULT_JDBC_URL;
        }

        String resolvedPort = port == null ? "3306" : port;
        return "jdbc:mysql://" + host + ":" + resolvedPort + "/" + name;
    }

    private static String resolveDbUsername() {
        return firstNonBlank(
                dotenv.get("AUCTION_DB_USERNAME"),
                dotenv.get("DB_USER"),
                dotenv.get("DB_USERNAME"),
                DEFAULT_USERNAME
        );
    }

    private static String resolveDbPassword() {
        return firstNonBlank(
                dotenv.get("AUCTION_DB_PASSWORD"),
                dotenv.get("DB_PASSWORD"),
                DEFAULT_PASSWORD
        );
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;

        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }

        return null;
    }
}