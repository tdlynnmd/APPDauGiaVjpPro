package com.auction.controller;

import com.auction.dto.LoginResponse;
import com.auction.network.ClientAuthApi;
import com.auction.util.ClientSession;
import com.auction.util.SceneNavigator;
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
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Hiển thị label thông báo vì trong login.fxml errorLabel đang visible="false"
        errorLabel.setVisible(true);

        // 2. KIỂM TRA SƠ BỘ
        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
            errorLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // 3. LOGIC ĐĂNG NHẬP
        // Gửi username/password sang Server để kiểm tra, không hardcode trong Controller nữa
        ClientAuthApi authApi = new ClientAuthApi();
        LoginResponse response = authApi.login(username, password);

        if (response.isSuccess()) {
            errorLabel.setText("Đăng nhập thành công! Đang chuyển màn hình...");
            errorLabel.setStyle("-fx-text-fill: green;");

            // Lưu phiên đăng nhập để DashboardController biết ai vừa đăng nhập
            ClientSession.saveLoginSession(response.getToken(), response.getUser());

            // Chuyển từ màn hình Login sang Dashboard
            SceneNavigator.showDashboard();
        } else {
            errorLabel.setText(response.getMessage());
            errorLabel.setStyle("-fx-text-fill: red;");
        }
    }
    @FXML
    public void goToRegister(ActionEvent event){
        SceneNavigator.showRegister();
    }
}