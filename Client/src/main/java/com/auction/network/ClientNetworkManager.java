package com.auction.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Lớp quản lý kết nối Socket TCP vật lý trung tâm phía Client.
 */
public class ClientNetworkManager {
    private static ClientNetworkManager instance;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    private ClientNetworkManager() {
        try {
            this.socket = new Socket();
            this.socket.connect(new InetSocketAddress("127.0.0.1", 5555), 3000);
            this.writer = new PrintWriter(new java.io.OutputStreamWriter(socket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8), true);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static ClientNetworkManager getInstance() {
        if (instance == null) {
            instance = new ClientNetworkManager();
        } else if (instance.socket == null || instance.socket.isClosed()) {
            System.out.println("[Network] Socket cũ đã đóng. Đang thiết lập kết nối hoàn toàn mới...");
            instance = new ClientNetworkManager();
        }
        return instance;
    }

    public PrintWriter getWriter() { return writer; }
    public BufferedReader getReader() { return reader; }

    public static void resetConnection() {
        if (instance != null) {
            try {
                System.out.println("[Network] Đang tiến hành dọn dẹp và đóng kết nối Socket cũ...");

                if (instance.writer != null) {
                    instance.writer.flush();
                    instance.writer.close();
                }
                if (instance.reader != null) {
                    instance.reader.close();
                }
                if (instance.socket != null && !instance.socket.isClosed()) {
                    instance.socket.shutdownInput();
                    instance.socket.shutdownOutput();
                    instance.socket.close();
                }

            } catch (IOException e) {
                System.out.println("[Network] Lỗi xảy ra khi đóng luồng kết nối cũ: " + e.getMessage());
            } finally {
                instance = null;

                System.gc();

                System.out.println("[Network] Đã dọn dẹp Socket thành công. Sẵn sàng cho lượt Login tiếp theo.");
            }
        }
    }
}