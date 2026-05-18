package com.auction.controller;

import com.auction.dto.LoginResultDTO;
import com.auction.dto.SocketResponse;
import com.auction.network.ClientAuthApi;
import com.auction.util.ClientSession;
import com.auction.util.SceneNavigator;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

import java.util.Objects;

/**
 * LoginController là Controller phía Client cho màn hình đăng nhập.
 *
 * Vai trò:
 * - Nhận username/email và password từ giao diện.
 * - Kiểm tra sơ bộ dữ liệu trước khi gửi lên Server.
 * - Gọi ClientAuthApi.login() để gửi request LOGIN qua socket.
 * - Nhận SocketResponse từ Server.
 * - Nếu đăng nhập thành công, lấy LoginResultDTO từ response.body và lưu session phía Client.
 *
 * Lưu ý:
 * - Controller chỉ xử lý giao diện và điều hướng.
 * - Controller không kiểm tra password đúng/sai.
 * - Controller không tự tạo token.
 * - Những nghiệp vụ đó thuộc về AuthService phía Server.
 */
public class LoginController {
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Pane rootContainer;

    // Biến theo dõi theme hiện tại.
    // false nghĩa là đang ở light mode, true nghĩa là đang ở dark mode.
    private boolean isDarkMode = false;

    /**
     * Đổi theme sáng/tối cho màn hình Login.
     * Đây chỉ là xử lý giao diện, không liên quan đến nghiệp vụ đăng nhập.
     */
    @FXML
    public void toggleTheme(ActionEvent event) {
        rootContainer.getStylesheets().clear();

        String path = isDarkMode
                ? "/com/auction/client/view/light.css"
                : "/com/auction/client/view/dark.css";

        try {
            String css = Objects.requireNonNull(getClass().getResource(path)).toExternalForm();
            rootContainer.getStylesheets().add(css);
            // Đảo trạng thái để lần bấm sau đổi sang theme còn lại.
            isDarkMode = !isDarkMode;
        } catch (Exception e) {
            System.out.println("Không tìm thấy file CSS tại " + path);
            e.printStackTrace();
        }
    }

    /**
     * handleLogin() được gọi khi người dùng bấm nút Login.
     *
     * Luồng xử lý:
     * 1. Lấy username/email và password từ form.
     * 2. Kiểm tra dữ liệu rỗng ở Client.
     * 3. Gửi request LOGIN sang Server.
     * 4. Nhận SocketResponse.
     * 5. Nếu thất bại thì hiển thị lỗi.
     * 6. Nếu thành công thì parse response.body thành LoginResultDTO.
     * 7. Lưu token + user vào ClientSession.
     * 8. Chuyển sang Dashboard.
     */
    @FXML
    public void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        errorLabel.setVisible(true);

        // Kiểm tra rỗng ở Client để tránh gửi request thiếu dữ liệu lên Server.
        // Server vẫn phải validate lại, vì dữ liệu từ Client không bao giờ được tin tuyệt đối.
        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ tài khoản và mật khẩu.");
            return;
        }

        // Gọi lớp API phía Client để gửi request LOGIN qua socket.
        // Controller không tự làm việc với Socket trực tiếp.
        ClientAuthApi authApi = new ClientAuthApi();
        SocketResponse response = authApi.login(username, password);

        if (response == null) {
            showError("Server did not return a valid response.");
            return;
        }

        if (!response.isSuccess()) {
            // Nếu Server báo lỗi, hiển thị message do Server trả về.
            // Ví dụ: sai tài khoản, sai mật khẩu, tài khoản bị khóa.
            showError(response.getMessage());
            return;
        }

        // LOGIN thành công thì response.body chứa LoginResultDTO.
        // LoginResultDTO gồm token và UserDTO an toàn để lưu phía Client.
        LoginResultDTO loginResult = authApi.parseBody(response, LoginResultDTO.class);

        if (loginResult == null || loginResult.getUser() == null) {
            showError("Server returned an invalid login response.");
            return;
        }

        showSuccess("Đăng nhập thành công. Đang chuyển màn hình...");

        // Lưu session phía Client để các màn hình sau biết user hiện tại là ai.
        ClientSession.saveLoginSession(
                loginResult.getToken(),
                loginResult.getUser()
        );

        SceneNavigator.showDashboard();
    }

    /**
     * Chuyển từ màn hình Login sang Register.
     * Hàm này được gọi khi người dùng bấm hyperlink "Register?".
     */
    @FXML
    public void goToRegister(ActionEvent event) {
        SceneNavigator.showRegister();
    }

    /**
     * Hiển thị lỗi lên label.
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: red;");
    }

    /**
     * Hiển thị thông báo thành công lên label.
     */
    private void showSuccess(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: green;");
    }
}