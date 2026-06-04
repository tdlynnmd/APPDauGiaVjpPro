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
 * Bộ điều khiển (Controller) hoặc lớp tiện ích LoginController xử lý giao diện Client JavaFX.
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

    /**
     * initialize() được JavaFX tự động gọi sau khi load login.fxml.
     * Nhiệm vụ:
     - Tự động áp dụng theme hiện tại của hệ thống tổng từ SceneNavigator.
     - Ẩn label lỗi ban đầu.
     */
    @FXML
    public void initialize() {
        rootContainer.getStylesheets().clear();
        String currentPath = SceneNavigator.isAppDarkMode
                ? "/com/auction/client/view/dark.css"
                : "/com/auction/client/view/light.css";
        try {
            String css = Objects.requireNonNull(getClass().getResource(currentPath)).toExternalForm();
            rootContainer.getStylesheets().add(css);
        } catch (Exception e) {
            System.out.println("Không thể nạp theme hệ thống: " + currentPath);
        }

        errorLabel.setVisible(false);
    }

    /**
     * Đổi theme sáng/tối cho màn hình Login.
     * Đây chỉ là xử lý giao diện, không liên quan đến nghiệp vụ đăng nhập.
     */
    @FXML
    public void toggleTheme(ActionEvent event) {
        rootContainer.getStylesheets().clear();

        String path = SceneNavigator.isAppDarkMode
                ? "/com/auction/client/view/light.css"
                : "/com/auction/client/view/dark.css";

        try {
            String css = Objects.requireNonNull(getClass().getResource(path)).toExternalForm();
            rootContainer.getStylesheets().add(css);

            SceneNavigator.isAppDarkMode = !SceneNavigator.isAppDarkMode;
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

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter your full username and passwords.");
            return;
        }

        ClientAuthApi authApi = new ClientAuthApi();
        SocketResponse response = authApi.login(username, password);

        if (response == null) {
            showError("Server did not return a valid response.");
            return;
        }

        if (!response.isSuccess()) {
            showError(response.getMessage());
            return;
        }

        LoginResultDTO loginResult = authApi.parseBody(response, LoginResultDTO.class);

        if (loginResult == null || loginResult.getUser() == null) {
            showError("Server returned an invalid login response.");
            return;
        }

        showSuccess("Đăng nhập thành công. Đang chuyển màn hình...");

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