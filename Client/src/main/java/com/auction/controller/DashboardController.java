package com.auction.controller;


import com.auction.util.ClientSession;
import com.auction.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import com.auction.dto.LogoutResponse;
import com.auction.network.ClientAuthApi;
import com.auction.enums.UserRole;

public class DashboardController {

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
        if (!ClientSession.isLoggedIn()) {
            SceneNavigator.showLogin();
            return;
        }

        String username = ClientSession.getCurrentUser().getUsername();
        UserRole role = ClientSession.getCurrentUser().getRole();

        roleLabel.setText("Role: " + role);
        welcomeLabel.setText("Hello " + username);

        hideAllRoleButtons();
        showButtonsByRole(role);
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
        button.setVisible(visible);
        button.setManaged(visible);
    }

    @FXML
    private void handleAuctionList() {
        showInfo("Chức năng Auction List sẽ được nối với màn hình danh sách đấu giá sau.");
    }

    @FXML
    private void handleSellerManagement() {
        showInfo("Chức năng Seller Management sẽ được nối với màn hình quản lý sản phẩm sau.");
    }

    @FXML
    private void handleAdminPanel() {
        showInfo("Chức năng Admin Panel sẽ được nối với màn hình quản trị sau.");
    }

    @FXML
    private void handleLogout() {

        /**
         Xu lí khi nguoi dung bấm nút Logout trên Dasshboard
         Luồng xử lí:
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
            LogoutResponse response = authApi.logout(userId);

            // Nếu Server báo lỗi, vẫn có thể cho Client thoát,
            // nhưng nên hiển thị để dễ debug trong quá trình làm project.
            if (!response.isSuccess()) {
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
