
/**
 Ket noi giao dien DashboardView.fxml , các dòng fx:id ở fxml dùng cho Controller này điều khiển thành phàn giao diện
 */
package com.auction.client.controller;

import com.auction.client.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

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

    /*
     * Đây là dữ liệu tạm để test Dashboard.
     * Sau này Người 3 làm Login xong thì username và role thật sẽ được truyền vào đây.
     *
     * Bạn có thể đổi DEMO_ROLE thành:
     * - "BIDDER"
     * - "SELLER"
     * - "ADMIN"
     * để test ẩn/hiện nút.
     */
    private static final String DEMO_USERNAME = "demo_user";
    private static final String DEMO_ROLE = "BIDDER";

    @FXML
    public void initialize() {
        String username = DEMO_USERNAME;
        String role = DEMO_ROLE.toUpperCase();

        welcomeLabel.setText("Xin chào, " + username);
        roleLabel.setText("Role: " + role);

        hideAllRoleButtons();
        showButtonsByRole(role);
    }

    private void hideAllRoleButtons() {
        setButtonVisible(auctionListButton, false);
        setButtonVisible(sellerManagementButton, false);
        setButtonVisible(adminPanelButton, false);
    }

    private void showButtonsByRole(String role) {
        if ("BIDDER".equals(role)) {
            setButtonVisible(auctionListButton, true);
        } else if ("SELLER".equals(role)) {
            setButtonVisible(sellerManagementButton, true);
        } else if ("ADMIN".equals(role)) {
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