package com.auction.manage;


import com.auction.models.User.User;

import java.util.HashMap;
import java.util.Map;

public class ConnectionManage {
    private static ConnectionManage instance;

    // Quản lý tập trung: Key là UserId, Value là đối tượng User (hoặc Socket sau này) [cite: 1603, 1604, 2069]
    private final Map<String, User> onlineUsers = new HashMap<>();

    private ConnectionManage() {}

    public static ConnectionManage getInstance() {
        if (instance == null) {
            instance = new ConnectionManage();
        }
        return instance;
    }

    // Logic nghiệp vụ: Đăng ký người dùng online khi họ kết nối thành công [cite: 1604]
    public void registerOnline(User user) {
        if (user != null) {
            onlineUsers.put(user.getId(), user);
            System.out.println("Server: Người dùng " + user.getUsername() + " đã Online.");
        }
    }

    // Logic nghiệp vụ: Xóa trạng thái online khi người dùng logout hoặc mất kết nối [cite: 1605, 2069]
    public void removeOffline(String userId) {
        if (onlineUsers.containsKey(userId)) {
            User user = onlineUsers.remove(userId);
            System.out.println("Server: Người dùng " + user.getUsername() + " đã Offline.");
        }
    }

    // Kiểm tra nhanh ai đang online để phục vụ logic đấu giá
    public boolean isUserOnline(String userId) {
        return onlineUsers.containsKey(userId);
    }

    public int getOnlineCount() {
        return onlineUsers.size();
    }

    // Lấy danh sách để hiển thị lên bảng điều khiển của Admin
    public Map<String, User> getActiveConnections() {
        return onlineUsers;
    }
}
