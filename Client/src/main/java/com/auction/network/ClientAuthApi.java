package com.auction.network;
/**
 ClientAuthApi là class phía Client chuyên gửi yêu cầu đăng nhập sang Server.
 Gửi LOGIN.
 Gửi REGISTER.
 Gửi LOGOUT.
 Chuyển DTO thành JSON.
 Đọc JSON response từ Server rồi chuyển lại thành Response DTO.
 */

import com.auction.dto.*;
import com.auction.enums.UserRole;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.PrintWriter;

public class ClientAuthApi {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5555;

    private final Gson gson = new Gson();

    public LoginResponse login(String usernameOrEmail, String password) {      // Hàm nay được LoginController gọi khi người dùng bấm nút login
        try {
            ClientNetworkManager network = ClientNetworkManager.getInstance();
            PrintWriter writer = network.getWriter();
            BufferedReader reader = network.getReader();

            LoginRequest loginRequest = new LoginRequest(usernameOrEmail, password);    // chứa username/email và password.
            JsonObject loginBody = gson.toJsonTree(loginRequest).getAsJsonObject();     // chuyển LoginRequest thành JSON Object
            SocketRequest socketRequest = new SocketRequest("LOGIN", loginBody); // Bọc LoginRequest vào SocketRequest, để khi sever nhìn thấy action = "LOGIN" phải biết gọi AuthController.login()
            String requestJson = gson.toJson(socketRequest);// Chuyển SocketRequest thành chuỗi JSON để gửi qua mạng

            writer.println(requestJson);                                            // gui request dạng chuỗi JSON sang Sever
            String responseJson = reader.readLine();                                // Đợi Sever trả response về
            if (responseJson == null || responseJson.isBlank()) {
                return LoginResponse.failure(
                        "The server did not return any data.",
                        "EMPTY_RESPONSE"
                );
            }
            return gson.fromJson(responseJson, LoginResponse.class);     // Chuyển JSON Server trả về thành LoginResponse.
        } catch (Exception e) {
            e.printStackTrace();
            return LoginResponse.failure(
                    "Cannot connect to the server. Please check whether the server is running.",
                    "CONNECTION_ERROR"
            );
        }
    }

    /**
     * Gửi request REGISTER sang sever
     * Luồng:
     * RegisterController
     * -> ClientAuthAPI.register()
     * -> SocketRequest(action = "REGISTER")
     * -> Server RequestDispatcher.handleRegister()
     */
    public RegisterResponse register(String username, String password, String email, UserRole role) {
        try {
            ClientNetworkManager network = ClientNetworkManager.getInstance();
            PrintWriter writer = network.getWriter();
            BufferedReader reader = network.getReader();

            // Tạo DTO chứa dữ liệu đăng kí tu form.
            RegisterRequest registerRequest = new RegisterRequest(username, password, email, role);

            // Convert RegisterRequest thành JSON body
            JsonObject body = gson.toJsonTree(registerRequest).getAsJsonObject();

            // Gắn action REGISTER dẻ Server biết đây la yêu cầu đăng ký
            SocketRequest socketRequest = new SocketRequest("REGISTER", body);
            String requestJson = gson.toJson(socketRequest);

            // Gửi request sang Server
            writer.println(requestJson);
            // Đọc response Server trả về.
            String responseJson = reader.readLine();

            if (responseJson == null || responseJson.isBlank()) {
                return RegisterResponse.failure(
                        "The server did not return any data.",
                        "EMPTY_RESPONSE"
                );
            }

            // Convert JSON thành RegisterResponse.
            return gson.fromJson(responseJson, RegisterResponse.class);

        } catch (Exception e) {
            e.printStackTrace();

            return RegisterResponse.failure(
                    "Cannot connect to the server. Please check whether the server is running.",
                    "CONNECTION_ERROR"
            );
        }
    }

    /**
     * Gửi request LOGOUT sang Server
     * Luồng:
     * DashboardController.handleLogout()
     * -> clientAuthApi.logout()
     * -> SocketRequest(action = "LOGOUT")
     * -> Server xóa session khỏi ConnectionManage
     */
    public LogoutResponse logout(String userId) {
        try {
            ClientNetworkManager network = ClientNetworkManager.getInstance();
            PrintWriter writer = network.getWriter();
            BufferedReader reader = network.getReader();

            // LogoutRequest chỉ cần userId.
            LogoutRequest logoutRequest = new LogoutRequest(userId);

            JsonObject body = gson.toJsonTree(logoutRequest).getAsJsonObject();

            // Gắn action LOGOUT để Server biết cần xử lý đăng xuất.
            SocketRequest socketRequest = new SocketRequest("LOGOUT", body);

            String requestJson = gson.toJson(socketRequest);

            writer.println(requestJson);

            String responseJson = reader.readLine();

            if (responseJson == null || responseJson.isBlank()) {
                return LogoutResponse.failure(
                        "The server did not return any data.",
                        "EMPTY_RESPONSE"
                );
            }

            return gson.fromJson(responseJson, LogoutResponse.class);

        } catch (Exception e) {
            e.printStackTrace();

            return LogoutResponse.failure(
                    "Cannot connect to the server. Please check whether the server is running.",
                    "CONNECTION_ERROR"
            );
        }
    }
}

