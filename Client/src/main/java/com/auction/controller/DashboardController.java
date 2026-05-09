package com.auction.controller;


import com.auction.util.ClientSession;
import com.auction.util.SceneNavigator;
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

    @FXML
    public void initialize() {
        if (!ClientSession.isLoggedIn()) {
            SceneNavigator.showLogin();
            return;
        }

        String username = ClientSession.getCurrentUser().getUsername();
        String role = ClientSession.getCurrentUser().getRole().toString();

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
        ClientSession.clear();
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
