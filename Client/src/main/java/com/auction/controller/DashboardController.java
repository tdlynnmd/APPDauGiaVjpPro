package com.auction.controller;

import com.auction.util.ClientSession;
import com.auction.util.SceneNavigator;
import javafx.event.ActionEvent; // Import thêm ActionEvent để xử lý nút bấm đổi theme
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import com.auction.dto.SocketResponse;
import com.auction.network.ClientAuthApi;
import com.auction.enums.UserRole;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane; // Import thêm Pane để quản lý theme
import javafx.scene.layout.Region; // Import thêm Region để quản lý lò xo động
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.paint.ImagePattern;
import javafx.scene.control.ScrollPane;
import java.io.IOException;
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
    private Button myBidsButton;

    @FXML
    private Button walletButton;

    // --- NÚT THOÁT ỨNG DỤNG ĐỂ CHẠY HIỆU ỨNG OUTRO TIỄN KHÁCH ---
    @FXML
    private Button exitButton;

    // --- CÁC ĐỐI TƯỢNG LÒ XO ĐỆM ĐƯỢC QUẢN LÝ ẨN/HIỆN ĐỒNG BỘ THEO ROLE ---
    @FXML
    private Region auctionListSpacer;

    @FXML
    private Region myBidsSpacer;

    @FXML
    private Region sellerManagementSpacer;

    @FXML
    private Region adminPanelSpacer;

    // --- COMPONENT THẺ VÍ MINI MỚI ĐỂ LẤP ĐẦY KHOẢNG TRỐNG THÔNG MINH ---
    @FXML
    private VBox walletMiniCard;

    @FXML
    private Label miniBalanceLabel;

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

        // --- ĐÃ FIX LỖI NULL POINTER EXCEPTION AN TOÀN ---
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

        // --- CẬP NHẬT HIỂN THỊ SỐ DƯ MẪU CHO CARD VÍ (Sau này kết nối API nạp dữ liệu thật) ---
        if (miniBalanceLabel != null) {
            miniBalanceLabel.setText("$1,500.00");
        }

        hideAllRoleButtons();
        showButtonsByRole(role);
    }

    private void hideAllRoleButtons() {
        // Ẩn đồng bộ cả Nút lẫn khối lò xo đi kèm để tránh lỗi khoảng trống không đều
        setElementVisible(auctionListButton, auctionListSpacer, false);
        setElementVisible(myBidsButton, myBidsSpacer, false);
        setElementVisible(sellerManagementButton, sellerManagementSpacer, false);
        setElementVisible(adminPanelButton, adminPanelSpacer, false);

        // Nút Wallet không cần spacer riêng vì nó nằm cố định cạnh Settings
        setButtonVisible(walletButton, false);

        // Mặc định ẩn thẻ ví mini
        setCardVisible(walletMiniCard, false);
    }

    private void showButtonsByRole(UserRole role) {
        if (role == UserRole.BIDDER) {
            setElementVisible(auctionListButton, auctionListSpacer, true);
            setElementVisible(myBidsButton, myBidsSpacer, true);
            setButtonVisible(walletButton, true);
            setCardVisible(walletMiniCard, true); // Hiện thẻ ví cho khách đấu giá nhìn số dư công khai
        } else if (role == UserRole.SELLER) {
            setElementVisible(sellerManagementButton, sellerManagementSpacer, true);
            setButtonVisible(walletButton, true);
            setCardVisible(walletMiniCard, true); // Hiện thẻ ví cho người bán theo dõi doanh thu phiên
        } else if (role == UserRole.ADMIN) {
            setElementVisible(adminPanelButton, adminPanelSpacer, true);
            setCardVisible(walletMiniCard, false); // Admin không cần thẻ ví cá nhân, ẩn đi cho gọn gàng
        }
    }

    // --- HÀM XỬ LÝ ẨN/HIỆN CÁC THÀNH PHẦN HOÀN TOÀN KHÔNG ĐỂ LẠI KHÔNG GIAN THỪA ---
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
        SceneNavigator.showAdminDashboard();
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
        com.auction.network.ClientNetworkManager.resetConnection();
        com.auction.service.ClientSocketService.reset();
        // Quay về màn hình đăng nhập.
        SceneNavigator.showLogin();
    }

    // --- HÀM XỬ LÝ SỰ KIỆN KHI CLICK VÀO NÚT EXIT APP MỚI ---
    @FXML
    private void handleExit() {
        /*
         * Khi người dùng nhấn nút Exit trên thanh Dashboard:
         * Giả lập một sự kiện WINDOW_CLOSE_REQUEST bắn thẳng vào Stage.
         * Logic tại Main.java sẽ bắt được, chặn tắt đột ngột, quay về màn hình Intro
         * và chạy hoạt cảnh tiễn khách Outro cực kỳ mượt mà.
         */
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
        System.out.println("Mở màn hình danh sách các phiên tôi đang đấu giá...");
        // SceneNavigator.showMyBids(); // Ví dụ gọi chuyển màn hình
    }

    @FXML
    private void handleWallet() {
        System.out.println("Mở màn hình Ví & Lịch sử giao dịch...");
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