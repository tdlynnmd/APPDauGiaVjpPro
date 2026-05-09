package com.auction.network;
/**
 SocketServer mở cổng Server để Client kết nối vào.
 */

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer {
    private static final int PORT = 5555;

    public void start() {
        System.out.println("[Server] Đang chạy tại port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {      // Mở ServerSocket tại port 5555.
            while (true) {                                              // Từ lúc này, Server sẵn sàng nhận kết nối.
                Socket clientSocket = serverSocket.accept();

                System.out.println("[Server] Có client kết nối: "
                        + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);

                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("[Server] Lỗi SocketServer: " + e.getMessage());
        }
    }
}