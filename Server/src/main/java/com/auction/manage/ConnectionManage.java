package com.auction.manage;

import com.auction.network.ClientSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class ConnectionManage {

    // 1. Singleton An toàn đa luồng (Double-Checked Locking)
    private static volatile ConnectionManage instance;

    // 2. Map Đa luồng & Đa thiết bị
    // Key: userId | Value: Một tập hợp (Set) chứa các đường dây mạng của User đó
    private final ConcurrentHashMap<String, Set<ClientSession>> activeConnections;

    private ConnectionManage() {
        this.activeConnections = new ConcurrentHashMap<>();
    }

    public static ConnectionManage getInstance() {
        if (instance == null) {
            synchronized (ConnectionManage.class) {
                if (instance == null) {
                    instance = new ConnectionManage();
                }
            }
        }
        return instance;
    }

    // 3. Đăng ký kết nối mới (Hỗ trợ 1 người đăng nhập nhiều máy)
    public void registerConnection(String userId, ClientSession session) {
        if (userId != null && session != null) {
            // Nếu chưa có user trong Map thì tạo một Set mới (dùng CopyOnWriteArraySet để an toàn đa luồng)
            activeConnections.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
            System.out.println("Server: Thiết bị mới của User [" + userId + "] đã kết nối.");
        }
    }

    // 4. Hủy 1 kết nối cụ thể khi người dùng tắt App ở 1 thiết bị
    public void removeConnection(String userId, ClientSession session) {
        Set<ClientSession> userSessions = activeConnections.get(userId);
        if (userSessions != null) {
            userSessions.remove(session);
            System.out.println("Server: 1 thiết bị của User [" + userId + "] đã ngắt kết nối.");

            // Nếu user đã tắt hết tất cả các máy (Set rỗng), mới chính thức coi là Offline
            if (userSessions.isEmpty()) {
                activeConnections.remove(userId);
                System.out.println("Server: User [" + userId + "] đã hoàn toàn Offline.");
            }
        }
    }

    // Kiểm tra xem User có đang mở App trên bất kỳ thiết bị nào không
    public boolean isUserOnline(String userId) {
        return activeConnections.containsKey(userId);
    }

    // Đếm tổng số người đang Online (Không phải đếm số thiết bị)
    public int getOnlineUserCount() {
        return activeConnections.size();
    }

    // Gửi tin nhắn Broadcast đến TẤT CẢ thiết bị của 1 User
    public void sendMessageToUser(String userId, String message) {
        Set<ClientSession> sessions = activeConnections.get(userId);
        if (sessions != null) {
            for (ClientSession session : sessions) {
                // Giả định ClientSession có hàm send()
                // session.send(message);
            }
        }
    }
}
