
package com.auction.util;
/**
 SceneNavigator là class chuyên dùng để chuyển màn hình.
 Giả sử không có SceneNavigator, mỗi controller sẽ phải tự viết lại đoạn code load FXML
 Nhiem vu: Cho controller gọi showLogin(), showRegister(), showDashboard()
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
    private static final String REGISTER_VIEW = "/com/auction/client/view/register.fxml";

    private SceneNavigator() {      // để private ể ko cho tạo object SceneNavigator
                                    // vì toàn bộ hàm trong class này là static nên việc tạo object là thừa
    }
    public static void setStage(Stage stage) {
        mainStage = stage;
    }   // nhận Stage tu Main
    public static void showLogin() {
        loadScene(LOGIN_VIEW, "Login");
    }       // chuyển về maàn hinh dang nhap
    public static void showRegister(){                      // chuyển về màn hình đăng ki
        loadScene(REGISTER_VIEW, "Register");
    }
    public static void showDashboard() {
        loadScene(DASHBOARD_VIEW, "Dashboard");
    }



    private static void loadScene(String fxmlPath, String title) {  // Hàm dùng chung để load FXML
        if (mainStage == null) {
            throw new IllegalStateException("Main stage has not been set.");
        }
        try {
            URL resource = SceneNavigator.class.getResource(fxmlPath);  // Tìm file FXML trong thư mục resources

            if (resource == null) {
                throw new IllegalArgumentException("Cannot find FXML file: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(resource);       // Đọc file FXML và tự tạo Controller tương ứng
            Parent root = loader.load();

            Scene scene = new Scene(root, 900, 600);    // Taọ scene mới từ giao diện vừa load
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
    Hien tai trong Main.java ở Client dang goi : SceneNavigator.showLogin();
    Trong SceneNavigator.java, hàm showLogin() sẽ tìm file:
    /com/auction/client/view/LoginView.fxml
    Tức là nó cần file thật ở vị trí:
    src/main/resources/com/auction/client/view/LoginView.fxml (bay gio sẽ sang file LoginView.fxml
 */