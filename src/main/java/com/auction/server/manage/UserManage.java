package com.auction.server.manage;

import com.auction.server.exception.AuthErrorCode;
import com.auction.server.exception.AuthenticationException;
import com.auction.server.models.User.User;
import com.auction.server.models.User.UserRole;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UserManage {
    private static volatile UserManage instance;

    // Sử dụng ConcurrentHashMap để thread-safe
    private final Map<String, User> users; // id -> user
    private final Map<String, String> usernameToIdMap; // username -> userId
    private final Map<String, String> emailToIdMap;    // email -> userId

    private UserManage() {
        this.users = new HashMap<>();
        this.usernameToIdMap = new HashMap<>();
        this.emailToIdMap = new HashMap<>();
    }

    /**
     * Lấy instance duy nhất của UserManage (Double-Checked Locking)
     */
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
     * Thêm người dùng mới vào hệ thống
     * * @param user Người dùng cần thêm
     * * @return true nếu thêm thành công, false nếu username/email đã tồn tại
     */
    public synchronized boolean addUser(User user) throws AuthenticationException {

        this.isUsernameExists(user.getUsername()); // Kiểm tra username đã tồn tại, nếu có sẽ ném AuthenticationException
        this.isEmailExists(user.getEmail());       // Kiểm tra email đã tồn tại, nếu có sẽ ném AuthenticationException

        String id = user.getId();
        users.put(id, user);
        usernameToIdMap.put(user.getUsername(), id);
        emailToIdMap.put(user.getEmail(), id);

        System.out.println("Thêm người dùng thành công");
        return true;
    }

    /** Cần sửa lại,
     * Cập nhật thông tin người dùng     * @param userId ID của người dùng cần cập nhật     * @param updatedUser Người dùng mới với thông tin cập nhật     * @return true nếu cập nhật thành công, false nếu người dùng không tồn tại
     */
    public synchronized boolean updateUser(String userId, User updatedUser) throws AuthenticationException {

        this.isUserIdInvalid(userId); // Kiểm tra ID hợp lệ, nếu không sẽ ném AuthenticationException

        User oldUser = users.get(userId);
        String oldUsername = oldUser.getUsername();
        String oldEmail = oldUser.getEmail();
        String newUsername = updatedUser.getUsername();
        String newEmail = updatedUser.getEmail();

        // Kiểm tra username mới không trùng với user khác (nếu thay đổi)
        if (!newUsername.equals(oldUsername)){
            this.isUsernameExists(newUsername);
        }

        // Kiểm tra email mới không trùng với user khác (nếu thay đổi)
        if (!newEmail.equals(oldEmail)){
            this.isEmailExists(newEmail);
        }

        // Cập nhật maps
        usernameToIdMap.remove(oldUsername);
        emailToIdMap.remove(oldEmail);
        usernameToIdMap.put(newUsername, userId);
        emailToIdMap.put(newEmail, userId);

        updatedUser.setId(userId); // Giữ nguyên ID
        users.put(userId, updatedUser);

        System.out.println("Cập nhật người dùng thành công: " + newUsername);
        return true;
    }

    /**
     * Xóa người dùng khỏi hệ thống     * @param userId ID của người dùng cần xóa     * @return true nếu xóa thành công, false nếu người dùng không tồn tại
     */
    public synchronized boolean deleteUser(String userId) throws AuthenticationException {

        //check có tồn tại id này ko
        this.isUserIdInvalid(userId); // Kiểm tra ID hợp lệ, nếu không sẽ ném AuthenticationException

        User user = users.get(userId);
        String username = user.getUsername();
        String email = user.getEmail();

        users.remove(userId);
        usernameToIdMap.remove(username);
        emailToIdMap.remove(email);

        System.out.println("Xóa người dùng thành công: " + username);
        return true;
    }



    /**
     * Lấy người dùng theo ID     * @param userId ID của người dùng     * @return User nếu tìm thấy, null nếu không tồn tại
     */
    public User getUser(String userId) {
        return users.get(userId);
    }

    /**
     * Lấy người dùng theo username     * @param username Username của người dùng     * @return User nếu tìm thấy, null nếu không tồn tại
     */
    public User getUserByUsername(String username) {
        String userId = usernameToIdMap.get(username);
        return userId != null ? users.get(userId) : null;
    }

    /**
     * Lấy người dùng theo email     * @param email Email của người dùng     * @return User nếu tìm thấy, null nếu không tồn tại
     */
    public User getUserByEmail(String email) {
        String userId = emailToIdMap.get(email);
        return userId != null ? users.get(userId) : null;
    }

    /**
     * Lấy tất cả người dùng     * @return Danh sách tất cả người dùng
     */
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    /**
     * Lấy người dùng theo vai trò     * @param role Vai trò cần lọc     * @return Danh sách người dùng có vai trò đó
     */
    public List<User> getUsersByRole(UserRole role) {
        return users.values().stream()
                .filter(user -> user.getRole().equals(role.toString()))
                .collect(Collectors.toList());
    }

    /**
     * Lấy số lượng người dùng hiện có     * @return Số lượng người dùng
     */
    public int getUserCount() {
        return users.size();
    }

    //Kiểm tra id truyền vào hợp lệ ko
    private void isUserIdInvalid(String userId) throws AuthenticationException {
        if(userId == null || userId.isEmpty() || !users.containsKey(userId))
            throw new AuthenticationException(AuthErrorCode.USER_NOT_FOUND);
    }

    //Kiểm tra username tồn tại chưa
    public void isUsernameExists(String username) throws AuthenticationException {
        if (usernameToIdMap.containsKey(username))
            throw new AuthenticationException(AuthErrorCode.USERNAME_ALREADY_EXISTS);
    }

    //Kiểm tra email tồn tại chưa
    public void isEmailExists(String email) throws AuthenticationException {
        if (emailToIdMap.containsKey(email))
            throw new AuthenticationException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
    }
}
