package com.auction.manage;

import com.auction.exception.AuthErrorCode;
import com.auction.exception.AuthenticationException;
import com.auction.models.User.User;
import com.auction.enums.UserRole;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Bộ quản lý thông tin tài khoản người dùng trên bộ đệm RAM để tối ưu hóa truy xuất.
 */
public class UserManage {
    private static volatile UserManage instance;

    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, String> usernameToIdMap = new ConcurrentHashMap<>();
    private final Map<String, String> emailToIdMap = new ConcurrentHashMap<>();

    private UserManage() {}

    public static UserManage getInstance() {
        UserManage temp = instance;
        if (temp == null) {
            synchronized (UserManage.class) {
                temp = instance;
                if (temp == null) {
                    temp = instance = new UserManage();
                }
            }
        }
        return temp;
    }

    /**
     * Thêm người dùng mới vào hệ thống (Khi Đăng nhập hoặc Đăng ký thành công)
     */
    public boolean addUser(User user) throws AuthenticationException {
        if (user == null) {
            throw new AuthenticationException(AuthErrorCode.USER_NOT_FOUND);
        }
        if (user.getUsername() == null || user.getEmail() == null) {
            throw new AuthenticationException(AuthErrorCode.USER_NOT_FOUND);
        }

        this.isUsernameExists(user.getUsername());
        this.isEmailExists(user.getEmail());

        String id = user.getId();

        users.put(id, user);
        usernameToIdMap.put(user.getUsername(), id);
        emailToIdMap.put(user.getEmail(), id);

        System.out.println("[UserManage] ✅ Nạp người dùng lên RAM thành công: " + user.getUsername());
        return true;
    }

    /**
     * Cập nhật thông tin người dùng
     * 🔥 SỬA LỖI CHÍ MẠNG: Khóa cục bộ theo ID, cập nhật gián tiếp qua Setter để bảo vệ tính toàn vẹn Maps và vùng nhớ tham chiếu
     */
    public boolean updateUser(String userId, User updatedUser) throws AuthenticationException {
        if (userId == null || updatedUser == null) return false;

        this.isUserIdInvalid(userId);

        synchronized (userId.intern()) {
            User oldUser = users.get(userId);

            String oldUsername = oldUser.getUsername();
            String oldEmail = oldUser.getEmail();
            String newUsername = updatedUser.getUsername();
            String newEmail = updatedUser.getEmail();

            if (!newUsername.equals(oldUsername)) {
                this.isUsernameExists(newUsername);
            }
            if (!newEmail.equals(oldEmail)) {
                this.isEmailExists(newEmail);
            }

            if (!newUsername.equals(oldUsername)) {
                usernameToIdMap.remove(oldUsername);
                usernameToIdMap.put(newUsername, userId);
            }
            if (!newEmail.equals(oldEmail)) {
                emailToIdMap.remove(oldEmail);
                emailToIdMap.put(newEmail, userId);
            }

            oldUser.setUsername(newUsername);
            oldUser.setEmail(newEmail);
            oldUser.setPassword(updatedUser.getPassword());

            System.out.println("[UserManage] 🔄 Cập nhật thông tin người dùng thành công: " + newUsername);
            return true;
        }
    }

    /**
     * Xóa người dùng khỏi hệ thống RAM (Gọi khi USER LOGOUT hoặc Socket Disconnect hoàn toàn)
     */
    public boolean deleteUser(String userId) throws AuthenticationException {
        if (userId == null) return false;

        this.isUserIdInvalid(userId);

        synchronized (userId.intern()) {
            User user = users.get(userId);
            String username = user.getUsername();
            String email = user.getEmail();

            users.remove(userId);
            usernameToIdMap.remove(username);
            emailToIdMap.remove(email);

            System.out.println("[UserManage] 🧹 Trục xuất người dùng khỏi RAM thành công: " + username);
            return true;
        }
    }

    public User getUser(String userId) {
        return userId != null ? users.get(userId) : null;
    }

    public User getUserByUsername(String username) {
        if (username == null) return null;
        String userId = usernameToIdMap.get(username);
        return userId != null ? users.get(userId) : null;
    }

    public User getUserByEmail(String email) {
        if (email == null) return null;
        String userId = emailToIdMap.get(email);
        return userId != null ? users.get(userId) : null;
    }

    public User getUserById(String id) {
        return id != null ? users.get(id) : null;
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public List<User> getUsersByRole(UserRole role) {
        if (role == null) return new ArrayList<>();
        return users.values().stream()
                .filter(user -> role.name().equals(user.getRole()))
                .collect(Collectors.toList());
    }

    public int getUserCount() {
        return users.size();
    }

    private void isUserIdInvalid(String userId) throws AuthenticationException {
        if (userId == null || userId.isEmpty() || !users.containsKey(userId))
            throw new AuthenticationException(AuthErrorCode.USER_NOT_FOUND);
    }

    public void isUsernameExists(String username) throws AuthenticationException {
        if (username != null && usernameToIdMap.containsKey(username))
            throw new AuthenticationException(AuthErrorCode.USERNAME_ALREADY_EXISTS);
    }

    public void isEmailExists(String email) throws AuthenticationException {
        if (email != null && emailToIdMap.containsKey(email))
            throw new AuthenticationException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
    }
}