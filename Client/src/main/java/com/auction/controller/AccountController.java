package com.auction.controller;

import com.auction.dto.AdminDTO;
import com.auction.dto.BidderDTO;
import com.auction.dto.SellerDTO;
import com.auction.dto.SocketResponse;
import com.auction.dto.UserDTO;
import com.auction.enums.UserRole;
import com.auction.network.ClientUserApi;
import com.auction.util.ClientSession;
import com.auction.util.SceneNavigator;
import com.auction.utils.GsonProvider;
import com.google.gson.Gson;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * AccountController là controller cho màn xem thông tin tài khoản hiện tại.
 *
 * Liên hệ logic có sẵn:
 * - Client gọi ClientUserApi.getUserProfile().
 * - ClientUserApi gửi action GET_USER_PROFILE qua socket.
 * - Server RequestDispatcher lấy userId từ ClientSession phía server.
 * - UserController/UserService trả về UserDTO theo đúng role hiện tại.
 *
 * Controller này KHÔNG tự xử lý nghiệp vụ account.
 * Server vẫn là nơi xác thực session, phân quyền và lấy dữ liệu thật.
 */
public class AccountController {
    private final ClientUserApi userApi = new ClientUserApi();
    private final Gson gson = GsonProvider.getGson();

    private final NumberFormat moneyFormat =
            NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    // =========================================================================
    // FXML CONTRACT
    // Bạn làm FXML cần gắn các fx:id này nếu muốn hiển thị đủ dữ liệu.
    // Field nào chưa có trong FXML thì controller bỏ qua, không làm crash màn hình.
    // =========================================================================

    @FXML private Parent rootContainer;

    @FXML private Label messageLabel;

    @FXML private Label userIdLabel;
    @FXML private Label usernameLabel;
    @FXML private Label emailLabel;
    @FXML private Label roleLabel;
    @FXML private Label statusLabel;

    // Khu vực ví dùng cho BIDDER/SELLER. ADMIN sẽ bị ẩn.
    @FXML private Node walletSection;
    @FXML private Label availableBalanceLabel;
    @FXML private Label frozenBalanceLabel;
    @FXML private Label totalBalanceLabel;

    // Khu vực riêng của BIDDER.
    @FXML private Node bidderSection;
    @FXML private Label joinedAuctionCountLabel;

    // Khu vực riêng của SELLER.
    @FXML private Node sellerSection;
    @FXML private Label ratingLabel;

    // Khu vực riêng của ADMIN.
    @FXML private Node adminSection;

    @FXML private Button refreshButton;
    @FXML private Button backButton;

    /**
     * Hàm khởi tạo màn Account.
     *
     * Nhiệm vụ:
     * - Áp dụng theme hiện tại.
     * - Kiểm tra client đã đăng nhập chưa.
     * - Gọi server để tải profile mới nhất.
     */
    @FXML
    public void initialize() {
        applyTheme();

        if (!ClientSession.isLoggedIn()) {
            showMessage("Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.");
            SceneNavigator.showLogin();
            return;
        }

        hideRoleSpecificSections();
        loadProfile();
    }

    /**
     * Xử lý nút Refresh trên màn Account.
     *
     * Nhiệm vụ:
     * - Gọi lại GET_USER_PROFILE để lấy dữ liệu mới nhất từ server.
     */
    @FXML
    private void handleRefresh() {
        loadProfile();
    }

    /**
     * Xử lý nút Back trên màn Account.
     *
     * Nhiệm vụ:
     * - Quay lại Dashboard hiện tại.
     */
    @FXML
    private void handleBack() {
        SceneNavigator.showDashboard();
    }

    /**
     * Gọi server để lấy profile hiện tại.
     *
     * Nhiệm vụ:
     * - Chạy request trong background thread để không treo JavaFX UI.
     * - Nhận SocketResponse từ ClientUserApi.
     * - Chuyển kết quả sang handleProfileResponse().
     */
    private void loadProfile() {
        setBusy(true);
        showMessage("Đang tải thông tin tài khoản...");

        Task<SocketResponse> task = new Task<>() {
            @Override
            protected SocketResponse call() {
                return userApi.getUserProfile();
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);
            handleProfileResponse(task.getValue());
        });

        task.setOnFailed(event -> {
            setBusy(false);
            Throwable error = task.getException();
            showMessage("Không thể tải profile: " + (error == null ? "unknown" : error.getMessage()));
        });

