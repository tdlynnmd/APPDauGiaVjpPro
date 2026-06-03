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
 * SceneNavigator la class chuyen dung de chuyen man hinh.
 *
 * Nhiem vu:
 * - Giu Stage chinh cua ung dung.
 * - Load dung file FXML theo tung man hinh.
 * - Tao Scene moi hoac thay root cua Scene hien tai.
 * - Giu trang thai cua so khi chuyen man, vi du maximized/fullscreen.
 * - Cho cac Controller goi cac ham chuyen man nhu showLogin(), showDashboard(), showAuctionList().
 */
public class SceneNavigator {
    private static Stage mainStage;

    /*
     * Bien luu trang thai theme toan he thong.
     * false = Light mode.
     * true = Dark mode.
     */
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
        // Khong cho tao object SceneNavigator vi toan bo ham trong class nay la static.
    }

    public static void setStage(Stage stage) {
        mainStage = stage;
    }

    // --- HÀM GETTER MỚI BỔ SUNG ĐỂ COI CỬA SỔ CHÍNH TỪ CÁC CONTROLLER ---
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
    // Vai trò: mở màn quản lý phiên đấu giá của seller.
    public static void showSellerAuctionManagement() {
        loadScene(SELLER_AUCTION_MANAGEMENT_VIEW, "Seller Auction Management");
    }

    public static void showLiveBidding(String auctionId) {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new IllegalArgumentException("auctionId must not be empty.");
        }


        FXMLLoader loader = loadSceneAndReturnLoader(LIVE_BIDDING_VIEW, "Live Bidding");

        /*
         * live-bidding.fxml tu tao LiveBiddingController.
         * Sau khi load xong, lay controller ra va truyen auctionId vao.
         * Tu day LiveBiddingController moi biet can enterLiveRoom cho phien dau gia nao.
         */
        LiveBiddingController controller = loader.getController();
        controller.setAuctionId(auctionId);
    }
    public static void showAuctionDetail(String auctionId) {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new IllegalArgumentException("auctionId must not be empty.");
        }

        FXMLLoader loader = loadSceneAndReturnLoader(AUCTION_DETAIL_VIEW, "Auction Detail");

        /*
         * auction-detail.fxml tu tao AuctionDetailController.
         * Sau khi load xong, lay controller ra va truyen auctionId vao.
         */
        AuctionDetailController controller = loader.getController();
        controller.setAuctionId(auctionId);
    }
    // Vai trò: mở màn cài đặt tài khoản và giao diện.
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

        // Gỡ bỏ balance listener cũ của màn hình trước khi chuyển cảnh
        ClientSession.setBalanceListener(null);

        try {
            URL resource = SceneNavigator.class.getResource(fxmlPath);

            if (resource == null) {
                throw new IllegalArgumentException("Cannot find FXML file: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            /*
             * Neu chua co Scene, tao Scene moi.
             * Neu da co Scene, chi thay root de giu trang thai cua so hien tai.
             */
            if (mainStage.getScene() == null) {
                Scene scene = new Scene(root, 900, 600);
                mainStage.setScene(scene);
            } else {
                mainStage.getScene().setRoot(root);
            }

            mainStage.setTitle("Online Auction - " + title);

            if (!mainStage.isMaximized()) {
                mainStage.centerOnScreen();
            }

            mainStage.show();

            return loader;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load scene: " + fxmlPath, e);
        }
    }
}