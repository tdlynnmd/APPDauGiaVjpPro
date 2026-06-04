package com.auction.dao;

import com.auction.models.User.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Interface định nghĩa các thao tác truy vấn cơ sở dữ liệu đối với thông tin tài khoản và ví tiền.
 */
public interface UserDAO {
    boolean insertUser(Connection conn, User user) throws SQLException;

    Optional<User> findById(String id);

    java.util.Map<String, String> findUsernamesByIds(List<String> ids);

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    boolean freezeMoney(Connection conn, String userId, double amount) throws SQLException;

    void unfreezeMoney(Connection conn, String userId, double amount) throws SQLException;

    boolean deductFrozenMoney(Connection conn, String userId, double amount) throws SQLException;

    boolean addAvailableBalance(Connection conn, String userId, double amount) throws SQLException;

    boolean withdrawAvailableBalance(Connection conn, String userId, double amount) throws SQLException;

    boolean addJoinedAuction(Connection conn, String userId, String auctionId) throws SQLException;

    void removeJoinedAuction(Connection conn, String userId, String auctionId) throws SQLException;

    List<User> findPaginated(int limit, int offset);

    long countTotalUsers();

    boolean updateStatus(Connection conn, String userId, String name) throws SQLException;

    boolean updateProfile(Connection conn, String userId, String username, String email) throws SQLException;

    boolean updatePassword(Connection conn, String userId, String hashedPassword) throws SQLException;
}