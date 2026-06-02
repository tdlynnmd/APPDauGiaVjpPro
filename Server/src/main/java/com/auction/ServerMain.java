package com.auction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auction.network.SocketServer;

/**
 * =========================================================================
 * Main - Điểm khởi hỏa duy nhất của hệ thống Đấu giá trực tuyến (Server)
 * =========================================================================
 * Kiến trúc:
 * - Tuân thủ tuyệt đối nguyên lý Tách biệt trách nhiệm (Separation of Concerns).
 * - Không chứa dữ liệu cấu hình cứng (Hardcoded), không chứa logic nghiệp vụ.
 * - Chỉ đóng vai trò là "chìa khóa vặn nổ máy" kích hoạt chuỗi vòng đời hệ thống.
 */
public class ServerMain {
    private static final Logger log = LoggerFactory.getLogger(ServerMain.class);

    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println("   🔨 HỆ THỐNG MÁY CHỦ ĐẤU GIÁ TRỰC TUYẾN (PRODUCTION)   ");
        System.out.println("=========================================================");

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.start();

        log.info("[Main] 🔌 Kích hoạt cổng mạng Socket vật lý...");

        try {
            SocketServer socketServer = new SocketServer();
            socketServer.start();
        } catch (Exception e) {
            log.error("[Main] 💥 SỰ CỐ MẠNG NGHIÊM TRỌNG! Không thể mở cổng Socket Server.", e);
            System.exit(1);
        }
    }
}
