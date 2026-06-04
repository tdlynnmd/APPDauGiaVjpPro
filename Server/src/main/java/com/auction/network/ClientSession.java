package com.auction.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auction.enums.UserRole;
import com.auction.manage.ConnectionManage;
import com.auction.manage.LiveRoomManage;
import com.auction.manage.UserManage;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lớp đại diện cho một phiên kết nối mạng của Client, quản lý ghi dữ liệu và bộ thực thi luồng.
 */
public class ClientSession {
    private static final Logger log = LoggerFactory.getLogger(ClientSession.class);

    private String userId;
    private UserRole role;
    private String username;
    private String currentAuctionId;

    private volatile boolean closed = false;

    private final ExecutorService sessionExecutor = Executors.newSingleThreadExecutor();

    private static final int MAX_REQUESTS_PER_SECOND = 10;
    private static final long RATE_WINDOW_MS = 1000;
    private final Queue<Long> requestTimestamps = new LinkedList<>();

    private final Socket socket;
    private final PrintWriter out;

    public ClientSession(Socket socket, PrintWriter out) {
        this.socket = socket;
        this.out = out;
    }

    /**
     * 🔥 RATE LIMITER: Kiểm tra xem Session này có đang gửi quá nhiều request hay không.
     * Sử dụng cửa sổ trượt (Sliding Window) 1 giây, tối đa 10 requests.
     * @return true nếu bị chặn (rate limited), false nếu cho phép tiếp tục
     */
    public synchronized boolean isRateLimited() {
        long now = System.currentTimeMillis();

        while (!requestTimestamps.isEmpty() && (now - requestTimestamps.peek()) > RATE_WINDOW_MS) {
            requestTimestamps.poll();
        }

        if (requestTimestamps.size() >= MAX_REQUESTS_PER_SECOND) {
            return true;
        }

        requestTimestamps.add(now);
        return false;
    }

    public ExecutorService getSessionExecutor() {
        return this.sessionExecutor;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public UserRole getRole() {return role;}
    public void setRole(UserRole role) {this.role = role;}

    public boolean sendMessage(String jsonMessage) {
        if (out != null) {
            out.println(jsonMessage);
            out.flush();
            return !out.checkError();
        }
        return false;
    }

    public boolean isLoggedIn() {
        return userId != null && role != null;
    }

    public void clearLoginInfo() {
        this.userId = null;
        this.role = null;
    }

    /**
     * 🔥 HÀM ĐÓNG KẾT NỐI AN TOÀN KHI CÓ SỰ CỐ ĐỨT MẠNG (Đạt chuẩn Idempotent & Chống rò rỉ)
     * Tự động dọn dẹp sạch sẽ dấu vết của Client trên RAM Server
     */
    public void close() {
        if (closed) {
            return;
        }

        synchronized (this) {
            if (closed) return;
            closed = true;
        }

        log.debug("[Network Guard] ⏳ Tiến hành đóng kết nối Idempotent cho User: {}", username);

        try {
            if (userId != null) {
                try {
                    ConnectionManage.getInstance().removeConnection(userId, this);
                } catch (Exception e) {
                    log.error("[Network Guard] ⚠️ Lỗi khi removeConnection của User [{}]: {}", userId, e.getMessage());
                }

                try {
                    if (!ConnectionManage.getInstance().isUserOnline(userId)) {
                        UserManage.getInstance().deleteUser(userId);
                    }
                } catch (Exception e) {
                    log.warn("[Network Guard] ⚠️ Lỗi khi deleteUser (có thể do session test hoặc đăng nhập giả lập) của User [{}]: {}", userId, e.getMessage());
                }

                try {
                    String roomId = currentAuctionId;
                    if (roomId != null) {
                        LiveRoomManage.getInstance().leaveRoom(roomId, this);

                        int viewerCount = LiveRoomManage.getInstance().getRoomSize(roomId);
                        java.util.Map<String, Object> liveExitedPayload = new java.util.HashMap<>();
                        liveExitedPayload.put("username", this.username);
                        liveExitedPayload.put("message", (this.username != null ? this.username : "User") + " đã ngắt kết nối đột ngột.");
                        liveExitedPayload.put("viewerCount", viewerCount);

                        com.auction.event.AuctionEventBus.getInstance().publish(
                            new com.auction.event.AuctionEvent(roomId, com.auction.event.AuctionEventType.LIVE_EXITED, liveExitedPayload)
                        );
                    }
                } catch (Exception e) {
                    log.error("[Network Guard] ⚠️ Lỗi khi dọn dẹp phòng live trực tuyến của User [{}]: {}", userId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[Network Guard] ⚠️ Có gợn lỗi khi dọn dẹp RAM: {}", e.getMessage(), e);
        } finally {

            this.sessionExecutor.shutdown();

            try {
                if (out != null) {
                    out.close();
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                log.debug("[Network Guard] ✅ Đã giải phóng hoàn toàn hạ tầng Socket vật lý và Executor.");
            } catch (IOException ex) {
                log.error("[Network Guard] ❌ Lỗi khi dập phích cắm Socket: {}", ex.getMessage(), ex);
            }
        }
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setCurrentAuctionId(String currentAuctionId) {
        this.currentAuctionId = currentAuctionId;
    }
}