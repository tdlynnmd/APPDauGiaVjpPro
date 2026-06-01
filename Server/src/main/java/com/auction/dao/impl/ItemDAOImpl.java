package com.auction.dao.impl;

import com.auction.config.DatabaseConnection;
import com.auction.dao.ItemDAO;
import com.auction.enums.ItemStatus;
import com.auction.models.Item.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemDAOImpl implements ItemDAO {

    // 🔥 SỬA: Nhận Connection từ ngoài truyền vào (giống các DAO khác) và ném SQLException lên Service
    @Override
    public boolean insertItem(Connection conn, Item item) throws SQLException {
        String sql = "INSERT INTO items (id, item_type, seller_id, name, description, starting_price, year_created, image_url, status, " +
                "painter, art_style, brand, warranty_months, model, km_age, license_plate, engine_type) " +
                "VALUES (UUID_TO_BIN(?, 1), ?, UUID_TO_BIN(?, 1), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            // 1. Set các trường CHUNG (Common fields)
            stmt.setString(1, item.getId());
            stmt.setString(2, item.getItemType().name());
            stmt.setString(3, item.getSellerId());
            stmt.setString(4, item.getName());
            stmt.setString(5, item.getDescription());
            stmt.setDouble(6, item.getStartingPrice());

            if (item.getYearCreated() > 0) {
                stmt.setInt(7, item.getYearCreated());
            } else {
                stmt.setNull(7, Types.INTEGER);
            }

            stmt.setString(8, item.getImageUrl());
            stmt.setString(9, item.getStatus() != null ? item.getStatus().name() : "ACTIVE");

            // 2. Set các trường ĐẶC THÙ (Khởi tạo toàn bộ bằng NULL trước)
            stmt.setNull(10, Types.VARCHAR); // painter
            stmt.setNull(11, Types.VARCHAR); // art_style
            stmt.setNull(12, Types.VARCHAR); // brand
            stmt.setNull(13, Types.INTEGER); // warranty_months
            stmt.setNull(14, Types.VARCHAR); // model
            stmt.setNull(15, Types.DECIMAL); // km_age
            stmt.setNull(16, Types.VARCHAR); // license_plate
            stmt.setNull(17, Types.VARCHAR); // engine_type

            // 3. Phân loại theo instanceof để điền giá trị thật đè lên NULL
            switch (item) {
                case Art art -> {
                    if (art.getPainter() != null) stmt.setString(10, art.getPainter());
                    if (art.getArtStyle() != null) stmt.setString(11, art.getArtStyle());
                }
                case Electronics elec -> {
                    if (elec.getBrand() != null) stmt.setString(12, elec.getBrand());
                    if (elec.getWarrantyMonths() > 0) {
                        stmt.setInt(13, elec.getWarrantyMonths());
                    }
                }
                case Vehicle vehicle -> {
                    if (vehicle.getModel() != null) stmt.setString(14, vehicle.getModel());
                    if (vehicle.getKmAge() >= 0) {
                        stmt.setDouble(15, vehicle.getKmAge());
                    }
                    if (vehicle.getLicensePlate() != null) stmt.setString(16, vehicle.getLicensePlate());
                    if (vehicle.getEngineType() != null) stmt.setString(17, vehicle.getEngineType());
                }
                default -> {
                }
            }

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Hàm ĐỌC (SELECT) độc lập, tự mở connection nên giữ lại try-catch cục bộ an toàn
     */
    @Override
    public Optional<Item> findById(String id) {
        String sql = "SELECT BIN_TO_UUID(id, 1) AS id, item_type, BIN_TO_UUID(seller_id, 1) AS seller_id, name, description, starting_price, year_created, image_url, status, painter, art_style, brand, warranty_months, model, km_age, license_plate, engine_type, created_at FROM items WHERE id = UUID_TO_BIN(?, 1) AND deleted_at IS NULL";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToItem(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi Find Item: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Hàm ĐỌC (SELECT) độc lập, tự mở connection nên giữ lại try-catch cục bộ an toàn
     */
    @Override
    public List<Item> findBySellerId(String sellerId) {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT BIN_TO_UUID(id, 1) AS id, item_type, BIN_TO_UUID(seller_id, 1) AS seller_id, name, description, starting_price, year_created, image_url, status, painter, art_style, brand, warranty_months, model, km_age, license_plate, engine_type, created_at FROM items WHERE seller_id = UUID_TO_BIN(?, 1) AND deleted_at IS NULL ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sellerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToItem(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi Find Items by Seller: " + e.getMessage());
        }
        return items;
    }

    // 🔥 SỬA: Loại bỏ khối try-catch nuốt lỗi, ném trực tiếp lỗi ra ngoài Service phục vụ rollback chuỗi Transaction
    @Override
    public boolean updateStatus(Connection conn, String itemId, String newStatus) throws SQLException {
        String sql = "UPDATE items SET status = ? WHERE id = UUID_TO_BIN(?, 1)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus);
            stmt.setString(2, itemId);
            return stmt.executeUpdate() > 0;
        }
    }

    // 🔥 SỬA: Nhận Connection từ ngoài truyền vào và ném lỗi lên Service điều phối
    @Override
    public boolean updateItem(Connection conn, Item item) throws SQLException {
        String sql = "UPDATE items SET name = ?, description = ?, starting_price = ?, year_created = ?, image_url = ?, status = ?, " +
                "painter = ?, art_style = ?, brand = ?, warranty_months = ?, model = ?, km_age = ?, license_plate = ?, engine_type = ? " +
                "WHERE id = UUID_TO_BIN(?, 1) AND deleted_at IS NULL";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            // 1. Gán giá trị cho các trường thông tin CHUNG
            stmt.setString(1, item.getName());
            stmt.setString(2, item.getDescription());
            stmt.setDouble(3, item.getStartingPrice());

            if (item.getYearCreated() > 0) {
                stmt.setInt(4, item.getYearCreated());
            } else {
                stmt.setNull(4, Types.INTEGER);
            }
            stmt.setString(5, item.getImageUrl());
            stmt.setString(6, item.getStatus().name());

            // 2. Khởi tạo mặc định NULL cho toàn bộ các trường ĐẶC THÙ của các lớp con
            stmt.setNull(7, Types.VARCHAR);  // painter
            stmt.setNull(8, Types.VARCHAR);  // art_style
            stmt.setNull(9, Types.VARCHAR);  // brand
            stmt.setNull(10, Types.INTEGER); // warranty_months
            stmt.setNull(11, Types.VARCHAR); // model
            stmt.setNull(12, Types.DECIMAL); // km_age
            stmt.setNull(13, Types.VARCHAR); // license_plate
            stmt.setNull(14, Types.VARCHAR); // engine_type

            // 3. Sử dụng switch pattern matching để ghi đè dữ liệu thực tế dựa trên kiểu thực thể
            switch (item) {
                case Art art -> {
                    if (art.getPainter() != null) stmt.setString(7, art.getPainter());
                    if (art.getArtStyle() != null) stmt.setString(8, art.getArtStyle());
                }
                case Electronics elec -> {
                    if (elec.getBrand() != null) stmt.setString(9, elec.getBrand());
                    if (elec.getWarrantyMonths() > 0) stmt.setInt(10, elec.getWarrantyMonths());
                }
                case Vehicle vehicle -> {
                    if (vehicle.getModel() != null) stmt.setString(11, vehicle.getModel());
                    if (vehicle.getKmAge() >= 0) stmt.setDouble(12, vehicle.getKmAge());
                    if (vehicle.getLicensePlate() != null) stmt.setString(13, vehicle.getLicensePlate());
                    if (vehicle.getEngineType() != null) stmt.setString(14, vehicle.getEngineType());
                }
                default -> {
                }
            }

            // 4. Khớp điều kiện WHERE bằng ID của vật phẩm ở vị trí cuối cùng
            stmt.setString(15, item.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Helper method: Xử lý Đa hình (Polymorphism) Hydration
     * Hàm này vốn dĩ đã throws SQLException sẵn nên giữ nguyên cấu trúc
     */
    private Item mapResultSetToItem(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String name = rs.getString("name");
        double startingPrice = rs.getDouble("starting_price");
        String description = rs.getString("description");

        int yearCreated = rs.getInt("year_created");
        if (rs.wasNull()) {
            yearCreated = 0;
        }

        String sellerId = rs.getString("seller_id");
        String imageUrl = rs.getString("image_url");
        ItemStatus status = ItemStatus.valueOf(rs.getString("status"));
        java.time.LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        String typeStr = rs.getString("item_type");

        switch (typeStr) {
            case "ART":
                String painter = rs.getString("painter");
                String artStyle = rs.getString("art_style");
                return new Art(id, name, startingPrice, description, yearCreated, sellerId, imageUrl, status, createdAt, painter, artStyle);

            case "ELECTRONICS":
                String brand = rs.getString("brand");

                int warrantyMonths = rs.getInt("warranty_months");
                if (rs.wasNull()) warrantyMonths = 0;

                return new Electronics(id, name, startingPrice, description, yearCreated, sellerId, imageUrl, status, createdAt, brand, warrantyMonths);

            case "VEHICLES":
                String model = rs.getString("model");
                String engineType = rs.getString("engine_type");
                String licensePlate = rs.getString("license_plate");

                double kmAge = rs.getDouble("km_age");
                if (rs.wasNull()) kmAge = 0.0;

                return new Vehicle(id, name, startingPrice, description, yearCreated, sellerId, imageUrl, status, createdAt, model, engineType, licensePlate, kmAge);

            default:
                throw new SQLException("Lỗi: Loại vật phẩm không xác định trong DB: " + typeStr);
        }
    }
}