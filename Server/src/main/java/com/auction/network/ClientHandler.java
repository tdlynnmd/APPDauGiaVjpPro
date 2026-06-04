package com.auction.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.auction.exception.AuthenticationException;

/**
 * Lớp quản lý kết nối Socket của một Client cụ thể ở luồng ảo riêng.
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
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

            socket.setSoTimeout(60000);

            session = new ClientSession(socket, writer);

            String requestJson;
            while ((requestJson = reader.readLine()) != null) {
                if (requestJson.trim().isEmpty()) continue;

                dispatcher.processRequest(requestJson, session);
            }

        } catch (IOException e) {
            System.out.println("[Server] Client ngắt kết nối đột ngột: " + e.getMessage());
        } finally {
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
