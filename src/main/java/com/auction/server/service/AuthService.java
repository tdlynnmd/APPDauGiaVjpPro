package com.auction.server.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.auction.server.exception.AuthErrorCode;
import com.auction.server.exception.AuthenticationException;
import com.auction.server.manage.ConnectionManage;
import com.auction.server.manage.UserManage;
import com.auction.server.models.User.User;
import com.auction.server.models.User.UserFactory;
import com.auction.server.models.User.UserRole;
import com.auction.server.models.User.Bidder;
import com.auction.server.models.User.Seller;
import com.auction.server.models.User.Admin;
import com.auction.dto.UserDTO;
import com.auction.dto.BidderDTO;
import com.auction.dto.SellerDTO;
import com.auction.dto.AdminDTO;


public class AuthService {
    private final UserManage userManage = UserManage.getInstance();
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    private static final String USERNAME_REGEX = "^[A-Za-z0-9._]{5,20}$";
    private static final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";


    //REGISTER
    public <T extends User> UserDTO register (String username, String password, String email, UserRole role) throws AuthenticationException {

        //Kiểm tra hợp lệ
        this.validateUsername(username);
        this.validateEmail(email);
        this.validatePassword(password);

        // ✅ THAY ĐỔI: Dùng BCrypt encoder.encode() thay hashPassword()
        String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());

        //Tạo
        T newUser = UserFactory.createUser(role, username, email, hashedPassword);

        //Thêm vào Map của userManage
        this.userManage.addUser(newUser);

        //Luu vào DataBase (nếu có)

        return  this.convertUserToDTO(newUser,role);
    }

    //Đăng nhập
    public UserDTO login(String usernameOrEmail, String password) throws AuthenticationException {
        if(usernameOrEmail == null || password == null || usernameOrEmail.isEmpty() || password.isEmpty()){
            throw new AuthenticationException(AuthErrorCode.INPUT_NULL_EMPTY) ;//Exception ko được để trống
        }

        //Kiểm tra = username hoặc email
        User user = userManage.getUserByUsername(usernameOrEmail);
        if (user == null) {
            user = userManage.getUserByEmail(usernameOrEmail);
        }

        if (user == null) {
           throw new AuthenticationException(AuthErrorCode.INVALID_CREDENTIALS); // Exception chung
        }

        //Kiểm tra mật khẩu
        // matches() tự động extract salt từ stored hash và so sánh
        if (!user.checkPassword(password)) {
            throw new AuthenticationException(AuthErrorCode.INVALID_CREDENTIALS); // Exception chung
        }

        //Thiết lập Online
        ConnectionManage.getInstance().registerOnline(user);

        return this.convertUserToDTO(user,user.getUserRole());
    }


    //Đăng xuất
    public void logout(String userId) throws AuthenticationException {
        //Xoá người dùng khỏi session, cập nhập trạng thái người dùng
        //Dọn dẹp tài nguyên, xoá thread
        //Thông báo đăng xuât thành công
        //Ghi log
        if(userId == null || userId.isEmpty())
            throw new AuthenticationException(AuthErrorCode.USER_NOT_FOUND);

        ConnectionManage.getInstance().removeOffline(userId);
    }


    /**
     * Kiểm tra email hợp lệ - Ít nhất 5 ký tự - nhiều nhất 20 ký tự, bao gồm chữ cái, chữ số và . , _
     */

    private void validateEmail(String email) throws AuthenticationException {
        if (email == null || email.isEmpty())
            throw new AuthenticationException(AuthErrorCode.EMAIL_NULL_EMPTY);
        if (!email.matches(EMAIL_REGEX))
            throw new AuthenticationException(AuthErrorCode.EMAIL_INVALID_FORMAT);
    }

    /**
     * Kiểm tra username hợp lệ - Ít nhất 5 ký tự - nhiều nhất 20 ký tự, bao gồm chữ cái, chữ số và . , _
     */
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

    /**
     * Kiểm tra mật khẩu hợp lệ - Ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt
     */
    private void validatePassword(String password) throws AuthenticationException {
        if (password == null || password.isEmpty())
            throw new AuthenticationException(AuthErrorCode.PASSWORD_NULL_EMPTY);
        if (password.length() < 8)
            throw new AuthenticationException(AuthErrorCode.PASSWORD_TOO_SHORT);
        if (!password.matches(PASSWORD_REGEX))
            throw new AuthenticationException(AuthErrorCode.PASSWORD_WEAK);
    }


    /**
     * Chuyển đổi User entity thành UserDTO tương ứng
     * Tách riêng dữ liệu nhạy cảm (password) khỏi dữ liệu được gửi đến client
     *
     * @param user User entity từ server
     * @return UserDTO tương ứng với role của user (BidderDTO, SellerDTO, AdminDTO)
     */
    public UserDTO convertUserToDTO(User user,UserRole role) {
        if (user == null) {
            return null;
        }

        if (UserRole.BIDDER.equals(role)) {
            Bidder bidder = (Bidder) user;
            return new BidderDTO(
                bidder.getId(),
                bidder.getUsername(),
                bidder.getEmail(),
                UserRole.BIDDER,
                bidder.getBalance(),
                bidder.getJoinedAuctionIds()
            );
        } else if (UserRole.SELLER.equals(role)) {
            Seller seller = (Seller) user;
            return new SellerDTO(
                seller.getId(),
                seller.getUsername(),
                seller.getEmail(),
                UserRole.SELLER,
                seller.getRating()
            );
        } else if (UserRole.ADMIN.equals(role)) {
            Admin admin = (Admin) user;
            return new AdminDTO(
                admin.getId(),
                admin.getUsername(),
                admin.getEmail(),
                UserRole.ADMIN,
                admin.getActionLogs()
            );
        }

        // Fallback: nếu ko match với role cụ thể nào, trả về UserDTO cơ bản
        return new UserDTO(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            UserRole.valueOf(user.getRole())
        );
    }

}


