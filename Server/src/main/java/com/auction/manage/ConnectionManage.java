package com.auction.manage;

import com.auction.network.ClientSession;

import java.util.Map;
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

    // Đóng tất cả kết nối để khoá người dùng, kết hợp trong userservice
    public void forceDisconnectUser(String userId) {
        // .remove(userId) sẽ lấy ra Set các session đồng thời xóa luôn User này khỏi Map activeConnections
        Set<ClientSession> sessions = activeConnections.remove(userId);

        if (sessions != null) {
            for (ClientSession session : sessions) {
                try {
                    // 1. (Tùy chọn) Gọi hàm đóng kết nối vật lý của Socket bên trong ClientSession
                    // Ví dụ nếu class ClientSession của bạn có hàm close() hoặc disconnect():
                    session.close();
                    System.out.println("Server: Đã đóng thành công 1 đường dây Socket Live.");
                } catch (Exception e) {
                    System.err.println("❌ Lỗi khi đóng kết nối vật lý của session: " + e.getMessage());
                }
            }
            System.out.println("Server: Đã giải phóng hoàn toàn toàn bộ ClientSession của User [" + userId + "].");
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

    /**
     * Gửi tin nhắn Broadcast đến TẤT CẢ thiết bị của 1 User
     * 🔥 CẢI TIẾN: Tích hợp cơ chế tự dọn dẹp kết nối ma (Self-healing) khi gửi lỗi
     */
    public void sendMessageToUser(String userId, String message) {
        Set<ClientSession> sessions = activeConnections.get(userId);
        if (sessions == null || sessions.isEmpty()) return;

        for (ClientSession session : sessions) {
            try {
                // Thực hiện bắn tin nhắn real-time về giao diện Client
                session.sendMessage(message);
            } catch (Exception e) {
                // Nếu bắn tin nhắn lỗi (chứng tỏ Socket này đã chết ngầm từ trước)
                System.err.println("[Connection] ⚠️ Phát hiện kết nối ma của User [" + userId + "], tiến hành trục xuất...");

                // Tự động tháo dỡ kết nối lỗi này ra khỏi Set ngay lập tức để giải phóng RAM
                removeConnection(userId, session);
            }
        }
    }

    /**
     * 🔥 THỰC THI CLOSE ALL: Cưỡng chế ngắt toàn bộ kết nối Socket trên toàn Server
     * Được gọi duy nhất từ luồng ServerBootstrap khi hệ thống thực hiện hạ cánh an toàn.
     */
    public void closeAllConnections() {
        System.out.println("[ConnectionManage] ⏳ Đang kích hoạt tiến trình giải phóng toàn bộ kết nối mạng...");

        if (activeConnections.isEmpty()) {
            System.out.println("[ConnectionManage] ℹ️ Không có thiết bị nào đang kết nối. Bỏ qua.");
            return;
        }

        int totalClosedDevices = 0;
        int totalUsers = activeConnections.size();

        // 1. Duyệt qua toàn bộ các Set kết nối của từng User đang online
        // Sử dụng entrySet() giúp ta bốc được cả thông tin UserId để in nhật ký (Log) chính xác
        for (Map.Entry<String, Set<ClientSession>> entry : activeConnections.entrySet()) {
            String userId = entry.getKey();
            Set<ClientSession> sessions = entry.getValue();

            if (sessions != null) {
                // 2. Quét qua từng thiết bị (Session) của User đó để giật phích cắm Socket vật lý
                for (ClientSession session : sessions) {
                    try {
                        if (session != null) {
                            // Gọi hàm đóng an toàn của bạn (hàm này sẽ close InputStream, OutputStream và Socket)
                            session.close();
                            totalClosedDevices++;
                        }
                    } catch (Exception e) {
                        System.err.println("[ConnectionManage] ❌ Lỗi khi cưỡng chế đóng Socket của User ["
                                + userId + "]: " + e.getMessage());
                    }
                }
            }
        }

        // 3. CHỐT CHẶN TỐI CAO: Xóa sạch sành sanh mọi dữ liệu trong ConcurrentHashMap
        // Cắt đứt hoàn toàn liên kết tham chiếu để giải phóng RAM ngay lập tức cho JVM
        activeConnections.clear();

        System.out.println("[ConnectionManage] ✅ ĐÃ GIẢI PHÓNG TOÀN DIỆN MẠNG!");
        System.out.println("[ConnectionManage] 👉 Kết quả: Đã đóng an toàn "
                + totalClosedDevices + " thiết bị thuộc " + totalUsers + " người dùng.");
    }
}
