package com.auction.util;

import com.auction.controller.AuctionDetailController;
import com.auction.controller.LiveBiddingController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Bộ điều hướng và chuyển đổi các màn hình (Scene) của ứng dụng JavaFX.
 */
public class SceneNavigator {
    private static Stage mainStage;

    public static boolean isAppDarkMode = false;

    private static final String LOGIN_VIEW = "/com/auction/client/view/login.fxml";
    private static final String REGISTER_VIEW = "/com/auction/client/view/register.fxml";
    private static final String DASHBOARD_VIEW = "/com/auction/client/view/DashboardView.fxml";

    private static final String AUCTION_LIST_VIEW = "/com/auction/client/view/auction-list.fxml";
    private static final String AUCTION_DETAIL_VIEW = "/com/auction/client/view/auction-detail.fxml";
    private static final String SELLER_ITEM_MANAGEMENT_VIEW = "/com/auction/client/view/seller-item-management.fxml";
    private static final String LIVE_BIDDING_VIEW = "/com/auction/client/view/live-bidding.fxml";
    private static final String ADMIN_DASHBOARD_VIEW = "/com/auction/client/view/admin-dashboard.fxml";
    private static final String WALLET_VIEW = "/com/auction/client/view/wallet.fxml";
    private static final String MY_BIDS_VIEW = "/com/auction/client/view/my-bids.fxml";
    private static final String ACCOUNT_VIEW = "/com/auction/client/view/account.fxml";
    private static final String SELLER_AUCTION_MANAGEMENT_VIEW = "/com/auction/client/view/seller-auction-management.fxml";
    private static final String SETTINGS_VIEW = "/com/auction/client/view/settings.fxml";

    private SceneNavigator() {
    }

    public static void setStage(Stage stage) {
        mainStage = stage;
    }

    public static Stage getStage() {
        return mainStage;
    }

    public static void showLogin() {
        loadScene(LOGIN_VIEW, "Login");
    }

    public static void showRegister() {
        loadScene(REGISTER_VIEW, "Register");
    }

    public static void showDashboard() {
        loadScene(DASHBOARD_VIEW, "Dashboard");
    }

    public static void showAuctionList() {
        loadScene(AUCTION_LIST_VIEW, "Auction List");
    }

    public static void showSellerItemManagement() {
        loadScene(SELLER_ITEM_MANAGEMENT_VIEW, "Seller Item Management");
    }

    public static void showAdminDashboard() {
        loadScene(ADMIN_DASHBOARD_VIEW, "Admin Dashboard");
    }
    public static void showMyBids() {
        loadScene(MY_BIDS_VIEW, "My Bids");
    }
    public static void showWallet() {
        loadScene(WALLET_VIEW, "Wallet");
    }
    public static void showAccount() {
        loadScene(ACCOUNT_VIEW, "Account");
    }
    public static void showSellerAuctionManagement() {
        loadScene(SELLER_AUCTION_MANAGEMENT_VIEW, "Seller Auction Management");
    }

    public static void showLiveBidding(String auctionId) {
        showLiveBidding(auctionId, null, false);
    }

    public static void showLiveBidding(String auctionId, com.auction.dto.AuctionDetailDTO preloadedDetail, boolean alreadyJoined) {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new IllegalArgumentException("auctionId must not be empty.");
        }

        FXMLLoader loader = loadSceneAndReturnLoader(LIVE_BIDDING_VIEW, "Live Bidding");

        LiveBiddingController controller = loader.getController();
        controller.setAuctionId(auctionId, preloadedDetail, alreadyJoined);
    }
    public static void showAuctionDetail(String auctionId) {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new IllegalArgumentException("auctionId must not be empty.");
        }

        FXMLLoader loader = loadSceneAndReturnLoader(AUCTION_DETAIL_VIEW, "Auction Detail");

        AuctionDetailController controller = loader.getController();
        controller.setAuctionId(auctionId);
    }
    public static void showSettings() {
        loadScene(SETTINGS_VIEW, "Settings");
    }

    private static void loadScene(String fxmlPath, String title) {
        loadSceneAndReturnLoader(fxmlPath, title);
    }

    private static FXMLLoader loadSceneAndReturnLoader(String fxmlPath, String title) {
        if (mainStage == null) {
            throw new IllegalStateException("Main stage has not been set.");
        }

        ClientSession.setBalanceListener(null);

        try {
            URL resource = SceneNavigator.class.getResource(fxmlPath);

            if (resource == null) {
                throw new IllegalArgumentException("Cannot find FXML file: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            boolean isFirstTime = (mainStage.getScene() == null);
            boolean wasMaximized = mainStage.isMaximized();
            boolean wasFullScreen = mainStage.isFullScreen();

            double width;
            double height;

            if (!isFirstTime) {
                width = mainStage.getScene().getWidth();
                height = mainStage.getScene().getHeight();
            } else {
                double prefW = root.prefWidth(-1);
                double prefH = root.prefHeight(-1);
                width = prefW > 0 ? prefW : 900;
                height = prefH > 0 ? prefH : 600;

                javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
                if (width > screenBounds.getWidth()) {
                    width = screenBounds.getWidth() * 0.95;
                }
                if (height > screenBounds.getHeight()) {
                    height = screenBounds.getHeight() * 0.90;
                }
            }

            Scene scene = new Scene(root, width, height);
            mainStage.setScene(scene);

            mainStage.setTitle("Online Auction - " + title);

            if (wasMaximized) {
                mainStage.setMaximized(true);
            }
            if (wasFullScreen) {
                mainStage.setFullScreen(true);
            }

            if (isFirstTime) {
                mainStage.centerOnScreen();
            }

            mainStage.show();

            return loader;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load scene: " + fxmlPath, e);
        }
    }
}