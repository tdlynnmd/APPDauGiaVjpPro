package com.auction.server.service;

import com.auction.server.exception.AuthErrorCode;
import com.auction.server.exception.AuthenticationException;
import com.auction.server.manage.ConnectionManage;
import com.auction.server.manage.UserManage;
import com.auction.server.models.User.User;
import com.auction.server.models.User.UserFactory;
import com.auction.server.models.User.UserRole;

public class AuthService {
    private final UserManage userManage = UserManage.getInstance();
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    private static final String USERNAME_REGEX = "^[A-Za-z0-9._]{5,20}$";
    private static final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";

    //đăng ký
    public <T extends User> T register (String username, String password, String email, UserRole role) throws AuthenticationException {

        //Kiểm tra hợp lệ
        this.validateUsername(username);
        this.validateEmail(email);
        this.validatePassword(password);

        //Mã hoá
        String hashedPassword = this.hashPassword(password);

        //Tạo
        T newUser = UserFactory.createUser(role, username, email, hashedPassword);

        //Thêm vào Map của userManage
        this.userManage.addUser(newUser);

        //Luu vào DataBase (nếu có)

        return newUser;
    }

    //Đăng nhập
    public User login(String usernameOrEmail, String password) throws AuthenticationException {
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
        String hashedInputPassword = this.hashPassword(password);
        if (!user.checkpassword(hashedInputPassword)) {
            throw new AuthenticationException(AuthErrorCode.INVALID_CREDENTIALS);// Exception chung
        }

        //Thiết lập Online
        ConnectionManage.getInstance().registerOnline(user);

        return user;
    }


    //Đăng xuất
    public void logout(User user) throws AuthenticationException {
        //Xoá người dùng khỏi session, cập nhập trạng thái người dùng
        //Dọn dẹp tài nguyên, xoá thread
        //Thông báo đăng xuât thành công
        //Ghi log
        if(user == null)
            throw new AuthenticationException(AuthErrorCode.USER_NULL);

        ConnectionManage.getInstance().removeOffline(user.getId());
    }


    /**
     * Kiểm tra email hợp lệ - Ít nhất 5 ký tự - nhiều nhất 20 ký tự, bao gồm chữ cái, chữ số và . , _
     */
    private boolean isEmailFormatValid(String email) {
        return email.matches(EMAIL_REGEX);
    }

    private void validateEmail(String email) throws AuthenticationException {
        if (email == null)
            throw new AuthenticationException(AuthErrorCode.EMAIL_NULL_EMPTY);
        if (!email.matches(EMAIL_REGEX))
            throw new AuthenticationException(AuthErrorCode.EMAIL_INVALID_FORMAT);
    }

    /**
     * Kiểm tra username hợp lệ - Ít nhất 5 ký tự - nhiều nhất 20 ký tự, bao gồm chữ cái, chữ số và . , _
     */
    private void validateUsername(String username) throws AuthenticationException {
        if (username == null)
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
        if (password == null)
            throw new AuthenticationException(AuthErrorCode.PASSWORD_NULL_EMPTY);
        if (password.length() < 8)
            throw new AuthenticationException(AuthErrorCode.PASSWORD_TOO_SHORT);
        if (!password.matches(PASSWORD_REGEX))
            throw new AuthenticationException(AuthErrorCode.PASSWORD_WEAK);
    }

    //Mã hoá password
    private String hashPassword(String password) {
        try {
            // Sử dụng thuật toán SHA-256 có sẵn trong Java
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Chuyển đổi mảng byte sang chuỗi Hex để lưu trữ
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return null; // Hoặc ném RuntimeException
        }
    }

}


