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
 Lỗi nghiệp vụ để RequestDispatcher bắt và bọc vào SocketResponse

 Luồng:
 Controller nhận RequestDTO, gọi AuthService, rồi trả dữ liệu thành công
 Controller ko tạo SocketResponse
 RequestDispatcher là nơi chuẩn hóa Response gửi qua socket
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
     -> Nếu thành công:
      - Trả LoginResultDTO gồm token và UserDTO.

     -> Nếu thất bại:
      - AuthService ném AuthenticationException.
      - RequestDispatcher sẽ bắt exception và trả SocketResponse.failure().     */
    public LoginResultDTO login(LoginRequest request) throws AuthenticationException {

            // Gọi service để kiểm tra tên đăng nhập, mật khẩu
            UserDTO user = authService.login(request.getUsernameOrEmail(), request.getPassword());

            // Tạo token tạm thời cho phiên đăng nhập.
            // Sau này nếu làm bảo mật kỹ hơn có thể tạo TokenService riêng.
            String token = UUID.randomUUID().toString();
            return new LoginResultDTO(token, user);


    }

    /**
      Xử lý đăng ký phía Server.

      Luồng:
      RequestDispatcher.handleRegister()
      -> AuthController.register()
      -> AuthService.register()
      -> Nếu thành công:
      - Trả UserDTO của user vừa tạo.

      -> Nếu thất bại:
      - AuthService ném AuthenticationException.
     */
    public UserDTO register(RegisterRequest request) throws AuthenticationException {
        return authService.register(
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                request.getRole()
        );
    }


    /**
     Xử lý đăng xuất phía Server.

     AuthController chỉ gọi AuthService để kiểm tra userId hợp lệ.
     Việc xóa connection cụ thể khỏi ConnectionManage sẽ do RequestDispatcher làm,
     vì RequestDispatcher đang cầm ClientSession hiện tại.

     */
    public void logout(String userId) throws AuthenticationException {
        authService.logout(userId);
    }
}
