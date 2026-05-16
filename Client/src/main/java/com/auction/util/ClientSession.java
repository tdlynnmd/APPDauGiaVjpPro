package com.auction.util;

import com.auction.dto.UserDTO;     // Sau khi login thành công, Server trả về thông tin user an toàn dưới dạng UserDTO.

/**
 * Lưu trạng thái đăng nhập ở phiá Client
 Sau khi login thành công, Client cần nhớ: token, user hiện tại
 Sau khi LoginController chuyển sang Dashboard, DashboardController cần biết:
 - username là gì
 - role là gì
 - người dùng đã login chưa
 Nếu không có ClientSession, Dashboard không biết ai vừa đăng nhập.
 */
public class ClientSession {
    private static String token;
    private static UserDTO currentUser;

    private ClientSession() {
    }

    public static void saveLoginSession(String newToken, UserDTO user) {    //Gọi hàm này khi login thành công.
        token = newToken;
        currentUser = user;
    }

    public static String getToken() {
        return token;
    }

    public static UserDTO getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {                //Kiểm tra Client đã đăng nhập hay chưa.
        return token != null && currentUser != null;
    }

    public static void clear() {            // xóa session
        token = null;
        currentUser = null;
    }
}
