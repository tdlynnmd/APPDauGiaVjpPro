package com.auction.dao;

import com.auction.models.Item.Item;
import java.util.List;
import java.util.Optional;

public interface ItemDAO {
    boolean insertItem(Item item);
    Optional<Item> findById(String id);
    List<Item> findBySellerId(String sellerId);
    boolean updateStatus(String itemId, String newStatus);
}