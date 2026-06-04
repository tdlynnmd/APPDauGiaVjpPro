package com.auction.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Lớp quản lý kết nối cơ sở dữ liệu MySQL sử dụng HikariCP Connection Pool.
 */
public class DatabaseConnection {
    private static volatile HikariDataSource dataSource;

    private static final String DEFAULT_JDBC_URL = "jdbc:mysql://localhost:3306/vnu_auction_system";
    private static final String DEFAULT_USERNAME = "root";

    private static final String DEFAULT_PASSWORD = "";
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    private DatabaseConnection() {}

    public static synchronized void initialize() {
        if (dataSource != null) {
            return;
        }

        try {
            HikariConfig config = new HikariConfig();

            String jdbcUrl = resolveJdbcUrl();
            String username = resolveDbUsername();
            String password = resolveDbPassword();

            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);

            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("serverTimezone", "UTC");

            if (jdbcUrl.contains("azure") || "true".equalsIgnoreCase(dotenv.get("DB_USE_SSL"))) {
                config.addDataSourceProperty("useSSL", "true");
                config.addDataSourceProperty("sslMode", "REQUIRED");
                config.addDataSourceProperty("allowPublicKeyRetrieval", "true");
                System.out.println("[DatabaseConnection] 🔒 Kích hoạt chế độ bảo mật SSL nghiêm ngặt cho Cloud.");
            } else {
                config.addDataSourceProperty("useSSL", "false");
                config.addDataSourceProperty("allowPublicKeyRetrieval", "true");
                System.out.println("[DatabaseConnection] 🔓 Kết nối cơ sở dữ liệu ở chế độ Local (Tắt mã hóa SSL).");
            }

            config.setMaximumPoolSize(10);
            config.setMinimumIdle(5);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);

            dataSource = new HikariDataSource(config);

            try (java.sql.Connection conn = dataSource.getConnection();
                 java.sql.Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE auctions DROP INDEX uq_item_id");
                System.out.println("[DatabaseConnection] ✅ Đã xóa ràng buộc unique uq_item_id khỏi bảng auctions.");
            } catch (java.sql.SQLException ignored) {
            }

        } catch (Exception e) {
            throw new RuntimeException("Trọng pháo khởi tạo Database Connection Pool bị gãy!", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            initialize();
        }
        return dataSource.getConnection();
    }

    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("[DatabaseConnection] ℹ️ Database connection pool đã đóng an toàn.");
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