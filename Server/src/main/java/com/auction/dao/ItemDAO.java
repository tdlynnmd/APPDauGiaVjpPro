package com.auction.dao;

import com.auction.models.Item.Item;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Interface định nghĩa các thao tác truy vấn cơ sở dữ liệu đối với vật phẩm đấu giá.
 */
public interface ItemDAO {
    boolean insertItem(Connection conn, Item item) throws SQLException;
    Optional<Item> findById(String id);
    List<Item> findBySellerId(String sellerId);

    boolean updateStatus(Connection conn, String itemId, String newStatus) throws SQLException;

    boolean updateItem(Connection conn, Item item) throws SQLException;

    boolean softDelete(Connection conn, String itemId) throws SQLException;

    List<Item> findAllPaginated(int limit, int offset);

    long countAllItems();
}