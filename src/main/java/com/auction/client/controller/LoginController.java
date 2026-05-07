package com.auction.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane; // Cần thiết để điều khiển nền
import java.util.Objects;

public class LoginController {

    // --- PHẦN CŨ CỦA EM (GIỮ NGUYÊN) ---
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    // --- PHẦN THÊM MỚI ĐỂ ĐỔI THEME ---
    @FXML
    private Pane rootContainer; // fx:id của cái nền ngoài cùng

    private boolean isDarkMode = false; // Biến theo dõi trạng thái màu

    @FXML
    public void toggleTheme(ActionEvent event) {
        // 1. Xóa CSS hiện tại
        rootContainer.getStylesheets().clear();

        // 2. Xác định đường dẫn dựa trên cấu trúc thư mục của em
        String path = isDarkMode ? "/com/auction/client/view/light.css" : "/com/auction/client/view/dark.css";

        try {
            // 3. Nạp file CSS mới
            String css = Objects.requireNonNull(getClass().getResource(path)).toExternalForm();
            rootContainer.getStylesheets().add(css);

            // 4. Đảo trạng thái để lần sau bấm lại nó đổi sang màu kia
            isDarkMode = !isDarkMode;

            System.out.println("Đã đổi sang: " + (isDarkMode ? "Dark Mode" : "Light Mode"));
        } catch (Exception e) {
            System.out.println("Lỗi: Không tìm thấy file CSS tại " + path);
            e.printStackTrace();
        }
    }

    // --- PHẦN CŨ CỦA EM (GIỮ NGUYÊN) ---
    @FXML
    public void handleLogin(ActionEvent event) {
        // 1. LẤY DỮ LIỆU
        String username = usernameField.getText();
        String password = passwordField.getText();

        // 2. KIỂM TRA SƠ BỘ
        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
            errorLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // 3. LOGIC ĐĂNG NHẬP
        if (username.equals("admin1") && password.equals("123456")) {
            errorLabel.setText("Đăng nhập thành công! Đang chuyển màn hình...");
            errorLabel.setStyle("-fx-text-fill: green;");
        } else {
            errorLabel.setText("Sai tài khoản hoặc mật khẩu. Vui lòng thử lại!");
            errorLabel.setStyle("-fx-text-fill: red;");
        }
    }
}