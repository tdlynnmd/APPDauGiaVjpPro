package com.auction.controller;

import com.auction.dto.LoginRequest;
import com.auction.dto.LoginResultDTO;
import com.auction.dto.RegisterRequest;
import com.auction.dto.UserDTO;
import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;
import com.auction.exception.AuthErrorCode;
import com.auction.exception.AuthenticationException;
import com.auction.exception.ValidationErrorCode;
import com.auction.exception.ValidationException;
import com.auction.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class AuthControllerTest {

    private AuthController authController;
    private FakeAuthService authService;

    @BeforeEach
    void setUp() throws Exception {
        authController = new AuthController();

        authService = new FakeAuthService();

        injectField(authController, "authService", authService);
    }

    // Inject fake AuthService để test không gọi DB/service thật
    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // Tạo UserDTO mẫu
    private UserDTO sampleUserDTO() {
        return new UserDTO(
                "user-1",
                "bidder01",
                "bidder01@example.com",
                UserRole.BIDDER,
                UserStatus.ACTIVE,
                1000.0,
                0.0
        );
    }

    // Check đúng mã lỗi ValidationException
    private void assertValidationError(ValidationException exception, ValidationErrorCode expectedError) {
        assertEquals(expectedError.getCode(), exception.getErrorCode());
    }

    // Check đúng mã lỗi AuthenticationException
    private void assertAuthError(AuthenticationException exception, AuthErrorCode expectedError) {
        assertEquals(expectedError.getCode(), exception.getErrorCode());
    }

    // Fake AuthService để kiểm tra controller truyền tham số đúng
    private static class FakeAuthService extends AuthService {
        UserDTO loginResult;
        UserDTO registerResult;

        String lastLoginUsernameOrEmail;
        String lastLoginPassword;

        String lastRegisterUsername;
        String lastRegisterPassword;
        String lastRegisterEmail;
        UserRole lastRegisterRole;

        String lastLogoutUserId;

        boolean loginShouldThrow = false;
        boolean registerShouldThrow = false;
        boolean logoutShouldThrow = false;

        @Override
        public UserDTO login(String usernameOrEmail, String password) throws AuthenticationException {
            lastLoginUsernameOrEmail = usernameOrEmail;
            lastLoginPassword = password;

            if (loginShouldThrow) {
                throw new AuthenticationException(AuthErrorCode.INVALID_CREDENTIALS);
            }

            return loginResult;
        }

        @Override
        public UserDTO register(String username, String password, String email, UserRole role) throws AuthenticationException {
            lastRegisterUsername = username;
            lastRegisterPassword = password;
            lastRegisterEmail = email;
            lastRegisterRole = role;

            if (registerShouldThrow) {
                throw new AuthenticationException(AuthErrorCode.USERNAME_ALREADY_EXISTS);
            }

            return registerResult;
        }

        @Override
        public void logout(String userId) throws AuthenticationException {
            lastLogoutUserId = userId;

            if (logoutShouldThrow) {
                throw new AuthenticationException(AuthErrorCode.USER_NOT_FOUND);
            }
        }
    }

    // =========================================================
    // login()
    // =========================================================

    // login request null phải ném BAD_REQUEST
    @Test
    void loginShouldThrowWhenRequestIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            authController.login(null);
        });

        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    // login hợp lệ phải gọi AuthService.login và trả LoginResultDTO
    @Test
    void loginShouldReturnLoginResultWhenRequestIsValid() throws AuthenticationException {
        UserDTO userDTO = sampleUserDTO();
        authService.loginResult = userDTO;

        LoginRequest request = new LoginRequest(
                "bidder01",
                "password123"
        );

        LoginResultDTO result = authController.login(request);

        assertNotNull(result);
        assertNotNull(result.getToken());
        assertFalse(result.getToken().isBlank());
        assertSame(userDTO, result.getUser());

        assertEquals("bidder01", authService.lastLoginUsernameOrEmail);
        assertEquals("password123", authService.lastLoginPassword);
    }

    // login bằng email cũng phải truyền đúng usernameOrEmail xuống service
    @Test
    void loginShouldPassEmailToService() throws AuthenticationException {
        UserDTO userDTO = sampleUserDTO();
        authService.loginResult = userDTO;

        LoginRequest request = new LoginRequest(
                "bidder01@example.com",
                "password123"
        );

        LoginResultDTO result = authController.login(request);

        assertNotNull(result);
        assertSame(userDTO, result.getUser());

        assertEquals("bidder01@example.com", authService.lastLoginUsernameOrEmail);
        assertEquals("password123", authService.lastLoginPassword);
    }

    // login service ném AuthenticationException thì controller không được nuốt lỗi
    @Test
    void loginShouldPropagateAuthenticationExceptionFromService() {
        authService.loginShouldThrow = true;

        LoginRequest request = new LoginRequest(
                "wrong-user",
                "wrong-password"
        );

        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            authController.login(request);
        });

        assertAuthError(exception, AuthErrorCode.INVALID_CREDENTIALS);
        assertEquals("wrong-user", authService.lastLoginUsernameOrEmail);
        assertEquals("wrong-password", authService.lastLoginPassword);
    }

    // login vẫn truyền request có field null xuống service, validate sâu để AuthService xử lý
    @Test
    void loginShouldPassNullFieldsToServiceForServiceValidation() throws AuthenticationException {
        authService.loginResult = sampleUserDTO();

        LoginRequest request = new LoginRequest(
                null,
                null
        );

        LoginResultDTO result = authController.login(request);

        assertNotNull(result);
        assertNull(authService.lastLoginUsernameOrEmail);
        assertNull(authService.lastLoginPassword);
    }

    // mỗi lần login thành công phải tạo token khác nhau
    @Test
    void loginShouldGenerateDifferentTokensForDifferentCalls() throws AuthenticationException {
        authService.loginResult = sampleUserDTO();

        LoginRequest request = new LoginRequest(
                "bidder01",
                "password123"
        );

        LoginResultDTO first = authController.login(request);
        LoginResultDTO second = authController.login(request);

        assertNotNull(first.getToken());
        assertNotNull(second.getToken());
        assertNotEquals(first.getToken(), second.getToken());
    }

    // =========================================================
    // register()
    // =========================================================

    // register request null phải ném BAD_REQUEST
    @Test
    void registerShouldThrowWhenRequestIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            authController.register(null);
        });

        assertValidationError(exception, ValidationErrorCode.BAD_REQUEST);
    }

    // register hợp lệ phải gọi AuthService.register và trả UserDTO
    @Test
    void registerShouldReturnUserDTOWhenRequestIsValid() throws AuthenticationException {
        UserDTO userDTO = sampleUserDTO();
        authService.registerResult = userDTO;

        RegisterRequest request = new RegisterRequest(
                "bidder01",
                "password123",
                "bidder01@example.com",
                UserRole.BIDDER
        );

        UserDTO result = authController.register(request);

        assertSame(userDTO, result);

        assertEquals("bidder01", authService.lastRegisterUsername);
        assertEquals("password123", authService.lastRegisterPassword);
        assertEquals("bidder01@example.com", authService.lastRegisterEmail);
        assertEquals(UserRole.BIDDER, authService.lastRegisterRole);
    }

    // register seller phải truyền đúng role SELLER xuống service
    @Test
    void registerShouldPassSellerRoleToService() throws AuthenticationException {
        UserDTO sellerDTO = new UserDTO(
                "seller-1",
                "seller01",
                "seller01@example.com",
                UserRole.SELLER,
                UserStatus.ACTIVE,
                0.0,
                0.0
        );

        authService.registerResult = sellerDTO;

        RegisterRequest request = new RegisterRequest(
                "seller01",
                "password123",
                "seller01@example.com",
                UserRole.SELLER
        );

        UserDTO result = authController.register(request);

        assertSame(sellerDTO, result);
        assertEquals(UserRole.SELLER, authService.lastRegisterRole);
    }

    // register service ném AuthenticationException thì controller không được nuốt lỗi
    @Test
    void registerShouldPropagateAuthenticationExceptionFromService() {
        authService.registerShouldThrow = true;

        RegisterRequest request = new RegisterRequest(
                "sameName",
                "password123",
                "user@example.com",
                UserRole.BIDDER
        );

        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            authController.register(request);
        });

        assertAuthError(exception, AuthErrorCode.USERNAME_ALREADY_EXISTS);

        assertEquals("sameName", authService.lastRegisterUsername);
        assertEquals("password123", authService.lastRegisterPassword);
        assertEquals("user@example.com", authService.lastRegisterEmail);
        assertEquals(UserRole.BIDDER, authService.lastRegisterRole);
    }

    // register vẫn truyền field null xuống service, validate sâu để AuthService xử lý
    @Test
    void registerShouldPassNullFieldsToServiceForServiceValidation() throws AuthenticationException {
        authService.registerResult = sampleUserDTO();

        RegisterRequest request = new RegisterRequest(
                null,
                null,
                null,
                null
        );

        UserDTO result = authController.register(request);

        assertNotNull(result);
        assertNull(authService.lastRegisterUsername);
        assertNull(authService.lastRegisterPassword);
        assertNull(authService.lastRegisterEmail);
        assertNull(authService.lastRegisterRole);
    }

    // =========================================================
    // logout()
    // =========================================================

    // logout phải gọi AuthService.logout với đúng userId
    @Test
    void logoutShouldCallServiceWithUserId() throws AuthenticationException {
        authController.logout("user-1");

        assertEquals("user-1", authService.lastLogoutUserId);
    }

    // logout với null vẫn truyền xuống service để AuthService validate
    @Test
    void logoutShouldPassNullUserIdToServiceForServiceValidation() throws AuthenticationException {
        authController.logout(null);

        assertNull(authService.lastLogoutUserId);
    }

    // logout service ném AuthenticationException thì controller không được nuốt lỗi
    @Test
    void logoutShouldPropagateAuthenticationExceptionFromService() {
        authService.logoutShouldThrow = true;

        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            authController.logout("missing-user");
        });

        assertAuthError(exception, AuthErrorCode.USER_NOT_FOUND);
        assertEquals("missing-user", authService.lastLogoutUserId);
    }
}