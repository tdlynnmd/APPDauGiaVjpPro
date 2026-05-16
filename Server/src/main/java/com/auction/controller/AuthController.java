package com.auction.controller;

import com.auction.dto.*;
import com.auction.exception.AuthenticationException;
import com.auction.service.AuthService;
import java.util.UUID;

/**
  AuthController là Controller phía Server cho nhóm chức năng xác thực
 *
 Nhận request đã được RequestDispatcher parse.
 Gọi AuthService để xử lý nghiệp vụ.
 Bọc kết quả thành Response DTO.
 Bắt exception và chuyển thành response thất bại.
 */

public class AuthController {
    private final AuthService authService;
    public AuthController(){
        this.authService = new AuthService();
    }

    /**
     Xử lí đăng nhập phía Server
     Luồng:
     RequestDispatcher.handleLogin()
     -> AuthController.login()
     -> AuthService.login()
     -> LoginResponse trả về Client
     */
    public LoginResponse login(LoginRequest request) {
        try {
            // Gọi service để kiểm tra tên đăng nhập, mật khẩu
            UserDTO user = authService.login(request.getUsernameOrEmail(), request.getPassword());

            // Tạo token tạm thời cho phiên đăng nhập.
            // Sau này nếu làm bảo mật kỹ hơn có thể tạo TokenService riêng.
            String token = UUID.randomUUID().toString();

            return LoginResponse.success(user, token);

        } catch (AuthenticationException e) {
            // Chuyển lỗi nghiệp vụ thành response để Client hiển thị.
            return LoginResponse.failure(e.getMessage(), e.getErrorCode());
        }
    }

    /**
      Xử lý đăng ký phía Server.

      Luồng:
      RequestDispatcher.handleRegister()
      -> AuthController.register()
      -> AuthService.register()
      -> RegisterResponse trả về Client
     */
    public RegisterResponse register(RegisterRequest request){
        try{
            // AuthService chịu trách nhiệm validate và tạo user
            UserDTO user = authService.register(request.getUsername(), request.getPassword(), request.getEmail(), request.getRole());

            return RegisterResponse.success(user);
        }
        catch (AuthenticationException e) {
            // Lỗi có chủ đích, ví dụ username trùng, email sai format, password yếu.
            return RegisterResponse.failure(e.getMessage(), e.getErrorCode());

        } catch (Exception e) {
            // Lỗi ngoài dự kiến, ví dụ role chưa đăng ký trong UserFactory.
            return RegisterResponse.failure(e.getMessage(), "REGISTER_ERROR");
        }
    }

    /**
     Xử lý đăng xuất phía Server.

     AuthController chỉ gọi AuthService để kiểm tra userId hợp lệ.
     Việc xóa connection cụ thể khỏi ConnectionManage sẽ do RequestDispatcher làm,
     vì RequestDispatcher đang cầm ClientSession hiện tại.
     */
    public LogoutResponse logout(String userId) {
        try {
            authService.logout(userId);

            return LogoutResponse.success();

        } catch (AuthenticationException e) {
            return LogoutResponse.failure(e.getMessage(), e.getErrorCode());

        } catch (Exception e) {
            return LogoutResponse.failure(e.getMessage(), "LOGOUT_ERROR");
        }
    }
}
