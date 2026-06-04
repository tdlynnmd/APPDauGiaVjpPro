package com.auction.network;

import com.auction.manage.ConnectionManage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Mở cổng Socket TCP để lắng nghe và kết nối các thực thể Client mới vào hệ thống.
 */
public class SocketServer {
    private static final int PORT = 5555;
    private static final int MAX_CLIENTS = 300;

    private final ExecutorService threadPool = Executors.newVirtualThreadPerTaskExecutor();
    public void start() {
        System.out.println("[Server] Đang chạy tại port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();

                if (ConnectionManage.getInstance().getOnlineCount() >= MAX_CLIENTS) {
                    clientSocket.close();
                    continue;
                }

                System.out.println("[Server] Có client kết nối: "
                        + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);

                threadPool.execute(clientHandler);
            }
        } catch (IOException e) {
            System.err.println("[Server] Lỗi SocketServer: " + e.getMessage());
        }
    }
}