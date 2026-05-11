package com.auction.network;
/**
 SocketServer mở cổng Server để Client kết nối vào.
 */

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServer {
    private static final int PORT = 5555;

    // Cấp phát một "đội quân" 50 luồng chạy sẵn
    private final ExecutorService threadPool = Executors.newFixedThreadPool(50);
    public void start() {
        System.out.println("[Server] Đang chạy tại port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {      // Mở ServerSocket tại port 5555.
            while (true) {                                              // Từ lúc này, Server sẵn sàng nhận kết nối.
                Socket clientSocket = serverSocket.accept();

                System.out.println("[Server] Có client kết nối: "
                        + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);

                // Quăng việc cho ThreadPool lo, không tự new Thread nữa
                threadPool.execute(clientHandler);
            }
        } catch (IOException e) {
            System.err.println("[Server] Lỗi SocketServer: " + e.getMessage());
        }
    }
}