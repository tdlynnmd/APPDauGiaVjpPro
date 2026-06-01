package com.auction.manage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auction.enums.ActionType;
import com.auction.network.ClientSession;
import com.auction.event.AuctionObserver;
import com.auction.event.AuctionEvent;
import com.auction.event.AuctionEventType;
import com.auction.dto.BidTransactionDTO;
import com.auction.dto.SocketResponse;
import com.auction.utils.GsonProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ============================================================
 * LiveRoomManage - Quản lý Socket Connections & Broadcast Events
 * ============================================================
 * TRƯỚC: Quản lý tập trung tất cả socket connections theo phòng (auctionId)
 * Chỉ có method: joinRoom(), leaveRoom(), broadcast()
 * BÂY GIỜ (Refactored with Observer Pattern):
 * - Implement AuctionObserver để lắng nghe sự kiện từ AuctionEventBus
 * - Khi nhận event, tự động broadcast đến tất cả clients trong phòng
 * - Giải phóng AuctionService khỏi trách nhiệm quản lý client notifications
 * Lợi ích của cách này:
 * ✅ Decoupling: AuctionService không cần biết về ClientSession
 * ✅ Scalability: Dễ thêm các Observer khác (NotificationService, AnalyticsService)
 * ✅ Maintainability: Tất cả broadcast logic tập trung ở 1 chỗ
 * ✅ Thread-safe: ConcurrentHashMap + CopyOnWriteArrayList
 * SOLID Principles:
 * - Single Responsibility: Chỉ quản lý broadcast & client sessions
 * - Open/Closed: Mở cho extension (thực thi AuctionObserver), đóng cho modification
 * - Dependency Inversion: Phụ thuộc vào AuctionObserver interface, không concrete class
 */
public class LiveRoomManage implements AuctionObserver {
    private static final Logger log = LoggerFactory.getLogger(LiveRoomManage.class);
    private static volatile LiveRoomManage instance;

    // Key: auctionId, Value: List<ClientSession> đang theo dõi phiên
    private final Map<String, CopyOnWriteArrayList<ClientSession>> rooms
            = new ConcurrentHashMap<>();

    // JSON serializer - dùng để serialize SocketResponse
    private static final com.google.gson.Gson gson = GsonProvider.getGson();

    private LiveRoomManage() {
        log.info("[LiveRoom] 🚀 LiveRoomManage khởi động");
    }

    /**
     * Singleton pattern - Double-checked locking
     */
    public static LiveRoomManage getInstance() {
        LiveRoomManage temp = instance;
        if (temp == null) {
            synchronized (LiveRoomManage.class) {
                temp = instance;
                if (temp == null) {
                    temp = instance = new LiveRoomManage();
                }
            }
        }
        return temp;
    }

    // ============================================================
    // OBSERVER PATTERN: Implement AuctionObserver interface
    // ============================================================

    /**
     * Callback gọi khi AuctionEventBus publish sự kiện
     * Mục đích:
     * - Lắng nghe tất cả sự kiện từ AuctionService, AuctionManage
     * - Dựa vào event.type, chuyển đổi sang ActionType mạng đích danh và xử lý tập trung
     *
     * @param event Sự kiện đã xảy ra
     */
    @Override
    public void update(AuctionEvent event) {
        if (event == null) {
            return;
        }

        String roomId = event.getRoomId();
        AuctionEventType eventType = event.getType();

        log.debug("[LiveRoom] 📨 Nhận event Vòng đời: {} từ phòng {}", eventType, roomId);

        // 🔥 SỬA ĐỒNG BỘ: Xóa hoàn toàn VIEWER_COUNT_CHANGED, tách biệt đích danh ActionType mạng
        switch (eventType) {
            case NEW_BID:
                handleNewBidEvent(roomId, event.getPayload());
                break;

            case TIMER_TICK:
                handleTimerTickEvent(roomId, event.getPayload());
                break;

            case STATUS_CHANGED:
                handleStatusChangedEvent(roomId, event.getPayload());
                break;

            // Đưa người vào xem trực tiếp -> Bắn mã mạng LIVE_ENTERED
            case LIVE_ENTERED:
                handleRoomNotification(roomId, event.getPayload(), ActionType.LIVE_ENTERED);
                break;

            // Đóng màn hình xem trực tiếp -> Bắn mã mạng LIVE_EXITED
            case LIVE_EXITED:
                handleRoomNotification(roomId, event.getPayload(), ActionType.LIVE_EXITED);
                break;

            // Người dùng đăng ký theo dõi nền -> Bắn mã mạng SUBSCRIBE_AUCTION
            case AUCTION_SUBSCRIBED:
                handleRoomNotification(roomId, event.getPayload(), ActionType.AUCTION_SUBSCRIBED);
                break;

            // Người dùng hủy theo dõi nền -> Bắn mã mạng UNSUBSCRIBE_AUCTION
            case AUCTION_UNSUBSCRIBED:
                handleRoomNotification(roomId, event.getPayload(), ActionType.AUCTION_UNSUBSCRIBED);
                break;

            default:
                log.warn("[LiveRoom] ⚠️ Event type không được xử lý: {}", eventType);
        }
    }

