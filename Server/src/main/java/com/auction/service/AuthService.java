package com.auction.service;

import com.auction.dao.UserDAO;
import com.auction.dao.impl.UserDAOImpl;
import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;
import com.auction.exception.AuthenticationException;
import com.auction.exception.AuthErrorCode;
import com.auction.dto.*;
import com.auction.manage.UserManage;
import com.auction.models.User.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Dịch vụ xử lý đăng nhập, đăng ký và đăng xuất của người dùng.
 */
public class AuthService {
    private final UserManage userManage = UserManage.getInstance();
    private final UserDAO userDAO = new UserDAOImpl();

    private static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    private static final String USERNAME_REGEX = "^[A-Za-z0-9._]{5,20}$";
    private static final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";

    public UserDTO register(String username, String password, String email, UserRole role)
            throws AuthenticationException {

        validateUsername(username);
        validateEmail(email);
        validatePassword(password);

        if (role == null) {
            throw new AuthenticationException(AuthErrorCode.ROLE_INVALID);
        }

        if (role == UserRole.ADMIN) {
            throw new AuthenticationException(AuthErrorCode.ROLE_INVALID);
        }

        if (userDAO.findByUsername(username).isPresent()) {
            throw new AuthenticationException(AuthErrorCode.USERNAME_ALREADY_EXISTS);
        }

        if (userDAO.findByEmail(email).isPresent()) {
            throw new AuthenticationException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User newUser = UserFactory.createUser(role, username, email, password);

        try (Connection conn = com.auction.config.DatabaseConnection.getConnection()) {

            boolean isSaved = userDAO.insertUser(conn, newUser);
            if (!isSaved) {
                throw new AuthenticationException(AuthErrorCode.REGISTRATION_FAILED);
            }

        } catch (SQLException e) {
            throw new AuthenticationException(AuthErrorCode.REGISTRATION_FAILED);
        }

        userManage.addUser(newUser);

        return this.convertUserToDTO(newUser);
    }

    public UserDTO login(String usernameOrEmail, String password) throws AuthenticationException {
        if (usernameOrEmail == null || password == null || usernameOrEmail.isEmpty() || password.isEmpty()) {
            throw new AuthenticationException(AuthErrorCode.INPUT_NULL_EMPTY);
        }

        User user = userManage.getUserByUsername(usernameOrEmail);
        if (user == null) {
            user = userManage.getUserByEmail(usernameOrEmail);
        }

        if (user == null) {
            Optional<User> userOpt = usernameOrEmail.contains("@")
                    ? userDAO.findByEmail(usernameOrEmail)
                    : userDAO.findByUsername(usernameOrEmail);

            if (userOpt.isEmpty()) {
                throw new AuthenticationException(AuthErrorCode.INVALID_CREDENTIALS);
            }
            user = userOpt.get();
        }

        if(user.getStatus() == UserStatus.BANNED) {
            throw new AuthenticationException(AuthErrorCode.ACCOUNT_LOCKED);
        }

        if (!user.checkPassword(password)) {
            throw new AuthenticationException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        if (userManage.getUserById(user.getId()) == null) {
            userManage.addUser(user);
        }

        return this.convertUserToDTO(user);
    }

    public void logout(String userId) throws AuthenticationException {
        if (userId == null || userId.trim().isEmpty()) {
            throw new AuthenticationException(AuthErrorCode.USER_NOT_FOUND);
        }

        User user = userManage.getUserById(userId);

        if (user == null && userDAO.findById(userId).isEmpty()) {
            throw new AuthenticationException(AuthErrorCode.USER_NOT_FOUND);
        }

    }

    private void validateEmail(String email) throws AuthenticationException {
        if (email == null || email.isEmpty())
            throw new AuthenticationException(AuthErrorCode.EMAIL_NULL_EMPTY);
        if (!email.matches(EMAIL_REGEX))
            throw new AuthenticationException(AuthErrorCode.EMAIL_INVALID_FORMAT);
    }

    private void validateUsername(String username) throws AuthenticationException {
        if (username == null || username.isEmpty())
            throw new AuthenticationException(AuthErrorCode.USERNAME_NULL_EMPTY);
        if (username.length() < 5)
            throw new AuthenticationException(AuthErrorCode.USERNAME_TOO_SHORT);
        if (username.length() > 20)
            throw new AuthenticationException(AuthErrorCode.USERNAME_TOO_LONG);
        if (!username.matches(USERNAME_REGEX))
            throw new AuthenticationException(AuthErrorCode.USERNAME_INVALID_FORMAT);
    }

    private void validatePassword(String password) throws AuthenticationException {
        if (password == null || password.isEmpty())
            throw new AuthenticationException(AuthErrorCode.PASSWORD_NULL_EMPTY);
        if (password.length() < 8)
            throw new AuthenticationException(AuthErrorCode.PASSWORD_TOO_SHORT);
        if (!password.matches(PASSWORD_REGEX))
            throw new AuthenticationException(AuthErrorCode.PASSWORD_WEAK);
    }

    public UserDTO convertUserToDTO(User user) {
        if (user == null) return null;
        UserRole role = user.getUserRole();

        if (UserRole.BIDDER.equals(role)) {
            Bidder bidder = (Bidder) user;
            return new BidderDTO(
                    bidder.getId(),
                    bidder.getUsername(),
                    bidder.getEmail(),
                    UserRole.BIDDER,
                    bidder.getStatus(),
                    bidder.getAvailableBalance(),
                    bidder.getFrozenBalance(),
                    bidder.getJoinedAuctionIds()
            );
        } else if (UserRole.SELLER.equals(role)) {
            Seller seller = (Seller) user;
            return new SellerDTO(
                    seller.getId(),
                    seller.getUsername(),
                    seller.getEmail(),
                    UserRole.SELLER,
                    seller.getStatus(),
                    seller.getAvailableBalance(),
                    seller.getFrozenBalance(),
                    seller.getRating()
            );
        } else if (UserRole.ADMIN.equals(role)) {
            Admin admin = (Admin) user;
            return new AdminDTO(
                    admin.getId(),
                    admin.getUsername(),
                    admin.getEmail(),
                    UserRole.ADMIN,
                    admin.getStatus()
            );
        }
        return null;
    }
}
