
package com.auction.util;
/**
 SceneNavigator là class chuyên dùng để chuyển màn hình.
 Giả sử không có SceneNavigator, mỗi controller sẽ phải tự viết lại đoạn code load FXML
 */
import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneNavigator {
    private static Stage mainStage;

    private static final String LOGIN_VIEW = "/com/auction/client/view/login.fxml";
    private static final String DASHBOARD_VIEW = "/com/auction/client/view/DashboardView.fxml";

    private SceneNavigator() {
    }

    public static void setStage(Stage stage) {
        mainStage = stage;
    }

    public static void showLogin() {
        loadScene(LOGIN_VIEW, "Login");
    }

    public static void showDashboard() {
        loadScene(DASHBOARD_VIEW, "Dashboard");
    }

    private static void loadScene(String fxmlPath, String title) {
        if (mainStage == null) {
            throw new IllegalStateException("Main stage has not been set.");
        }

        try {
            URL resource = SceneNavigator.class.getResource(fxmlPath);

            if (resource == null) {
                throw new IllegalArgumentException("Cannot find FXML file: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            Scene scene = new Scene(root, 900, 600);
            mainStage.setTitle("Online Auction - " + title);
            mainStage.setScene(scene);
            mainStage.centerOnScreen();
            mainStage.show();

        } catch (IOException e) {
            throw new RuntimeException("Failed to load scene: " + fxmlPath, e);
        }
    }
}
/**
    Hien tai trong ClientApp.java dang goi : SceneNavigator.showLogin();
    Trong SceneNavigator.java, hàm showLogin() sẽ tìm file:
    /com/auction/client/view/LoginView.fxml
    Tức là nó cần file thật ở vị trí:
    src/main/resources/com/auction/client/view/LoginView.fxml (bay gio sẽ sang file LoginView.fxml
 */