    /**
     * Xử lý sự kiện: Có người đặt giá mới (NEW_BID)
     * Payload: BidTransactionDTO
     * Action: Broadcast SocketResponse.event("BID_UPDATED", ...) đến tất cả clients
     *
     * @param roomId auctionId
     * @param payload BidTransactionDTO từ event
     */
    private void handleNewBidEvent(String roomId, Object payload) {
        if (!(payload instanceof BidTransactionDTO bidData)) {
            log.error("[LiveRoom] ❌ NEW_BID payload không phải BidTransactionDTO");
            return;
        }

        // Tạo cấu trúc Body phản hồi thông minh gửi xuống Client chứa cả 2 thông tin
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("roomId", roomId);
        responseBody.put("highestBidderName", bidData.getBidderName());  // Người dẫn đầu mới
        responseBody.put("highestPrice", bidData.getAmount());   // Giá đỉnh mới
        responseBody.put("newEndTime", bidData.getNewEndTime()); // Đồng bộ thời gian kết thúc mới
        responseBody.put("liveStepPrice", bidData.getLiveStepPrice()); // Đồng bộ bước giá live mới
        responseBody.put("bidTransaction", bidData);             // Nguyên thực thể DTO để vẽ dòng lịch sử

        // Tạo SocketResponse với ActionType chung (ví dụ ActionType.BID_UPDATE của bạn)
        SocketResponse response = SocketResponse.event(
                ActionType.BID_UPDATE,
                "Phòng đấu giá có biến động đặt giá mới từ: " + bidData.getBidderName(),
                responseBody
        );

        // Phát loa duy nhất 1 lần xuống cho cả phòng
        String jsonMessage = gson.toJson(response);
        broadcast(roomId, jsonMessage);

        log.info("[LiveRoom] 💰 [GỘP THÀNH CÔNG] Broadcast kết quả đặt giá phòng {} | Winner: {} | Price: {}",
                roomId, bidData.getBidderName(), bidData.getAmount());
    }

    /**
     * Xử lý sự kiện: Countdown thời gian (TIMER_TICK)
     * Payload: Integer (số giây còn lại) hoặc Map với chi tiết thời gian
     * Action: Broadcast thời gian countdown đến clients (update UI real-time)
     *
     * @param roomId auctionId
     * @param payload Thông tin thời gian
     */
    private void handleTimerTickEvent(String roomId, Object payload) {
        if (payload instanceof Number secondsRemaining) {
            SocketResponse response = SocketResponse.event(
                    ActionType.TIME_UPDATE,
                    "Thời gian còn lại: " + secondsRemaining.longValue() + " giây",
                    Map.of("roomId", roomId, "secondsRemaining", secondsRemaining.longValue())
            );
            broadcast(roomId, gson.toJson(response));
        }
    }

