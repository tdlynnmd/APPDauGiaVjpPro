package com.auction.server.network;
/**
 ClientHandler xử lý request từ một Client cụ thể.
 Khi SocketServer nhận kết nối, clientHandler sẽ nhận từng request rồi chia xuống cho Controller ở sever thực hiện
 */

import com.auction.dto.LoginRequest;
import com.auction.dto.LoginResponse;       //Server dùng DTO chung với Client.
import com.auction.dto.SocketRequest;
import com.auction.server.controller.AuthController;      // ClientHandler sẽ gọi AuthController để xử lý login.
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {            //ClientHandler implements Runnable để có thể chạy trong Thread.
    private final Socket socket;                            //Đây là kết nối tới một Client cụ thể.
    private final Gson gson = new Gson();                   //Dùng Gson để đọc/ghi JSON.

    private final AuthController authController = new AuthController();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader reader = new BufferedReader(         // Đọc dữ liệu Client gửi lên.
                        new InputStreamReader(socket.getInputStream())
                );
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)    // Gửi dữ liệu từ Server về Client.
        ) {
            String requestJson = reader.readLine();

            if (requestJson == null || requestJson.isBlank()) {
                LoginResponse response = LoginResponse.failure(
                        "Request rỗng",
                        "EMPTY_REQUEST"
                );
                writer.println(gson.toJson(response));
                return;
            }

            SocketRequest socketRequest = gson.fromJson(
                    requestJson,
                    SocketRequest.class
            );

            Object response = handleRequest(socketRequest);

            String responseJson = gson.toJson(response);

            writer.println(responseJson);

        } catch (Exception e) {
            System.err.println("[Server] Lỗi xử lý client: " + e.getMessage());
        }
    }

    private Object handleRequest(SocketRequest socketRequest) {         //Hàm phân loại request.
        if (socketRequest == null || socketRequest.getAction() == null) {
            return LoginResponse.failure(
                    "Request không hợp lệ",
                    "BAD_REQUEST"
            );
        }

        if ("LOGIN".equals(socketRequest.getAction())) {
            LoginRequest loginRequest = gson.fromJson(
                    socketRequest.getBody(),
                    LoginRequest.class
            );

            return authController.login(loginRequest);
        }

        return LoginResponse.failure(
                "Action không được hỗ trợ: " + socketRequest.getAction(),
                "UNSUPPORTED_ACTION"
        );
    }
}