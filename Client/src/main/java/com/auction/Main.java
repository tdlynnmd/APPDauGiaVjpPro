package com.auction;

import com.auction.util.SceneNavigator;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * ClientApp là cửa khởi động của app người dùng
 * 1. Khởi động JavaFX
 * 2. Nhận cửa sổ chính từ JavaFX
 * 3. Đưa cửa sổ đó cho SceneNavigator quản lý
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 1. Lưu Stage vào Navigator như cũ của các bạn
        SceneNavigator.setStage(primaryStage);

        // --- 2. ĐOẠN CODE BẮT SỰ KIỆN F11 TOÀN CỤC CHO STAGE ---
        // Mỗi khi SceneNavigator đổi màn hình (thay Scene mới), đoạn code này sẽ tự động
        // gán quyền lắng nghe phím F11 cho màn hình mới đó mà không làm crash app.
        primaryStage.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    if (event.getCode() == javafx.scene.input.KeyCode.F11) {
                        // Đảo ngược trạng thái Fullscreen (Bật <-> Tắt)
                        primaryStage.setFullScreen(!primaryStage.isFullScreen());
                    }
                });
            }
        });

        // Mẹo nhỏ: Xóa bỏ dòng chữ "Press ESC to exit full screen" mặc định nếu bạn thấy vướng mắt
        primaryStage.setFullScreenExitHint("");
        // ------------------------------------------------------

        // 3. Thay vì trực tiếp mở Login lạnh lùng, ta nạp và hiển thị màn hình Intro lên trước
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/intro.fxml"));
            Parent root = loader.load();

            // Khởi tạo khung chứa Scene cơ sở đầu tiên cho ứng dụng với kích thước chuẩn 900x600 như thiết kế
            Scene scene = new Scene(root, 900, 600);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Online Auction - Khởi tạo hệ thống...");
            primaryStage.centerOnScreen();
            primaryStage.show();

        } catch (Exception e) {
            System.err.println("[Lỗi nghiêm trọng] Không thể tải màn hình Intro. Chuyển hướng khẩn cấp sang Login.");
            e.printStackTrace();
            SceneNavigator.showLogin(); // Phương án dự phòng an toàn tuyệt đối
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}