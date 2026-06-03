package com.auction.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {
    // Để volatile để bảo đảm tính hiển thị hiển nhiên trong môi trường đa luồng
    private static volatile HikariDataSource dataSource;

    private static final String DEFAULT_JDBC_URL = "jdbc:mysql://localhost:3306/vnu_auction_system";
    private static final String DEFAULT_USERNAME = "root";

    // 🔥 SỬA TẠI ĐÂY: Xóa hoàn toàn mật khẩu thật, ép buộc sử dụng chuỗi rỗng mặc định của MySQL local
    private static final String DEFAULT_PASSWORD = "";
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    private DatabaseConnection() {} // Anti-instantiation

    /**
     * 🔥 TỐI ƯU CHUYÊN NGHIỆP: Thay thế khối static bằng hàm khởi tạo tường minh.
     * Hàm này sẽ được nhạc trưởng hàm main() chủ động kích hoạt khi bắt đầu bật Server.
     */
    public static synchronized void initialize() {
        if (dataSource != null) {
            return; // Đảm bảo tính Idempotent - không khởi tạo trùng lặp pool
        }

        try {
            HikariConfig config = new HikariConfig();

            // 1. Lấy thông tin credentials và URL từ env hoặc default
            String jdbcUrl = resolveJdbcUrl();
            String username = resolveDbUsername();
            String password = resolveDbPassword();

            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);

            // 2. Cấu hình tối ưu hiệu năng cao cho Connection Pool (MySQL Specific)
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("serverTimezone", "UTC");

            // 🔥 TỐI ƯU THÔNG MINH: Chỉ ép bật SSL REQUIRED nếu chuỗi kết nối hướng lên Cloud Azure.
            // Nếu chạy dưới localhost, hệ thống tự động tắt đi để tránh lỗi từ chối kết nối vật lý.
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

            // Cấu hình kích thước Pool an toàn cho hệ thống Socket
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(5);
            config.setConnectionTimeout(30000); // 30 seconds
            config.setIdleTimeout(600000); // 10 minutes
            config.setMaxLifetime(1800000); // 30 minutes

            // 3. Khai hỏa thiết lập DataSource duy nhất
            dataSource = new HikariDataSource(config);

            // 4. Tự động xóa ràng buộc UNIQUE uq_item_id trên bảng auctions nếu tồn tại
            // Ràng buộc này gây lỗi duplicate entry khi tạo nhiều phiên cho cùng 1 item
            try (java.sql.Connection conn = dataSource.getConnection();
                 java.sql.Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE auctions DROP INDEX uq_item_id");
                System.out.println("[DatabaseConnection] ✅ Đã xóa ràng buộc unique uq_item_id khỏi bảng auctions.");
            } catch (java.sql.SQLException ignored) {
                // Index không tồn tại - bỏ qua lỗi
            }

        } catch (Exception e) {
            // Không nuốt lỗi câm lặng, ném Exception rõ ràng để hàm main dừng khởi động Server
            throw new RuntimeException("Trọng pháo khởi tạo Database Connection Pool bị gãy!", e);
        }
    }

    /**
     * Lấy kết nối vật lý an toàn từ Pool phục vụ cho tầng DAO
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            // Phòng hờ nếu lập trình viên quên gọi initialize() ở hàm main
            initialize();
        }
        return dataSource.getConnection();
    }

    /**
     * Đóng pool an toàn phục vụ tiến trình Graceful Shutdown khi tắt Server
     */
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
        // 🔥 SỬA TẠI ĐÂY: Nếu file .env không khai báo, hệ thống lấy DEFAULT_PASSWORD là rỗng ("")
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