package com.auction.controller;

import com.auction.service.ClientSocketService;
import com.auction.util.SceneNavigator;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class IntroController {

    @FXML
    private StackPane introRoot;

    @FXML
    private Label statusLabel;

    @FXML
    private ProgressIndicator loadingIndicator;

    @FXML
    public void initialize() {
        // Khởi tạo luồng ngầm thực hiện việc liên kết Socket để giữ UI không bị đóng băng
        Thread networkInitThread = new Thread(() -> {
            try {
                // Tạo khoảng trễ ngắn 600ms giúp mắt người kịp định hình giao diện Intro
                Thread.sleep(600);

                Platform.runLater(() -> statusLabel.setText("Đang khởi tạo cổng dịch vụ Socket..."));

                /*
                 * ĐOẠN PHÂN TÍCH KẾT NỐI THẬT:
                 * Gọi phương thức khởi tạo tĩnh của bạn. Phương thức này sẽ tự động chạy ngầm
                 * startReaderThread() để kết nối trực tiếp với Server qua ClientNetworkManager.
                 */
                ClientSocketService.getInstance();

                Platform.runLater(() -> statusLabel.setText("Kết nối thành công! Đang thiết lập cấu hình..."));
                Thread.sleep(400);

                // Chuyển về luồng giao diện chính để thực hiện hiệu ứng chuyển màn
                Platform.runLater(this::fadeOutAndSwitch);

            } catch (Exception e) {
                // Tình huống xử lý ngoại lệ nếu máy chủ chưa mở hoặc mất kết nối mạng
                e.printStackTrace();
                Platform.runLater(() -> {
                    loadingIndicator.setStyle("-fx-progress-color: #ffbf00;");
                    statusLabel.setText("Máy chủ đang bận. Vui lòng kiểm tra lại kết nối.");
                    statusLabel.setTextFill(javafx.scene.paint.Color.web("#ffbf00"));
                });
            }
        });

        networkInitThread.setDaemon(true);
        networkInitThread.start();
    }

    private void fadeOutAndSwitch() {
        // Tạo hiệu ứng mờ dần (Fade Out) trong 500ms
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(500), introRoot);
        fadeTransition.setFromValue(1.0);
        fadeTransition.setToValue(0.0);

        // Khi hiệu ứng điện ảnh kết thúc, kích hoạt hàm của nhóm để mở màn Login
        fadeTransition.setOnFinished(event -> SceneNavigator.showLogin());
        fadeTransition.play();
    }
}