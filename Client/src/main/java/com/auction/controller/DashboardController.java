package com.auction.controller;

import com.auction.util.ClientSession;
import javafx.application.Platform;
import com.auction.util.SceneNavigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import com.auction.dto.SocketResponse;
import com.auction.network.ClientAuthApi;
import com.auction.enums.UserRole;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.paint.ImagePattern;
import javafx.scene.control.ScrollPane;
import java.io.IOException;
import java.util.Objects;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Bộ điều khiển (Controller) hoặc lớp tiện ích DashboardController xử lý giao diện Client JavaFX.
 */
public class DashboardController {

    @FXML
    private Pane rootContainer;

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
    private Button sellerAuctionManagementButton;

    @FXML
    private Button adminPanelButton;

    @FXML
    private Button logoutButton;

    @FXML
    private Button myBidsButton;

    @FXML
    private Button walletButton;

    @FXML
    private Button exitButton;

    @FXML
    private Region auctionListSpacer;

    @FXML
    private Region myBidsSpacer;

    @FXML
    private Region sellerManagementSpacer;

    @FXML
    private Region sellerAuctionManagementSpacer;

    @FXML
    private Region adminPanelSpacer;

    @FXML
    private VBox walletMiniCard;

    @FXML
    private Label miniBalanceLabel;

    @FXML private Label headerAvailableBalance;
    @FXML private Label headerFrozenBalance;
    @FXML private Label headerTotalBalance;

    @FXML private Button settingsButton;

    @FXML
    public void initialize() {
        com.auction.util.HeaderBalanceHelper.setupHeaderBalance(headerAvailableBalance, headerFrozenBalance, headerTotalBalance);
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

        if (mainScrollPane != null) {
            mainScrollPane.setStyle("-fx-viewport-background-color: transparent;");
        }

        if (!ClientSession.isLoggedIn()) {
            SceneNavigator.showLogin();
            return;
        }

        String imagePath = "/image/default-avatar.png";
        String username = ClientSession.getCurrentUser().getUsername();
        UserRole role = ClientSession.getCurrentUser().getRole();

        roleLabel.setText("Role: " + role);
        welcomeLabel.setText("Hello " + username);

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

        updateMiniBalance();
        ClientSession.setBalanceListener((available, frozen) -> {
            Platform.runLater(this::updateMiniBalance);
        });

        hideAllRoleButtons();
        showButtonsByRole(role);
    }

    private void updateMiniBalance() {
        if (miniBalanceLabel == null || ClientSession.getCurrentUser() == null) {
            return;
        }

        double availableBalance = ClientSession.getCurrentUser().getAvailableBalance();
        miniBalanceLabel.setText(formatMoney(availableBalance));
    }

    private String formatMoney(double amount) {
        return NumberFormat
                .getCurrencyInstance(new Locale("vi", "VN"))
                .format(amount);
    }

    private void hideAllRoleButtons() {
        setElementVisible(auctionListButton, auctionListSpacer, false);
        setElementVisible(myBidsButton, myBidsSpacer, false);
        setElementVisible(sellerManagementButton, sellerManagementSpacer, false);
        setElementVisible(adminPanelButton, adminPanelSpacer, false);
        setElementVisible(sellerAuctionManagementButton, sellerAuctionManagementSpacer, false);
        setButtonVisible(walletButton, false);

        setCardVisible(walletMiniCard, false);
    }

    private void showButtonsByRole(UserRole role) {
        if (role == UserRole.BIDDER) {
            setElementVisible(auctionListButton, auctionListSpacer, true);
            setElementVisible(myBidsButton, myBidsSpacer, true);
            setButtonVisible(walletButton, true);
            setCardVisible(walletMiniCard, true);
        }
        else if (role == UserRole.SELLER) {
            setElementVisible(sellerManagementButton, sellerManagementSpacer, true);
            setButtonVisible(walletButton, true);
            setCardVisible(walletMiniCard, true);
            setElementVisible(sellerAuctionManagementButton, sellerAuctionManagementSpacer, true);
        }
        else if (role == UserRole.ADMIN) {
            setElementVisible(adminPanelButton, adminPanelSpacer, true);
            setCardVisible(walletMiniCard, false);
        }
    }

    private void setElementVisible(Button button, Region spacer, boolean visible) {
        if (button != null) {
            button.setVisible(visible);
            button.setManaged(visible);
        }
        if (spacer != null) {
            spacer.setVisible(visible);
            spacer.setManaged(visible);
        }
    }

    private void setButtonVisible(Button button, boolean visible) {
        if (button != null) {
            button.setVisible(visible);
            button.setManaged(visible);
        }
    }

    private void setCardVisible(VBox card, boolean visible) {
        if (card != null) {
            card.setVisible(visible);
            card.setManaged(visible);
        }
    }

    @FXML
    private void handleAuctionList() {
        
        SceneNavigator.showAuctionList();
    }

    @FXML
    private void handleSellerManagement() {
        
        SceneNavigator.showSellerItemManagement();    }

    @FXML
    private void handleAdminPanel() {
        SceneNavigator.showAdminDashboard();
    }

    @FXML
    private void handleSellerAuctionManagement() {
        SceneNavigator.showSellerAuctionManagement();
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

            ClientAuthApi authApi = new ClientAuthApi();
            SocketResponse response = authApi.logout(userId);

            if (response == null) {
                showInfo("Server không trả về phản hồi đăng xuất hợp lệ.");
            } else if (!response.isSuccess()) {
                showInfo("Server báo lỗi khi đăng xuất: " + response.getMessage());
            }
        }

        ClientSession.clear();
        com.auction.service.ClientSocketService.reset();
        com.auction.network.ClientNetworkManager.resetConnection();
        SceneNavigator.showLogin();
    }
    @FXML
    private void handleAccount() {
        SceneNavigator.showAccount();
    }

    @FXML
    private void handleSettings() {
        SceneNavigator.showSettings();
    }
    @FXML
    private void handleExit() {
        
        if (SceneNavigator.getStage() != null) {
            SceneNavigator.getStage().getOnCloseRequest().handle(
                    new javafx.stage.WindowEvent(
                            SceneNavigator.getStage(),
                            javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST
                    )
            );
        }
    }
    @FXML
    private void handleMyBids() {
        SceneNavigator.showMyBids();
    }

    @FXML
    private void handleWallet() {
        SceneNavigator.showWallet();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi hệ thống");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
