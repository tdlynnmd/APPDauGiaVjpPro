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

public class ClientSession {
    private static final Logger log = LoggerFactory.getLogger(ClientSession.class);

    private String userId; // Ban đầu null, sau khi login mới có giá trị
    private UserRole role;      // Server luu role để kiểm tra quyền
    private String username;     // 🔥 Bổ sung để hiển thị log/thông báo
    private String currentAuctionId; // 🔥 Bổ sung: ID phiên đấu giá client đang xem trực tuyến

    // 🔥 TỐI ƯU 1: Cờ hiệu kiểm soát Idempotent chống dọn dẹp trùng lặp.
    // Sử dụng volatile để đảm bảo tính hiển thị ngay lập tức giữa các luồng xử lý song song.
    private volatile boolean closed = false;

    // 🔥 TỐI ƯU KIẾN TRÚC: Mỗi Client khi kết nối vào sẽ được cấp riêng một hàng đợi tuần tự (Single Thread Executor)
    // Đảm bảo tất cả các Request do chính Client này bấm nút gửi lên sẽ bắt buộc phải xếp hàng chạy theo đúng thứ tự thời gian.
    private final ExecutorService sessionExecutor = Executors.newSingleThreadExecutor();

    // 🔥 RATE LIMITER: Hàng đợi lưu timestamp (epoch ms) các request gần nhất trong cửa sổ trượt 1 giây
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

        // Loại bỏ các timestamp đã hết hạn (nằm ngoài cửa sổ 1 giây)
        while (!requestTimestamps.isEmpty() && (now - requestTimestamps.peek()) > RATE_WINDOW_MS) {
            requestTimestamps.poll();
        }

        // Nếu đã đạt hoặc vượt giới hạn, chặn request
        if (requestTimestamps.size() >= MAX_REQUESTS_PER_SECOND) {
            return true;
        }

        // Ghi nhận timestamp của request hợp lệ
        requestTimestamps.add(now);
        return false;
    }

    // 🔥 Getter để RequestDispatcher có thể mượn hàng đợi của Session xử lý
    public ExecutorService getSessionExecutor() {
        return this.sessionExecutor;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public UserRole getRole() {return role;}
    public void setRole(UserRole role) {this.role = role;}


    // Hàm này để ConnectionManage có thể gọi để bắn tin nhắn real-time về UI
    public boolean sendMessage(String jsonMessage) {
        if (out != null) {
            out.println(jsonMessage);
            out.flush();
            return !out.checkError();
        }
        return false;
    }

    // 1 session được coi là đã login khi có cả userID và role
    public boolean isLoggedIn() {
        return userId != null && role != null;
    }

    // Xóa thông tin khi logout thành công
    public void clearLoginInfo() {
        this.userId = null;
        this.role = null;
    }

    /**
     * 🔥 HÀM ĐÓNG KẾT NỐI AN TOÀN KHI CÓ SỰ CỐ ĐỨT MẠNG (Đạt chuẩn Idempotent & Chống rò rỉ)
     * Tự động dọn dẹp sạch sẽ dấu vết của Client trên RAM Server
     */
    public void close() {
        // CHỐT CHẶN 1: Nếu đã đóng rồi thì lập tức quay xe, không xử lý dọn dẹp lại nữa
        if (closed) {
            return;
        }

        // Đặt khối synchronized ngắn hạn để đảm bảo chỉ duy nhất một luồng được quyền đóng mốc đầu tiên
        synchronized (this) {
            if (closed) return;
            closed = true; // Đóng dấu chủ quyền: Session này chính thức ngừng hoạt động
        }

        log.debug("[Network Guard] ⏳ Tiến hành đóng kết nối Idempotent cho User: {}", username);

        // 🔥 TỐI ƯU 2: B bọc toàn bộ luồng nghiệp vụ dọn dẹp bộ nhớ và ngắt Socket vào try/finally
        try {
            // 1. Dọn dẹp logic trên các RAM Manager hệ thống
            if (userId != null) {
                ConnectionManage.getInstance().removeConnection(userId, this);

                if (!ConnectionManage.getInstance().isUserOnline(userId)) {
                    UserManage.getInstance().deleteUser(userId);
                }

                if (currentAuctionId != null) {
                    LiveRoomManage.getInstance().leaveRoom(currentAuctionId, this);
                }
            }
        } catch (Exception e) {
            // Cô lập lỗi logic: Nếu dọn dẹp RAM bị crash, in lỗi ra log chứ không được phép làm nghẽn luồng hạ cánh Socket
            log.error("[Network Guard] ⚠️ Có gợn lỗi khi dọn dẹp RAM: {}", e.getMessage(), e);
        } finally {

            // 🔥 CHỐT CHẶN TỐI CAO TRONG FINALLY: Bắt buộc luôn luôn được thực thi kể cả khi đoạn code trên bị sập!
            // Đảm bảo hàng đợi đơn luồng và Socket vật lý PHẢI ĐƯỢC GIẢI PHÓNG TRONG MỌI HOÀN CẢNH.
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