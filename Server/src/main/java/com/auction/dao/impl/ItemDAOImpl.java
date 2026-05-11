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

    @Override
    public boolean insertItem(Item item) {
        String sql = "INSERT INTO items (id, item_type, seller_id, name, description, starting_price, year_created, image_url, status, " +
                "painter, art_style, brand, warranty_months, model, km_age, license_plate, engine_type) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // 1. Set các trường CHUNG (Common fields)
            stmt.setString(1, item.getId());
            stmt.setString(2, item.getItemType().name());
            stmt.setString(3, item.getSellerId());
            stmt.setString(4, item.getName());
            stmt.setString(5, item.getDescription());
            stmt.setDouble(6, item.getStartingPrice());

            // 🔥 SỬA LỖI: Xử lý giá trị int có thể mang ý nghĩa NULL
            // Quy ước: Nếu yearCreated <= 0, ta coi như chưa biết năm sáng tác -> Đẩy NULL xuống DB
            if (item.getYearCreated() > 0) {
                stmt.setInt(7, item.getYearCreated());
            } else {
                stmt.setNull(7, Types.INTEGER);
            }

            stmt.setString(8, item.getImageUrl());
            stmt.setString(9, item.getStatus() != null ? item.getStatus().name() : "ACTIVE"); // Dùng .name() thay vì .toString() cho an toàn với Enum

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
                    // String có thể so sánh null bình thường
                    if (art.getPainter() != null) stmt.setString(10, art.getPainter());
                    if (art.getArtStyle() != null) stmt.setString(11, art.getArtStyle());
                }
                case Electronics elec -> {
                    if (elec.getBrand() != null) stmt.setString(12, elec.getBrand());

                    // Quy ước: Nếu bảo hành <= 0 thì coi là không có thông tin (NULL)
                    if (elec.getWarrantyMonths() > 0) {
                        stmt.setInt(13, elec.getWarrantyMonths());
                    }
                }
                case Vehicle vehicle -> {
                    if (vehicle.getModel() != null) stmt.setString(14, vehicle.getModel());

                    // kmAge là double, quy ước nếu < 0 thì coi là NULL
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

        } catch (SQLException e) {
            System.err.println("Lỗi Insert Item: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Optional<Item> findById(String id) {
        String sql = "SELECT * FROM items WHERE id = ? AND deleted_at IS NULL";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSetToItem(rs));
            }
        } catch (SQLException e) {
            System.err.println("Lỗi Find Item: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Item> findBySellerId(String sellerId) {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE seller_id = ? AND deleted_at IS NULL ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sellerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToItem(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi Find Items by Seller: " + e.getMessage());
        }
        return items;
    }

    @Override
    public boolean updateStatus(String itemId, String newStatus) {
        String sql = "UPDATE items SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newStatus);
            stmt.setString(2, itemId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi Update Item Status: " + e.getMessage());
            return false;
        }
    }

    /**
     * 🔥 Helper method: Xử lý Đa hình (Polymorphism) Hydration
     * Ánh xạ chính xác theo Constructor của Art, Electronics, Vehicle
     */
    private Item mapResultSetToItem(ResultSet rs) throws SQLException {
        // 1. Đọc các trường CHUNG
        String id = rs.getString("id");
        String name = rs.getString("name");
        double startingPrice = rs.getDouble("starting_price");
        String description = rs.getString("description");

        // 🔥 SỬA LỖI ĐỌC NULL: Đọc int, sau đó kiểm tra xem nó có thực sự là NULL trong DB không
        int yearCreated = rs.getInt("year_created");
        if (rs.wasNull()) {
            yearCreated = 0; // Gán lại thành 0 (hoặc -1) nếu bạn muốn Model hiểu là "Không có năm"
        }

        String sellerId = rs.getString("seller_id");
        String imageUrl = rs.getString("image_url");
        ItemStatus status = ItemStatus.valueOf(rs.getString("status"));
        java.time.LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        String typeStr = rs.getString("item_type");

        // 2. Dựa vào TYPE để gọi constructor
        switch (typeStr) {
            case "ART":
                String painter = rs.getString("painter");
                String artStyle = rs.getString("art_style");
                return new Art(id, name, startingPrice, description, yearCreated, sellerId, imageUrl, status, createdAt, painter, artStyle);

            case "ELECTRONICS":
                String brand = rs.getString("brand");

                // Khắc phục tương tự cho warrantyMonths
                int warrantyMonths = rs.getInt("warranty_months");
                if (rs.wasNull()) warrantyMonths = 0;

                return new Electronics(id, name, startingPrice, description, yearCreated, sellerId, imageUrl, status, createdAt, brand, warrantyMonths);

            case "VEHICLES":
                String model = rs.getString("model");
                String engineType = rs.getString("engine_type");
                String licensePlate = rs.getString("license_plate");

                // Khắc phục tương tự cho kmAge
                double kmAge = rs.getDouble("km_age");
                if (rs.wasNull()) kmAge = 0.0;

                return new Vehicle(id, name, startingPrice, description, yearCreated, sellerId, imageUrl, status, createdAt, model, engineType, licensePlate, kmAge);

            default:
                throw new SQLException("Lỗi: Loại vật phẩm không xác định trong DB: " + typeStr);
        }
    }
}