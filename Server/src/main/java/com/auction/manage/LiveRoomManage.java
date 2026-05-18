package com.auction.manage;

import com.auction.network.ClientSession;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * LiveRoomManage: Quản lý tập trung tất cả socket connections theo phòng (auctionId)
 *
 * Nguồn: Đề xuất kiến trúc LiveRoom thay thế Observer Pattern
 * Lợi ích:
 * - Broadcast message đến tất cả clients trong phòng dễ dàng
 * - Hỗ trợ 1 user nhiều connections (mobile + web)
 * - Clear separation of concerns: Auction chỉ focus logic, broadcast ở đây
 * - Thread-safe với ConcurrentHashMap + CopyOnWriteArrayList
 */
public class LiveRoomManage {
    private static volatile LiveRoomManage instance;

    // Key: auctionId, Value: List<ClientSession> đang theo dõi phiên
    private final Map<String, CopyOnWriteArrayList<ClientSession>> rooms
        = new ConcurrentHashMap<>();

    private LiveRoomManage() {}

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
                System.out.println("[LiveRoom] ✅ Client JOIN phiên " + auctionId);
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
            System.out.println("[LiveRoom] ❌ Client LEAVE phiên " + auctionId);

            // Nếu List rỗng, trả về null -> ConcurrentHashMap sẽ TỰ ĐỘNG XÓA key này!
            return room.isEmpty() ? null : room;
        });
    }

    /**
     * Phát sóng message đến tất cả clients trong phòng (Broadcast)
     * - Thread-safe: CopyOnWriteArrayList cho phép iterate an toàn khi add/remove
     * - Nếu gửi lỗi, tự động xóa client khỏi phòng
     */
    public void broadcast(String auctionId, String jsonMessage) {
        if (auctionId == null || jsonMessage == null) {
            return;
        }

        CopyOnWriteArrayList<ClientSession> room = rooms.get(auctionId);
        if (room == null || room.isEmpty()) {
            return; // Không có ai trong phòng
        }

        System.out.println("[LiveRoom] 📢 Broadcast tới " + room.size()
            + " clients ở phiên " + auctionId);

        // Iterate từ CopyOnWriteArrayList - thread-safe
        for (ClientSession client : room) {
            try {
                client.sendMessage(jsonMessage);
            } catch (Exception e) {
                // Nếu gửi lỗi (client disconnect), tự động xóa khỏi phòng
                System.err.println("[LiveRoom] ⚠️ Gửi message lỗi tới "
                    + client.getUserId() + ": " + e.getMessage());
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
            System.out.println("[LiveRoom] Phòng " + auctionId + " đã bị xóa");
        }
    }
}

