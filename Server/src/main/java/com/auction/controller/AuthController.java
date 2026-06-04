package com.auction.controller;

import com.auction.dto.*;
import com.auction.exception.AuthenticationException;
import com.auction.exception.ValidationErrorCode;
import com.auction.exception.ValidationException;
import com.auction.service.AuthService;
import java.util.UUID;

/**
 * Bộ điều khiển tiếp nhận các yêu cầu đăng nhập, đăng ký và xác thực tài khoản.
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
        if (request == null) throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Yêu cầu không hợp lệ.");

        UserDTO user = authService.login(request.getUsernameOrEmail(), request.getPassword());

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
        if (request == null) throw new ValidationException(ValidationErrorCode.BAD_REQUEST, "Yêu cầu không hợp lệ.");
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
