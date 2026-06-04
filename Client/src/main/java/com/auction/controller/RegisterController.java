package com.auction.controller;

import com.auction.dto.SocketResponse;
import com.auction.enums.UserRole;
import com.auction.network.ClientAuthApi;
import com.auction.util.SceneNavigator;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

import java.util.Objects;

/**
 * Bộ điều khiển (Controller) hoặc lớp tiện ích RegisterController xử lý giao diện Client JavaFX.
 */
public class RegisterController {

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private ComboBox<UserRole> roleComboBox;

    @FXML
    private Label errorLabel;

    @FXML
    private Pane rootContainer;

    /**
     * initialize() được JavaFX tự động gọi sau khi load register.fxml.
     * Nhiệm vụ:
     - Tự động áp dụng theme hiện tại của hệ thống tổng.
     - Đưa danh sách role vào ComboBox.
     - Mặc định chọn BIDDER.
     - Ẩn label lỗi ban đầu.

     Không đưa ADMIN vào đây vì tài khoản admin không nên cho đăng ký tự do.
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

        roleComboBox.getItems().setAll(UserRole.BIDDER, UserRole.SELLER);
        roleComboBox.setValue(UserRole.BIDDER);

        errorLabel.setVisible(true);
        errorLabel.setText("");
    }

    /**
     handleRegister() được gọi khi người dùng bấm nút Register.
     * Luồng xử lý:
     * 1. Lấy dữ liệu từ form.
     * 2. Kiểm tra các lỗi nhập liệu cơ bản.
     * 3. Gửi request REGISTER sang Server.
     * 4. Nhận SocketResponse.
     * 5. Nếu thành công thì chuyển về Login.
     * 6. Nếu thất bại thì hiển thị lỗi.
     */
    @FXML
    public void handleRegister(ActionEvent event) {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        UserRole role = roleComboBox.getValue();

        errorLabel.setVisible(true);

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please fill in all registration information!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match!");
            return;
        }

        if (role == null) {
            showError("Please select an account role!");
            return;
        }

        ClientAuthApi authApi = new ClientAuthApi();
        SocketResponse response = authApi.register(username, password, email, role);

        if (response.isSuccess()) {
            showSuccess("Registration successful! Returning to the login screen.");

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Registration Successful");
            alert.setHeaderText(null);
            alert.setContentText("Your account has been created. You can log in now.");
            alert.showAndWait();

            SceneNavigator.showLogin();
        } else {
            showError(response.getMessage());
        }
    }

    /**
     * Chuyển từ màn hình Register về Login.
     * Hàm này được gọi khi người dùng bấm hyperlink "Login?".
     */
    @FXML
    public void goToLogin(ActionEvent event) {
        SceneNavigator.showLogin();
    }

    /**
     * Đổi theme sáng/tối cho màn hình Register.
     * Đây chỉ là xử lý giao diện, không liên quan đến nghiệp vụ đăng ký.
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
            e.printStackTrace();
            showError("CSS file not found: " + path);
        }
    }

    /**
     * Hiển thị lỗi lên label.
     */
    private void showError(String message) {
        errorLabel.setVisible(true);
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: red;");
    }

    /**
     * Hiển thị thông báo thành công lên label.
     */
    private void showSuccess(String message) {
        errorLabel.setVisible(true);
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: green;");
    }
}