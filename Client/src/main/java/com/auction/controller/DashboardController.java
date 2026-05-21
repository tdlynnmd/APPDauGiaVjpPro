package com.auction.controller;

import com.auction.util.ClientSession;
import com.auction.util.SceneNavigator;
import javafx.event.ActionEvent; // Import thêm ActionEvent để xử lý nút bấm đổi theme
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import com.auction.dto.SocketResponse;
import com.auction.network.ClientAuthApi;
import com.auction.enums.UserRole;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane; // Import thêm Pane để quản lý theme
import javafx.scene.shape.Circle;
import javafx.scene.paint.ImagePattern;
import javafx.scene.control.ScrollPane;
import java.util.Objects;

public class DashboardController {

    @FXML
    private Pane rootContainer; // Bạn nhớ đặt fx:id="rootContainer" cho Pane gốc trong Scene Builder nhé

    @FXML
    private ScrollPane mainScrollPane;

    @FXML
    private Circle avatarCircle;

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Button auctionListButton;

    @FXML
    private Button sellerManagementButton;

    @FXML
    private Button adminPanelButton;

    @FXML
    private Button logoutButton;

    @FXML
    public void initialize() {
        // --- ĐOẠN CODE TỰ ĐỘNG ÁP DỤNG THEME KHI VỪA MỞ MÀN HÌNH DASHBOARD ---
        if (rootContainer != null) {
            rootContainer.getStylesheets().clear();
            String currentPath = SceneNavigator.isAppDarkMode
                    ? "/com/auction/client/view/dark.css"
                    : "/com/auction/client/view/light.css";
            try {
                String css = Objects.requireNonNull(getClass().getResource(currentPath)).toExternalForm();
                rootContainer.getStylesheets().add(css);
            } catch (Exception e) {
                System.out.println("Không thể nạp theme hệ thống cho Dashboard: " + currentPath);
            }
        }

        // --- ÉP LỚP NỀN VIEWPORT CỦA SCROLLPANE THÀNH TRONG SUỐT ---
        if (mainScrollPane != null) {
            // Chỉ loại bỏ các viền/nền mặc định gồ ghề, nhường toàn quyền tô màu lại cho file CSS xử lý
            mainScrollPane.setStyle("-fx-viewport-background-color: transparent;");
        }

        if (!ClientSession.isLoggedIn()) {
            SceneNavigator.showLogin();
            return;
        }
        //--------------------------------------------------------------

        String imagePath = "/image/default-avatar.png";
        String username = ClientSession.getCurrentUser().getUsername();
        UserRole role = ClientSession.getCurrentUser().getRole();

        roleLabel.setText("Role: " + role);
        welcomeLabel.setText("Hello " + username);

        // --- ĐÃ FIX LỖI NULL POINTER EXCEPTION AN TOÀN ---
        if (avatarCircle != null) {
            try {
                Image avatarImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath)));
                ImagePattern pattern = new ImagePattern(avatarImage);
                avatarCircle.setFill(pattern);
            } catch (Exception e) {
                System.out.println("Không thể nạp ảnh avatar người dùng tại đường dẫn: " + imagePath);
                e.printStackTrace();
            }
        }

        hideAllRoleButtons();
        showButtonsByRole(role);
    }

    /**
     * Đổi theme sáng/tối cho màn hình Dashboard.
     * Đây chỉ là xử lý giao diện, không liên quan đến nghiệp vụ.
     */
    @FXML
    public void toggleTheme(ActionEvent event) {
        if (rootContainer != null) {
            rootContainer.getStylesheets().clear();

            // Đọc trạng thái từ SceneNavigator thay vì biến cục bộ để đồng bộ toàn app
            String path = SceneNavigator.isAppDarkMode
                    ? "/com/auction/client/view/light.css"
                    : "/com/auction/client/view/dark.css";

            try {
                String css = Objects.requireNonNull(getClass().getResource(path)).toExternalForm();
                rootContainer.getStylesheets().add(css);

                // Cập nhật lại trạng thái tổng của toàn App để các màn hình khác dùng chung
                SceneNavigator.isAppDarkMode = !SceneNavigator.isAppDarkMode;
            } catch (Exception e) {
                System.out.println("Không tìm thấy file CSS tại " + path);
                e.printStackTrace();
            }
        }
    }

    private void hideAllRoleButtons() {
        setButtonVisible(auctionListButton, false);
        setButtonVisible(sellerManagementButton, false);
        setButtonVisible(adminPanelButton, false);
    }

    private void showButtonsByRole(UserRole role) {
        if (role == UserRole.BIDDER) {
            setButtonVisible(auctionListButton, true);
        } else if (role == UserRole.SELLER) {
            setButtonVisible(sellerManagementButton, true);
        } else if (role == UserRole.ADMIN) {
            setButtonVisible(adminPanelButton, true);
        }
    }

    private void setButtonVisible(Button button, boolean visible) {
        if (button != null) {
            button.setVisible(visible);
            button.setManaged(visible);
        }
    }

    @FXML
    private void handleAuctionList() {
        /*
         * Bidder vào màn danh sách các phiên đấu giá đang hoạt động.
         */
        SceneNavigator.showAuctionList();
    }

    @FXML
    private void handleSellerManagement() {
        /*
         * Seller vào màn quản lý/tạo phiên đấu giá cho vật phẩm.
         */
        SceneNavigator.showSellerItemManagement();    }

    @FXML
    private void handleAdminPanel() {
        showInfo("Chức năng Admin Panel sẽ được nối với màn hình quản trị sau.");
    }

    @FXML
    private void handleLogout() {

        /**
         Xu lis khi nguoi dung bấm nút Logout trên Dasshboard
         Luồng xử lis:
         1. Lấy userId hiện tại từ ClientSession
         2. Gửi request LOGOUT sang Server
         3. Server xóa session khỏi ConnectionManage
         4. Client xóa token/user hiện tại
         5. Quay về màn hình Login
         */
        if (ClientSession.isLoggedIn()) {
            String userId = ClientSession.getCurrentUser().getId();

            // Gửi logout lên Server để Server xóa kết nối online.
            ClientAuthApi authApi = new ClientAuthApi();
            SocketResponse response = authApi.logout(userId);

            // Nếu Server báo lỗi, vẫn có thể cho Client thoát,
            // nhưng nên hiển thị để dễ debug trong quá trình làm project.
            if (response == null) {
                showInfo("Server không trả về phản hồi đăng xuất hợp lệ.");
            } else if (!response.isSuccess()) {
                showInfo("Server báo lỗi khi đăng xuất: " + response.getMessage());
            }
        }

        // Xóa session phía Client.
        // Sau bước này, Dashboard không còn biết user hiện tại là ai.
        ClientSession.clear();

        // Quay về màn hình đăng nhập.
        SceneNavigator.showLogin();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}