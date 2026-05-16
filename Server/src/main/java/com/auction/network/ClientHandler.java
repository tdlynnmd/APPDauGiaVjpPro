package com.auction.network;
/**
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
import com.auction.manage.ConnectionManage;

public class ClientHandler implements Runnable {
    private final Socket socket;
    // Chuyển việc phân luồng cho Dispatcher lo
    private final RequestDispatcher dispatcher;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.dispatcher = new RequestDispatcher();
    }

    @Override
    public void run() {
        ClientSession session = null;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            // Bọc Socket và Writer vào một cái "Thẻ bàn" (ClientSession)
            session = new ClientSession(socket, writer);

            String requestJson;
            // VÒNG LẶP SINH TỬ: Giữ kết nối liên tục.
            // Vòng lặp này chỉ dừng khi Client tắt App hoặc rớt mạng (reader trả về null)
            while ((requestJson = reader.readLine()) != null) {
                if (requestJson.isBlank()) continue;

                // Bưng cục JSON và cái thẻ bàn ném cho Tổ trưởng (Dispatcher) xử lý
                dispatcher.processRequest(requestJson, session);
            }

        } catch (IOException e) {
            System.out.println("[Server] Client ngắt kết nối đột ngột: " + e.getMessage());
        } finally {
            // DỌN DẸP KHI CLIENT RỚT MẠNG
            if (session != null) {
                String userId = session.getUserId();
                if (userId != null) {
                    // Xóa khỏi danh bạ Online
                    ConnectionManage.getInstance().removeConnection(userId, session);
                }
                session.close();
            }
        }
    }
}