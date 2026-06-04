package com.auction.dao;

import com.auction.models.Auction.AutoBid;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Interface định nghĩa các thao tác truy vấn cơ sở dữ liệu đối với cấu hình AutoBid.
 */
public interface AutoBidDAO {
    boolean insertOrUpdate(Connection conn, AutoBid autoBid) throws SQLException;
    Optional<AutoBid> findActiveByUserAndAuction(Connection conn, String userId, String auctionId) throws SQLException;
    List<AutoBid> findActiveByAuctionId(Connection conn, String auctionId) throws SQLException;
    boolean disableAutoBid(Connection conn, String id) throws SQLException;
}
