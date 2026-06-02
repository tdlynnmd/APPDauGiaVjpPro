package com.auction.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientNetworkManager {
    private static ClientNetworkManager instance;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    private ClientNetworkManager() {
        // === GIỮ NGUYÊN CODE CŨ KHỞI TẠO BAN ĐẦU ===
        try {
            this.socket = new Socket("localhost", 5555);
            this.writer = new PrintWriter(new java.io.OutputStreamWriter(socket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8), true);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException e) { e.printStackTrace(); }
    }

    // === CẬP NHẬT HÀM GETINSTANCE AN TOÀN HƠN ===
    public static ClientNetworkManager getInstance() {
        if (instance == null) {
            instance = new ClientNetworkManager();
        } else if (instance.socket == null || instance.socket.isClosed()) {
            // Nếu instance đã tồn tại nhưng Socket bên dưới đã bị đóng do logout,
            // tạo một thực thể mới hoàn toàn để mở lại đường truyền mạng mới sạch sẽ.
            System.out.println("[Network] Socket cũ đã đóng. Đang thiết lập kết nối hoàn toàn mới...");
            instance = new ClientNetworkManager();
        }
        return instance;
    }

    public PrintWriter getWriter() { return writer; }
    public BufferedReader getReader() { return reader; }

    // Hàm dọn dẹp, đóng Socket cũ khi Logout để không bị đơ app
    public static void resetConnection() {
        if (instance != null) {
            try {
                System.out.println("[Network] Đang tiến hành dọn dẹp và đóng kết nối Socket cũ...");

                // --- PHẦN CẬP NHẬT MỚI: ÉP NGẮT CÁC LUỒNG ĐỂ TRÁNH BỊ TREO READLINE() ---
                if (instance.writer != null) {
                    instance.writer.flush();
                    instance.writer.close();
                }
                if (instance.reader != null) {
                    instance.reader.close();
                }
                if (instance.socket != null && !instance.socket.isClosed()) {
                    // Ép dừng cưỡng chế luồng đọc ngầm để tránh nghẽn kẹt RAM của luồng JavaFX chính
                    instance.socket.shutdownInput();
                    instance.socket.shutdownOutput();
                    instance.socket.close();
                }
                // ---------------------------------------------------------------------

            } catch (IOException e) {
                System.out.println("[Network] Lỗi xảy ra khi đóng luồng kết nối cũ: " + e.getMessage());
            } finally {
                // Ép instance về null để lần Login sau hàm getInstance() sẽ tạo ra kết nối hoàn toàn mới
                instance = null;

                // Gọi dọn rác hệ thống thu hồi bộ nhớ của Thread cũ ngay lập tức
                System.gc();

                System.out.println("[Network] Đã dọn dẹp Socket thành công. Sẵn sàng cho lượt Login tiếp theo.");
            }
        }
    }
}