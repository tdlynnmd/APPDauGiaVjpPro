package com.auction.controller;

import com.auction.dto.LoginRequest;
import com.auction.dto.LoginResponse;
import com.auction.dto.UserDTO;
import com.auction.exception.AuthenticationException;
import com.auction.service.AuthService;
import java.util.UUID;

/**
 AuthController là “quầy tiếp nhận” của Server.
 Nó không tự kiểm tra mật khẩu, không tự tìm user, không tự xử lý nghiệp vụ sâu.
 Nó chỉ nhận dữ liệu login từ Client, gọi AuthService
 Tại AuthService, sẽ kiểm tra su dung sai cua thong tin login
 , rồi đóng gói kết quả trả lại Client.
 */

public class AuthController {
    private final AuthService authService;
    public AuthController(){
        this.authService = new AuthService();
    }
    public LoginResponse login(LoginRequest request){
        try{
            UserDTO user = authService.login(
                    request.getUsernameOrEmail(),
                    request.getPassword()
            );
            String token = UUID.randomUUID().toString();
            return LoginResponse.success(user, token);
        }
        catch(AuthenticationException e){
            return LoginResponse.failure(e.getMessage(), "AUTH_ERROR");
        }
    }
}
