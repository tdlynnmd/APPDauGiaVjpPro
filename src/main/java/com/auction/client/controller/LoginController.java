package com.auction.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    // Thẻ @FXML giống như việc "cắm jack" nối với cái fx:id bên SceneBuilder
    @FXML
    private TextField usernameField; // Cầm trịch ô nhập tài khoản

    @FXML
    private PasswordField passwordField; // Cầm trịch ô nhập mật khẩu

    @FXML
    private Label errorLabel; // Cầm trịch dòng chữ báo lỗi

    // Đây chính là hàm sẽ chạy khi bấm nút (do mình đã đặt On Action = handleLogin)
    @FXML
    public void handleLogin(ActionEvent event) {

        // 1. LẤY DỮ LIỆU: Moi chữ từ 2 cái ô nhập liệu ra
        String username = usernameField.getText();
        String password = passwordField.getText();

        // 2. KIỂM TRA SƠ BỘ: Nếu lười không nhập gì mà cứ bấm nút
        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
            errorLabel.setStyle("-fx-text-fill: red;"); // Hiện chữ màu đỏ
            return; // Đuổi về, không cho chạy tiếp xuống dưới
        }

        // 3. ĐIỀU PHỐI (Gọi Người số 2)
        // LƯU Ý: Vì Người 2 chưa làm xong AuthService, mình tạm thời code "cứng" (Mock)
        // để bạn tự test xem giao diện có chạy đúng không.
        // Sau này Người 2 làm xong, bạn sẽ thay cụm if này bằng hàm của Người 2 nhé.

        if (username.equals("admin1") && password.equals("123456")) {
            // Nếu Mật khẩu đúng:
            errorLabel.setText("Đăng nhập thành công! Đang chuyển màn hình...");
            errorLabel.setStyle("-fx-text-fill: green;"); // Báo chữ màu xanh lá

            // Chỗ này bạn sẽ gọi code của Người 4 để chuyển sang Dashboard
            // SceneNavigator.showDashboard();
        } else {
            // Nếu Mật khẩu sai:
            errorLabel.setText("Sai tài khoản hoặc mật khẩu. Vui lòng thử lại!");
            errorLabel.setStyle("-fx-text-fill: red;"); // Chửi bằng chữ màu đỏ
        }
    }
}