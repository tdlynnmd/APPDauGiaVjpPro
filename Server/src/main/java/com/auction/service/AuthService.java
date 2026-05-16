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
      Đăng ký người dùng mới.

      Luồng xử lý:
      1. Validate username, email, password, role.
      2. Kiểm tra username/email đã tồn tại trong database chưa.
      3. Gọi UserFactory để tạo đúng subclass.
      4. Lưu user vào database.
      5. Nếu lưu DB thành công, đưa user vào RAM qua UserManage.
      6. Convert User entity thành UserDTO để trả về Client.
     */
    public UserDTO register(String username, String password, String email, UserRole role)
            throws AuthenticationException {

        // Server bắt buộc validate lại, không tin hoàn toàn dữ liệu từ Client.
        validateUsername(username);
        validateEmail(email);
        validatePassword(password);

        // Role không được null.
        // Nếu null, UserFactory sẽ không biết tạo loại user nào.
        if (role == null) {
            throw new AuthenticationException(AuthErrorCode.ROLE_INVALID);
        }

        // Kiểm tra trùng username trong database.
        if (userDAO.findByUsername(username).isPresent()) {
            throw new AuthenticationException(AuthErrorCode.USERNAME_ALREADY_EXISTS);
        }

        // Kiểm tra trùng email trong database.
        if (userDAO.findByEmail(email).isPresent()) {
            throw new AuthenticationException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // Tạo object User theo đúng role.
        // BIDDER -> Bidder
        // SELLER -> Seller
        // ADMIN  -> Admin
        User newUser = UserFactory.createUser(role, username, email, password);

        // Lưu database trước.
        // Nếu DB lỗi thì không đưa user vào RAM.
        boolean isSaved = userDAO.insertUser(newUser);
        if (!isSaved) {
            throw new AuthenticationException(AuthErrorCode.REGISTRATION_FAILED);
        }

        // Sau khi DB lưu thành công, đưa user vào RAM để dùng nhanh.
        userManage.addUser(newUser);

        // Trả về DTO an toàn, không chứa password/hash password.
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
      Đăng xuất user khỏi hệ thống.

      Vai trò:
      - Kiểm tra userId có hợp lệ không.
      - Kiểm tra user có tồn tại trong RAM hoặc database không.

     * Lưu ý:
      - AuthService KHÔNG xóa ClientSession khỏi ConnectionManage.
      - RequestDispatcher mới là nơi đang cầm ClientSession hiện tại,
        nên RequestDispatcher sẽ gọi ConnectionManage.removeConnection().
     */
    public void logout(String userId) throws AuthenticationException {
        if (userId == null || userId.isBlank()) {
            throw new AuthenticationException(AuthErrorCode.USER_NOT_FOUND);
        }

        // Kiểm tra trong RAM trước.
        User user = userManage.getUserById(userId);

        // Nếu RAM chưa có thì kiểm tra trong database.
        if (user == null && userDAO.findById(userId).isEmpty()) {
            throw new AuthenticationException(AuthErrorCode.USER_NOT_FOUND);
        }

        // Không gọi userManage.deleteUser(userId) ở đây.
        // Vì logout là xóa connection/session online, không phải xóa user khỏi hệ thống.
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