    /**
     * Xử lý sự kiện: Trạng thái phiên thay đổi (STATUS_CHANGED)
     * Payload: Map chứa {newStatus, message}
     * Action: Broadcast trạng thái mới đến clients (OPEN -> RUNNING -> FINISHED)
     *
     * @param roomId auctionId
     * @param payload Thông tin trạng thái cũ/mới
     */
    private void handleStatusChangedEvent(String roomId, Object payload) {
        if (payload instanceof Map<?, ?> statusMap) {
            String newStatus = (String) statusMap.get("newStatus");
            String message = (String) statusMap.get("message");

            // Gói tin cực kỳ gọn nhẹ, không chứa thông tin tài chính dư thừa
            SocketResponse response = SocketResponse.event(
                    ActionType.STATUS_UPDATED,
                    message != null ? message : "Trạng thái phiên thay đổi sang: " + newStatus,
                    Map.of("roomId", roomId, "newStatus", newStatus)
            );

            String jsonMessage = gson.toJson(response);
            broadcast(roomId, jsonMessage);

            log.info("[LiveRoom] 🔄 Vòng đời phòng đổi sang: {}", newStatus);

            // Tự động giải phóng bộ nhớ khi phiên đóng cửa
            if ("FINISHED".equals(newStatus) || "CANCELED".equals(newStatus)) {
                clearRoom(roomId);
            }
        }
    }

    /**
     * ============================================================
     * UNIFIED HELPER METHOD: handleRoomNotification()
     * ============================================================
     * 🔥 TỐI ƯU TOÀN DIỆN: Đồng bộ hóa cấu trúc theo kiến trúc bỏ VIEWER_COUNT_CHANGED.
     * Áp dụng nguyên lý DRY triệt để nhằm biến hàm này thành một "nhà máy đóng gói" sự kiện phòng.
     *
     * @param roomId ID phòng đấu giá mục tiêu
     * @param payload Dữ liệu dạng Map chứa {username, message, viewerCount} gửi từ AuctionService
     * @param actionType Mã định danh mạng đích danh (LIVE_ENTERED, LIVE_EXITED, SUBSCRIBE_AUCTION, UNSUBSCRIBE_AUCTION)
     */
    private void handleRoomNotification(String roomId, Object payload, ActionType actionType) {
        // ===================================================================
        // BƯỚC 1: TRÍCH XUẤT DỮ LIỆU PAYLOAD AN TOÀN (DEFENSIVE CHECK)
        // ===================================================================
        if (!(payload instanceof Map<?, ?> payloadMap)) {
            log.error("[LiveRoom] ❌ Định dạng Payload đầu vào bất hợp lệ cho tác vụ: {}", actionType);
            return;
        }

        String message = (String) payloadMap.get("message");
        Integer viewerCount = (Integer) payloadMap.get("viewerCount");
        String username = (String) payloadMap.get("username"); // 🔥 TỐI ƯU MỚI: Lấy thêm tên người kích hoạt sự kiện

        // Cơ chế bẫy dữ liệu dự phòng (Fallback) thông minh
        if (message == null) {
            message = "Hệ thống ghi nhận thay đổi trạng thái phòng.";
        }
        if (viewerCount == null) {
            viewerCount = getRoomSize(roomId);
        }

        // ===================================================================
        // BƯỚC 2: KHỞI TẠO CẤU TRÚC JSON PHẢN HỒI GIÀU THÔNG TIN
        // ===================================================================
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("roomId", roomId);
        responseBody.put("logMessage", message);             // Chuỗi hiển thị lên khung chat trực tuyến
        responseBody.put("currentViewerCount", viewerCount); // Con số cập nhật nhãn số lượng người xem trên UI

        if (username != null) {
            responseBody.put("username", username);          // 🔥 BỔ SUNG: Giúp Client biết chính xác ĐÍCH DANH ai vừa ra/vào phòng để vẽ avatar hoặc hiệu ứng UI nếu cần
        }

        // ===================================================================
        // BƯỚC 3: ĐÓNG GÓI SỬ DỤNG ĐÍCH DANH ACTIONTYPE CỦA LƯỚI MẠNG
        // ===================================================================
        // Vỏ bọc SocketResponse mang đúng bản chất hành động mạng (LIVE_ENTERED hoặc LIVE_EXITED)
        SocketResponse response = SocketResponse.event(
                actionType,
                message,
                responseBody
        );

        // ===================================================================
        // BƯỚC 4: THỰC THI PHÁT LOA TRUYỀN THÔNG BẤT ĐỒNG BỘ CHỐNG NGHẼN
        // ===================================================================
        String jsonMessage = gson.toJson(response);
        broadcast(roomId, jsonMessage);

        log.info("[LiveRoom] 🔔 [MẠNG ĐỒNG BỘ] Phát sóng gói tin thành công | Mã lệnh mạng: {} | Số người xem: {} | Kích hoạt bởi: {}",
                actionType, viewerCount, (username != null ? username : "Hệ thống"));
    }

