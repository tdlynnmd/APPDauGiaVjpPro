package com.auction.server.service;

import com.auction.server.manage.UserManage;
import com.auction.server.models.User.User;
import com.auction.server.models.User.UserRole;

public class AuthService {
    private final UserManage userManage = UserManage.getInstance();
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    private static final String USERNAME_REGEX = "^[A-Za-z0-9._]{5,20}$";
    private static final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";


    /** Cần đưa sang service
     * Xác thực đăng nhập     * @param usernameOrEmail Username hoặc email     * @param password Mật khẩu     * @return User nếu đăng nhập thành công, null nếu thất bại
     */
    /*public User authenticate(String usernameOrEmail, String password) {
        if (!this.validateUserInfo(username,)) {
            return null;
        }

        String userId = usernameToIdMap.get(usernameOrEmail);
        if (userId == null) {
            userId = emailToIdMap.get(usernameOrEmail);
        }

        if (userId == null) {
            return null;
        }

        User user = users.get(userId);
        return user.checkpassword(password) ? user : null;
    }*/

    //đăng ký
    public boolean register (String username, String password, String email, UserRole role){
        if (!(this.validateUserInfo(username, email, password))) {
            return false;
        }

        if(this.userManage.isUsernameExists(username) || this.userManage.isEmailExists(email)){
            System.out.println("Lỗi: Bạn đã có tài khoản");
            return false;
        }
        String hashedPassword = this.hashPassword(password);

        return true;
    }

    /**
     * Kiểm tra email hợp lệ - Ít nhất 5 ký tự - nhiều nhất 20 ký tự, bao gồm chữ cái, chữ số và . , _
     */
    private boolean isEmailFormatValid(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return email.matches(EMAIL_REGEX);
    }

    /**
     * Kiểm tra username hợp lệ - Ít nhất 5 ký tự - nhiều nhất 20 ký tự, bao gồm chữ cái, chữ số và . , _
     */
    private boolean isUsernameFormatValid(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        return username.matches(USERNAME_REGEX);
    }

    /**
     * Kiểm tra mật khẩu hợp lệ - Ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt
     */
    private boolean isPasswordValid(String password) {
        if(password == null && password.isEmpty()) {
            return false;
        };
        return password.matches(PASSWORD_REGEX);
    }

    //Check tổng hợp
    private boolean validateUserInfo(String username, String email, String password){
        return this.isUsernameFormatValid(username) && this.isEmailFormatValid(email) && this.isPasswordValid(password);
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


