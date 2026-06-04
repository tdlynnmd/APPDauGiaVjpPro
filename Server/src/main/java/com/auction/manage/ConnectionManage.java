package com.auction.manage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auction.network.ClientSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Lớp quản lý các thiết bị kết nối và phiên làm việc (Client Session) của người dùng.
 */
public class ConnectionManage {
    private static final Logger log = LoggerFactory.getLogger(ConnectionManage.class);

    private static volatile ConnectionManage instance;

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

    public void registerConnection(String userId, ClientSession session) {
        if (userId != null && session != null) {
            activeConnections.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
            log.info("Server: Thiết bị mới của User [{}] đã kết nối.", userId);
        }
    }

    public void removeConnection(String userId, ClientSession session) {
        Set<ClientSession> userSessions = activeConnections.get(userId);
        if (userSessions != null) {
            userSessions.remove(session);
            log.info("Server: 1 thiết bị của User [{}] đã ngắt kết nối.", userId);

            if (userSessions.isEmpty()) {
                activeConnections.remove(userId);
                log.info("Server: User [{}] đã hoàn toàn Offline.", userId);
            }
        }
    }

    public void forceDisconnectUser(String userId) {
        Set<ClientSession> sessions = activeConnections.remove(userId);

        if (sessions != null) {
            for (ClientSession session : sessions) {
                try {
                    session.close();
                    log.info("Server: Đã đóng thành công 1 đường dây Socket Live.");
                } catch (Exception e) {
                    log.error("❌ Lỗi khi đóng kết nối vật lý của session: {}", e.getMessage(), e);
                }
            }
            log.info("Server: Đã giải phóng hoàn toàn toàn bộ ClientSession của User [{}].", userId);
        }
    }

    public void forceDisconnectOtherDevices(String userId, ClientSession currentSession) {
        Set<ClientSession> sessions = activeConnections.get(userId);
        if (sessions != null) {
            for (ClientSession session : sessions) {
                if (session != currentSession) {
                    try {
                        session.close();
                        sessions.remove(session);
                        log.info("Server: Đã cưỡng chế đóng 1 kết nối Socket thiết bị khác của User [{}].", userId);
                    } catch (Exception e) {
                        log.error("❌ Lỗi khi đóng kết nối thiết bị khác của User [{}]: {}", userId, e.getMessage(), e);
                    }
                }
            }
        }
    }

    public boolean isUserOnline(String userId) {
        return activeConnections.containsKey(userId);
    }

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
            boolean success = session.sendMessage(message);
            if (!success) {
                log.warn("[Connection] ⚠️ Phát hiện kết nối ma của User [{}], tiến hành trục xuất...", userId);

                removeConnection(userId, session);
                try {
                    session.close();
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * 🔥 THỰC THI CLOSE ALL: Cưỡng chế ngắt toàn bộ kết nối Socket trên toàn Server
     * Được gọi duy nhất từ luồng ServerBootstrap khi hệ thống thực hiện hạ cánh an toàn.
     */
    public void closeAllConnections() {
        log.info("[ConnectionManage] ⏳ Đang kích hoạt tiến trình giải phóng toàn bộ kết nối mạng...");

        if (activeConnections.isEmpty()) {
            log.info("[ConnectionManage] ℹ️ Không có thiết bị nào đang kết nối. Bỏ qua.");
            return;
        }

        int totalClosedDevices = 0;
        int totalUsers = activeConnections.size();

        for (Map.Entry<String, Set<ClientSession>> entry : activeConnections.entrySet()) {
            String userId = entry.getKey();
            Set<ClientSession> sessions = entry.getValue();

            if (sessions != null) {
                for (ClientSession session : sessions) {
                    try {
                        if (session != null) {
                            session.close();
                            totalClosedDevices++;
                        }
                    } catch (Exception e) {
                        log.error("[ConnectionManage] ❌ Lỗi khi cưỡng chế đóng Socket của User [{}]: {}", userId, e.getMessage(), e);
                    }
                }
            }
        }

        activeConnections.clear();

        log.info("[ConnectionManage] ✅ ĐÃ GIẢI PHÓNG TOÀN DIỆN MẠNG!");
        log.info("[ConnectionManage] 👉 Kết quả: Đã đóng an toàn {} thiết bị thuộc {} người dùng.", totalClosedDevices, totalUsers);
    }

    /**
     * Đếm tổng số kết nối Socket (số thiết bị) vật lý đang kết nối live trên toàn Server.
     * Sử dụng để cấu hình giới hạn trần cứng (Max Connections Cap) chống nghẽn luồng.
     */
    public int getOnlineCount() {
        int totalDevices = 0;

        for (Set<ClientSession> sessions : activeConnections.values()) {
            if (sessions != null) {
                totalDevices += sessions.size();
            }
        }

        return totalDevices;
    }

    /**
     * Gửi cập nhật ví thời gian thực tới tất cả thiết bị của user
     */
    public void sendBalanceUpdate(String userId, double available, double frozen) {
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("availableBalance", available);
        payload.put("frozenBalance", frozen);
        
        com.auction.dto.SocketResponse event = com.auction.dto.SocketResponse.event(
                com.auction.enums.ActionType.WALLET_UPDATE,
                "Số dư tài khoản của bạn đã thay đổi.",
                payload
        );
        
        String jsonMessage = com.auction.utils.GsonProvider.getGson().toJson(event);
        sendMessageToUser(userId, jsonMessage);
    }
}