    /**
     * Join vào phòng đấu giá (Subscribe)
     * - Tạo room nếu chưa tồn tại
     * - Thêm client vào danh sách subscribers
     */
    public void joinRoom(String auctionId, ClientSession clientSession) {
        if (auctionId == null || clientSession == null) return;

        rooms.compute(auctionId, (key, room) -> {
            if (room == null) room = new CopyOnWriteArrayList<>();
            if (!room.contains(clientSession)) {
                room.add(clientSession);
                // 🔥 VÁ LỖI TẠI ĐÂY: Ghi dấu phòng trực tuyến vào cấu trúc Session của Client
                clientSession.setCurrentAuctionId(auctionId);
                log.info("[LiveRoom] ✅ Client JOIN phiên {}", auctionId);
            }
            return room; // Trả list đã update ngược lại vào Map
        });
    }

    /**
     * Rút khỏi phòng đấu giá (Unsubscribe)
     * - Xóa client khỏi danh sách -> ngắt kế nối khi đóng tab liveroom, hoac mất mạng
     * - Xóa room nếu trống (dọn dẹp bộ nhớ)
     */
    public void leaveRoom(String auctionId, ClientSession clientSession) {
        if (auctionId == null || clientSession == null) return;

        // Tính toán lại giá trị của phòng này trong trạng thái khóa an toàn
        rooms.computeIfPresent(auctionId, (key, room) -> {
            room.remove(clientSession);
            // 🔥 VÁ LỖI TẠI ĐÂY: Giải phóng con trỏ phòng trực tuyến của Client về null
            clientSession.setCurrentAuctionId(null);
            log.info("[LiveRoom] ❌ Client LEAVE phiên {}", auctionId);

            // Nếu List rỗng, trả về null -> ConcurrentHashMap sẽ TỰ ĐỘNG XÓA key này!
            return room.isEmpty() ? null : room;
        });
    }

    /**
     * ============================================================
     * PRIVATE BROADCAST - Phát sóng message đến tất cả clients trong phòng
     * ============================================================
     * Encapsulation: Chỉ các method bên trong LiveRoomManage được phép gọi
     * Không cho AuctionService hoặc các class khác gọi trực tiếp
     * - Thread-safe: CopyOnWriteArrayList cho phép iterate an toàn khi add/remove
     * - Nếu gửi lỗi, tự động xóa client khỏi phòng
     *
     * @param auctionId ID phòng
     * @param jsonMessage SocketResponse đã được serialize thành JSON
     */
    private void broadcast(String auctionId, String jsonMessage) {
        if (auctionId == null || jsonMessage == null) {
            return;
        }

        CopyOnWriteArrayList<ClientSession> room = rooms.get(auctionId);
        if (room == null || room.isEmpty()) {
            return; // Không có ai trong phòng
        }

        log.debug("[LiveRoom] 📢 Broadcast tới {} clients ở phiên {}", room.size(), auctionId);

        // Iterate từ CopyOnWriteArrayList - thread-safe
        for (ClientSession client : room) {
            boolean success = client.sendMessage(jsonMessage);
            if (!success) {
                // Nếu gửi lỗi (client disconnect), tự động xóa khỏi phòng
                log.warn("[LiveRoom] ⚠️ Gửi message lỗi tới {}, tự động dọn dẹp khỏi phòng.", client.getUserId());
                leaveRoom(auctionId, client);
            }
        }
    }

    /**
     * Lấy số lượng clients trong phòng (for monitoring)
     */
    public int getRoomSize(String auctionId) {
        CopyOnWriteArrayList<ClientSession> room = rooms.get(auctionId);
        return (room != null) ? room.size() : 0;
    }

    /**
     * Lấy tất cả phòng đang hoạt động (for monitoring)
     */
    public int getTotalRooms() {
        return rooms.size();
    }

    /**
     * Xóa toàn bộ clients khỏi phòng (khi phiên kết thúc)
     * - Gọi khi finalizeAuction hoặc cancelAuction để dọn dẹp
     */
    public void clearRoom(String auctionId) {
        CopyOnWriteArrayList<ClientSession> room = rooms.remove(auctionId);
        if (room != null) {
            room.clear();
            log.info("[LiveRoom] Phòng {} đã bị xóa", auctionId);
        }
    }
}