        Thread worker = new Thread(task, "account-profile-request-thread");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Xử lý response từ server.
     *
     * Nhiệm vụ:
     * - Kiểm tra request thành công/thất bại.
     * - Parse body thành DTO đúng theo role.
     * - Cập nhật ClientSession để các màn khác dùng dữ liệu mới.
     * - Render dữ liệu lên UI.
     */
    private void handleProfileResponse(SocketResponse response) {
        if (response == null) {
            showMessage("Server không trả về phản hồi profile.");
            return;
        }

        if (!response.isSuccess()) {
            showMessage(response.getMessage() == null
                    ? "Tải profile thất bại."
                    : response.getMessage());
            return;
        }

        UserDTO profile = parseProfileByRole(response);
        if (profile == null) {
            showMessage("Dữ liệu profile server trả về không hợp lệ.");
            return;
        }

        ClientSession.saveLoginSession(ClientSession.getToken(), profile);
        renderProfile(profile);
        showMessage("Đã tải thông tin tài khoản.");
    }

    /**
     * Parse profile theo đúng vai trò.
     *
     * Nhiệm vụ:
     * - Đầu tiên dùng ClientUserApi.parseUser() để lấy role cơ bản.
     * - Sau đó parse lại body thành DTO cụ thể:
     *   BIDDER -> BidderDTO
     *   SELLER -> SellerDTO
     *   ADMIN  -> AdminDTO
     *
     * Lý do:
     * - UserDTO là lớp cha.
     * - BidderDTO/SellerDTO có field riêng như joinedAuctionIds/rating.
     */
    private UserDTO parseProfileByRole(SocketResponse response) {
        UserDTO baseProfile = userApi.parseUser(response);
        if (baseProfile == null || baseProfile.getRole() == null || response.getBody() == null) {
            return null;
        }

        UserRole role = baseProfile.getRole();

        if (role == UserRole.BIDDER) {
            return gson.fromJson(response.getBody(), BidderDTO.class);
        }

        if (role == UserRole.SELLER) {
            return gson.fromJson(response.getBody(), SellerDTO.class);
        }

        if (role == UserRole.ADMIN) {
            return gson.fromJson(response.getBody(), AdminDTO.class);
        }

        return baseProfile;
    }

    /**
     * Render toàn bộ dữ liệu profile lên màn hình.
     *
     * Nhiệm vụ:
     * - Hiển thị thông tin chung cho cả 3 vai trò.
     * - Gọi renderRoleSpecificData() để hiển thị phần riêng từng role.
     */
    private void renderProfile(UserDTO profile) {
        setLabelText(userIdLabel, profile.getId());
        setLabelText(usernameLabel, profile.getUsername());
        setLabelText(emailLabel, profile.getEmail());
        setLabelText(roleLabel, profile.getRole() == null ? "" : profile.getRole().name());
        setLabelText(statusLabel, profile.getStatus() == null ? "" : profile.getStatus().name());

        renderRoleSpecificData(profile);
    }

    /**
     * Hiển thị dữ liệu riêng theo vai trò.
     *
     * Vai trò:
     * - BIDDER: xem số dư, tiền bị đóng băng, số phiên đã tham gia/theo dõi.
     * - SELLER: xem số dư, tiền bị đóng băng, rating uy tín.
     * - ADMIN: chỉ xem thông tin tài khoản, không hiển thị ví.
     */
    private void renderRoleSpecificData(UserDTO profile) {
        hideRoleSpecificSections();

        if (profile.getRole() == UserRole.BIDDER) {
            renderWallet(profile);
            renderBidderData(profile);
            setSectionVisible(walletSection, true);
            setSectionVisible(bidderSection, true);
            return;
        }

        if (profile.getRole() == UserRole.SELLER) {
            renderWallet(profile);
            renderSellerData(profile);
            setSectionVisible(walletSection, true);
            setSectionVisible(sellerSection, true);
            return;
        }

        if (profile.getRole() == UserRole.ADMIN) {
            setSectionVisible(adminSection, true);
        }
    }

