package com.auction.service;

import com.auction.dto.SocketResponse;

/**
 * Interface định nghĩa bộ lắng nghe sự kiện cập nhật thời gian thực từ Server.
 */
public interface RealtimeUpdateListener {

    /**
     * Ham duoc goi khi ClientSocketService nhan duoc SocketResponse co type = EVENT.
     *
     * Luu y:
     * - Ham nay duoc goi tu thread doc socket.
     * - Neu Controller cap nhat JavaFX UI trong ham nay, can dung Platform.runLater(...).
     *
     * @param event realtime event Server gui ve
     */
    void onRealtimeUpdate(SocketResponse event);
}
