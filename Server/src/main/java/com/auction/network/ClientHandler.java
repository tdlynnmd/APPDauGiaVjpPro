package com.auction.network;
/*
 ClientHandler xu ly 1 client cụ thể
 Nhiệm vụ:
 -> Đọc request từ client gửi lên
 -> Gửi request đó cho RequestDispatcher xử lý
 -> Nhận response trả về
 -> Gửi response lại cho client
 -> Quản lý vòng đời kết nối socket của client đó
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.auction.exception.AuthenticationException;

public class ClientHandler implements Runnable {
    private final Socket socket;
    // Chuyển việc phân luồng cho Dispatcher lo
    private final RequestDispatcher dispatcher;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.dispatcher = RequestDispatcher.getInstance();
    }

    @Override
    public void run() {
        ClientSession session = null;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
            PrintWriter writer = new PrintWriter(new java.io.OutputStreamWriter(socket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8), true);

            // 🔥 TỐI ƯU SOCKET: Đặt Read Timeout = 60 giây.
            // Nếu Client không gửi bất kỳ dữ liệu nào (kể cả PING keep-alive) trong 60 giây,
            // readLine() sẽ ném SocketTimeoutException, kích hoạt luồng dọn dẹp tự động.
            socket.setSoTimeout(60000);

            // Bọc Socket và Writer vào một cái "Thẻ bàn" (ClientSession)
            session = new ClientSession(socket, writer);

            String requestJson;
            // VÒNG LẶP SINH TỬ: Giữ kết nối liên tục.
            // Vòng lặp này chỉ dừng khi Client tắt App hoặc rớt mạng (reader trả về null)
            while ((requestJson = reader.readLine()) != null) {
                if (requestJson.trim().isEmpty()) continue;

                // Bưng cục JSON và cái thẻ bàn ném cho Tổ trưởng (Dispatcher) xử lý
                dispatcher.processRequest(requestJson, session);
            }

        } catch (IOException e) {
            System.out.println("[Server] Client ngắt kết nối đột ngột: " + e.getMessage());
        } finally {
            // 🔥 ĐÂY LÀ CHÌA KHÓA: Dù crash hay tắt app bình thường, block finally luôn chạy
            System.out.println("Hệ thống: Tiến hành kích hoạt luồng dọn dẹp tự động...");
            try {
                if (session != null) {
                    session.close();
                }
            } catch (AuthenticationException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
