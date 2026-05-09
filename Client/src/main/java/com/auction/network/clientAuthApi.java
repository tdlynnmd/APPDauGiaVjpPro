package com.auction.network;
/**
 * ClientAuthApi là class phía Client chuyên gửi yêu cầu đăng nhập sang Server.
 */

import com.auction.dto.LoginRequest;        // LoginRequest: dữ liệu login gửi sang Server
import com.auction.dto.LoginResponse;       // LoginResponse: kết quả login nhận từ Server
import com.auction.dto.SocketRequest;       // SocketRequest: phong bì chứa action + body
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class clientAuthApi {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT =  5555;

    private final Gson gson = new Gson();

    public LoginResponse login (String usernameOrEmail, String password) {      // Hàm nay được LoginController gọi khi người dùng bấm nút login
        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);               // Mở cổng kết nối tới Sever, nếu Sever chưa chạy, sẽ lỗi đi vào catch
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);       // gửi du liệu tu Client sang Sever
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));     // đọc dữ liệu Sever gi về
        )
        {
            LoginRequest loginRequest = new LoginRequest(usernameOrEmail, password);    // chứa username/email và password.
            JsonObject loginBody = gson.toJsonTree(loginRequest).getAsJsonObject();     // chuyển LoginRequest thành JSON Object
            SocketRequest socketRequest = new SocketRequest("LOGIN", loginBody); // Bọc LoginRequest vào SocketRequest, để khi sever nhìn thấy action = "LOGIN" phải biết gọi AuthController.login()
            String requestJson = gson.toJson(socketRequest);                        // Chuyển SocketRequest thành chuỗi JSON để ửi qua mạng
            writer.println(requestJson);                                            // gui request dạng chuỗi JSON sang Sever

            String responseJson = reader.readLine();                                // Đợi Sever trả response về
            if (responseJson == null || responseJson.isBlank()) {
                return LoginResponse.failure(
                        "The server did not return any data.",
                        "EMPTY_RESPONSE"
                );
            }
            return gson.fromJson(responseJson, LoginResponse.class);     // Chuyển JSON Server trả về thành LoginResponse.
        } catch(Exception e){
            return LoginResponse.failure(
                    "Cannot connect to the server. Please check whether the server is running.",
                    "CONNECTION_ERROR"
            );
        }
    }
}