    /**
     * Render thông tin ví.
     *
     * Nhiệm vụ:
     * - Hiển thị số dư khả dụng.
     * - Hiển thị số dư đang bị đóng băng.
     * - Hiển thị tổng số dư.
     */
    private void renderWallet(UserDTO profile) {
        double available = profile.getAvailableBalance();
        double frozen = profile.getFrozenBalance();

        setLabelText(availableBalanceLabel, formatMoney(available));
        setLabelText(frozenBalanceLabel, formatMoney(frozen));
        setLabelText(totalBalanceLabel, formatMoney(available + frozen));
    }

    /**
     * Render dữ liệu riêng của Bidder.
     *
     * Nhiệm vụ:
     * - Nếu server trả BidderDTO, hiển thị số phiên bidder đã tham gia/theo dõi.
     */
    private void renderBidderData(UserDTO profile) {
        if (!(profile instanceof BidderDTO bidder)) {
            setLabelText(joinedAuctionCountLabel, "0");
            return;
        }

        List<String> joinedAuctionIds = bidder.getJoinedAuctionIds();
        int joinedCount = joinedAuctionIds == null ? 0 : joinedAuctionIds.size();

        setLabelText(joinedAuctionCountLabel, String.valueOf(joinedCount));
    }

    /**
     * Render dữ liệu riêng của Seller.
     *
     * Nhiệm vụ:
     * - Nếu server trả SellerDTO, hiển thị rating của seller.
     */
    private void renderSellerData(UserDTO profile) {
        if (!(profile instanceof SellerDTO seller)) {
            setLabelText(ratingLabel, "");
            return;
        }

        setLabelText(ratingLabel, String.format("%.1f", seller.getRating()));
    }

    /**
     * Ẩn toàn bộ section riêng theo role.
     *
     * Nhiệm vụ:
     * - Đảm bảo khi đổi user/refresh, UI không bị sót dữ liệu role cũ.
     */
    private void hideRoleSpecificSections() {
        setSectionVisible(walletSection, false);
        setSectionVisible(bidderSection, false);
        setSectionVisible(sellerSection, false);
        setSectionVisible(adminSection, false);
    }

    /**
     * Áp dụng theme hiện tại của app.
     *
     * Nhiệm vụ:
     * - Dùng chung light.css/dark.css như các controller hiện có.
     */
    private void applyTheme() {
        if (rootContainer == null) {
            return;
        }

        rootContainer.getStylesheets().clear();

        String cssPath = SceneNavigator.isAppDarkMode
                ? "/com/auction/client/view/dark.css"
                : "/com/auction/client/view/light.css";

        try {
            String css = Objects.requireNonNull(getClass().getResource(cssPath)).toExternalForm();
            rootContainer.getStylesheets().add(css);
        } catch (Exception e) {
            System.out.println("Không thể nạp theme cho Account: " + cssPath);
        }
    }

    /**
     * Khóa/mở các nút thao tác khi đang gọi server.
     *
     * Nhiệm vụ:
     * - Tránh người dùng bấm refresh nhiều lần liên tục.
     */
    private void setBusy(boolean busy) {
        setDisabled(refreshButton, busy);
        setDisabled(backButton, busy);
    }

    /**
     * Bật/tắt một section UI.
     *
     * Nhiệm vụ:
     * - setVisible và setManaged cùng lúc để section ẩn không chiếm layout.
     */
    private void setSectionVisible(Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    /**
     * Disable node an toàn.
     *
     * Nhiệm vụ:
     * - Không crash nếu FXML chưa gắn fx:id tương ứng.
     */
    private void setDisabled(Node node, boolean disabled) {
        if (node != null) {
            node.setDisable(disabled);
        }
    }

    /**
     * Gán text cho Label an toàn.
     *
     * Nhiệm vụ:
     * - Không crash nếu FXML chưa có label.
     * - Không hiển thị null lên UI.
     */
    private void setLabelText(Label label, String value) {
        if (label != null) {
            label.setText(value == null ? "" : value);
        }
    }

    /**
     * Hiển thị message trạng thái.
     *
     * Nhiệm vụ:
     * - Cho người dùng biết đang tải, thành công hoặc lỗi.
     */
    private void showMessage(String message) {
        setLabelText(messageLabel, message);
    }

    /**
     * Format tiền theo locale Việt Nam.
     *
     * Nhiệm vụ:
     * - Đồng bộ cách hiển thị số dư account với các màn ví/dashboard.
     */
    private String formatMoney(double amount) {
        return moneyFormat.format(amount);
    }
}