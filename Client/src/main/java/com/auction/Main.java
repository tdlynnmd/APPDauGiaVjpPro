package com.auction;


import com.auction.util.SceneNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * ClientApp là cửa khởi động của app người dùng
 1. Khởi động JavaFX
 2. Nhận cửa sổ chính từ JavaFX
 3. Đưa cửa sổ đó cho SceneNavigator quản lý
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        SceneNavigator.setStage(primaryStage);
        SceneNavigator.showLogin();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
