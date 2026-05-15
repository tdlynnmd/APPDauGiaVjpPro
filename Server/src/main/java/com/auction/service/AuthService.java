package com.auction.service;

import com.auction.dao.UserDAO;
import com.auction.dao.impl.UserDAOImpl;
import com.auction.enums.UserRole;
import com.auction.exception.AuthenticationException;
import com.auction.exception.AuthErrorCode;
import com.auction.dto.*;
import com.auction.manage.UserManage;
import com.auction.models.User.*;

import java.util.Optional;

public class AuthService {
    private final UserManage userManage = UserManage.getInstance();
    private final UserDAO userDAO = new UserDAOImpl(); // Tích hợp DAO

    private static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    private static final String USERNAME_REGEX = "^[A-Za-z0-9._]{5,20}$";
    private static final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";

    /**
     * Đăng ký người dùng mới
     * Luồng: Validate -> Factory tạo Object -> Lưu DB -> Thêm vào RAM -> Trả về DTO
     */
    public UserDTO register(String username, String password, String email, UserRole role) throws AuthenticationException {
        // 1. Kiểm tra định dạng đầu vào
        validateUsername(username);
        validateEmail(email);
        validatePassword(password);

        // 2. Kiểm tra trùng lặp trong Database (Thông qua DAO)
        if (userDAO.findByUsername(username).isPresent()) {
            throw new AuthenticationException(AuthErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (userDAO.findByEmail(email).isPresent()) {
            throw new AuthenticationException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 3. Tạo Object User qua Factory
        User newUser = UserFactory.createUser(role, username, email, password);

        // 4. Lưu vào Database trước
        boolean isSaved = userDAO.insertUser(newUser);
        if (!isSaved) {
            throw new AuthenticationException(AuthErrorCode.REGISTRATION_FAILED);
        }

        // 5. Sau khi DB thành công, đưa vào RAM (UserManage) để quản lý online
        userManage.addUser(newUser);

        return this.convertUserToDTO(newUser);
    }

    /**
     * Đăng nhập
     * Luồng: Tìm trong RAM (nếu đang online) -> Không có thì tìm trong DB -> Check Password -> Trả về DTO
     */
    public UserDTO login(String usernameOrEmail, String password) throws AuthenticationException {
        if (usernameOrEmail == null || password == null || usernameOrEmail.isEmpty() || password.isEmpty()) {
            throw new AuthenticationException(AuthErrorCode.INPUT_NULL_EMPTY);
        }

        // 1. Tìm User (Ưu tiên tìm trong RAM trước, nếu không có thì truy vấn DB)
        User user = userManage.getUserByUsername(usernameOrEmail);
        if (user == null) {
            user = userManage.getUserByEmail(usernameOrEmail);
        }

        if (user == null) {
            // Nếu RAM chưa có, tìm trong Database
            Optional<User> userOpt = usernameOrEmail.contains("@")
                    ? userDAO.findByEmail(usernameOrEmail)
                    : userDAO.findByUsername(usernameOrEmail);

            if (userOpt.isEmpty()) {
                throw new AuthenticationException(AuthErrorCode.INVALID_CREDENTIALS);
            }
            user = userOpt.get();
        }

        // 2. Kiểm tra mật khẩu (Sử dụng BCrypt đã có trong User entity)
        if (!user.checkPassword(password)) {
            throw new AuthenticationException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        // 3. Nếu đăng nhập thành công mà user chưa có trên RAM, thì đưa lên RAM
        if (userManage.getUserById(user.getId()) == null) {
            userManage.addUser(user);
        }

        return this.convertUserToDTO(user);
    }

    /**
     * Đăng xuất
     */
    public void logout(String userId) throws AuthenticationException {
        if (userId == null || userId.isEmpty()) {
            throw new AuthenticationException(AuthErrorCode.USER_NOT_FOUND);
        }
        // Xóa khỏi danh sách quản lý online trên RAM
        userManage.deleteUser(userId);
    }

    // --- CÁC HÀM VALIDATE GIỮ NGUYÊN ---
    private void validateEmail(String email) throws AuthenticationException { /*...*/ }
    private void validateUsername(String username) throws AuthenticationException { /*...*/ }
    private void validatePassword(String password) throws AuthenticationException { /*...*/ }

    /**
     * Chuyển đổi User entity thành UserDTO
     * Cập nhật để hỗ trợ availableBalance và frozenBalance
     */
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
                    bidder.getAvailableBalance(), // Sửa ở đây
                    bidder.getFrozenBalance(),    // Thêm ở đây
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
                    seller.getAvailableBalance(), // Sửa ở đây
                    seller.getFrozenBalance(),    // Thêm ở đây
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