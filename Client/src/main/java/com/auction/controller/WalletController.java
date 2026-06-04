package com.auction.controller;

import com.auction.dto.SocketResponse;
import com.auction.dto.UserDTO;
import com.auction.enums.UserRole;
import com.auction.network.ClientUserApi;
import com.auction.util.ClientSession;
import com.auction.util.SceneNavigator;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Bộ điều khiển (Controller) hoặc lớp tiện ích WalletController xử lý giao diện Client JavaFX.
 */
public class WalletController {
    private final ClientUserApi walletApi = new ClientUserApi();

    private final NumberFormat moneyFormat =
            NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    private boolean isDepositAction = true;
    private Double pendingAmount = 0.0;
    private boolean isProcessingTransaction = false;

    @FXML private Parent rootContainer;

    @FXML private Label usernameLabel;
    @FXML private Label roleLabel;
    @FXML private Label availableBalanceLabel;
    @FXML private Label frozenBalanceLabel;
    @FXML private Label totalBalanceLabel;
    @FXML private Label messageLabel;

    @FXML private Label headerAvailableBalance;
    @FXML private Label headerFrozenBalance;
    @FXML private Label headerTotalBalance;

    @FXML private TextField amountField;

    @FXML private HBox transactionBox;

    @FXML private VBox formLeftPane;
    @FXML private VBox qrRightPane;
    @FXML private ToggleGroup paymentMethodGroup;
    @FXML private ToggleButton momoToggle;
    @FXML private ToggleButton zalopayToggle;
    @FXML private ToggleButton bankingToggle;
    @FXML private ToggleButton shopeepayToggle;

    @FXML private ImageView momoLogo;
    @FXML private ImageView zalopayLogo;
    @FXML private ImageView bankingLogo;
    @FXML private ImageView shopeepayLogo;

    @FXML private Label qrTitleLabel;
    @FXML private Label qrHintLabel;
    @FXML private ImageView qrImageView;
    @FXML private Button confirmQRButton;
    @FXML private Button cancelQRButton;

    @FXML private VBox sellerMaintenanceBox;

    @FXML private Button refreshButton;
    @FXML private Button depositButton;
    @FXML private Button withdrawButton;
    @FXML private Button backButton;

    @FXML
    public void initialize() {
        com.auction.util.HeaderBalanceHelper.setupHeaderBalance(headerAvailableBalance, headerFrozenBalance, headerTotalBalance);
        applyTheme();

        if (!isWalletViewerSession()) {
            showError("Chi tai khoan Bidder hoac Seller moi duoc xem vi.");
            SceneNavigator.showDashboard();
            return;
        }

        if (qrRightPane != null) {
            qrRightPane.setVisible(false);
            qrRightPane.setManaged(false);
        }

        bindCurrentUserToHeader();
        setBusy(false);
        applyWalletModeByRole();
        showMessage("Dang tai thong tin vi...");

        ClientSession.setBalanceListener((available, frozen) -> {
            Platform.runLater(() -> {
                UserDTO currentUser = ClientSession.getCurrentUser();
                if (currentUser != null) {
                    renderBalances(currentUser);
                }
            });
        });

        loadWalletProfile();
    }

    /**
     * Ap dung theme hien tai cua app.
     * Neu FXML chua gan rootContainer thi bo qua de controller khong crash.
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
            System.out.println("Khong the nap theme cho Wallet: " + cssPath);
        }
    }

    private boolean isWalletViewerSession() {
        if (!ClientSession.isLoggedIn() || ClientSession.getCurrentUser() == null) {
            return false;
        }

        UserRole role = ClientSession.getCurrentUser().getRole();
        return role == UserRole.BIDDER || role == UserRole.SELLER;
    }

    private boolean canUseWalletTransactions() {
        return ClientSession.isLoggedIn()
                && ClientSession.getCurrentUser() != null
                && ClientSession.getCurrentUser().getRole() == UserRole.BIDDER;
    }

    private void applyWalletModeByRole() {
        boolean bidder = canUseWalletTransactions();

        setDisabled(depositButton, !bidder);
        setDisabled(withdrawButton, !bidder);
        setDisabled(amountField, !bidder);

        if (paymentMethodGroup != null && !bidder) {
            momoToggle.setDisable(true);
            zalopayToggle.setDisable(true);
            bankingToggle.setDisable(true);
            shopeepayToggle.setDisable(true);
        }

        if (bidder) {
            if (transactionBox != null) {
                transactionBox.setVisible(true);
                transactionBox.setManaged(true);
            }
            if (sellerMaintenanceBox != null) {
                sellerMaintenanceBox.setVisible(false);
                sellerMaintenanceBox.setManaged(false);
            }
        } else {
            if (transactionBox != null) {
                transactionBox.setVisible(false);
                transactionBox.setManaged(false);
            }
            if (sellerMaintenanceBox != null) {
                sellerMaintenanceBox.setVisible(true);
                sellerMaintenanceBox.setManaged(true);
            }
            showMessage("Chế độ xem số dư ví (Dành riêng cho Seller). Chức năng đang cập nhật.");
        }
    }

    /**
     * Hien thi thong tin user hien tai tu ClientSession.
     * So du van se duoc refresh lai bang GET_USER_PROFILE.
     */
    private void bindCurrentUserToHeader() {
        UserDTO user = ClientSession.getCurrentUser();
        if (user == null) {
            return;
        }

        setLabelText(usernameLabel, safeText(user.getUsername()));
        setLabelText(roleLabel, user.getRole() == null ? "" : user.getRole().name());
        renderBalances(user);
    }

    /**
     * FXML action: bam nut Refresh.
     *
     * Goi GET_USER_PROFILE de lay so du moi nhat tu Server.
     */
    @FXML
    private void handleRefresh() {
        resetFormToNormalState();
        loadWalletProfile();
    }

    /**
     * Lấy chuỗi tên phương thức đang được tích chọn trong ToggleGroup
     */
    private String getSelectedPaymentMethodName() {
        if (paymentMethodGroup == null || paymentMethodGroup.getSelectedToggle() == null) {
            return null;
        }
        ToggleButton selected = (ToggleButton) paymentMethodGroup.getSelectedToggle();
        if (selected == momoToggle) return "Ví MoMo";
        if (selected == zalopayToggle) return "Ví ZaloPay";
        if (selected == bankingToggle) return "Ngân Hàng Điện Tử";
        if (selected == shopeepayToggle) return "ShopeePay";
        return "Cổng thanh toán";
    }

    /**
     * FXML action: bam nut Nap tien.
     *
     * CHỈNH SỬA: Đọc số tiền, kiểm tra ô tích chọn ➔ Mở khung QR bên phải, làm mờ form bên trái.
     */
    @FXML
    private void handleDeposit() {
        if (!canUseWalletTransactions()) {
            showError("Seller chi duoc xem vi, khong duoc nap tien.");
            return;
        }

        String method = getSelectedPaymentMethodName();
        if (method == null) {
            showError("Vui lòng click chọn một ô phương thức thanh toán (MoMo, ZaloPay...) phía dưới.");
            return;
        }

        Double amount = readAmountFromInput();
        if (amount == null) {
            return;
        }

        isDepositAction = true;
        pendingAmount = amount;

        activateQRLayoutFlow(true, amount, method);
    }

    /**
     * FXML action: bam nut Rut tien.
     *
     * CHỈNH SỬA: Kiểm tra số dư khả dụng, ô tích chọn ➔ Mở khung QR bên phải, làm mờ form bên trái.
     */
    @FXML
    private void handleWithdraw() {
        if (!canUseWalletTransactions()) {
            showError("Seller chi duoc xem vi, khong duoc rut tien trong phien ban hien tai.");
            return;
        }

        String method = getSelectedPaymentMethodName();
        if (method == null) {
            showError("Vui lòng click chọn một ô phương thức để nhận tiền rút.");
            return;
        }

        Double amount = readAmountFromInput();
        if (amount == null) {
            return;
        }

        UserDTO currentUser = ClientSession.getCurrentUser();
        if (currentUser != null && currentUser.getAvailableBalance() < amount) {
            showError("So du kha dung khong du de rut so tien nay.");
            return;
        }

        isDepositAction = false;
        pendingAmount = amount;

        activateQRLayoutFlow(false, amount, method);
    }

    /**
     * Kích hoạt chế độ mở rộng QR bên phải và làm mờ toàn bộ form nhập bên trái
     */
    private void activateQRLayoutFlow(boolean isDeposit, double amount, String method) {
        if (qrRightPane == null || formLeftPane == null) return;

        String formattedMoney = moneyFormat.format(amount);
        if (isDeposit) {
            setLabelText(qrTitleLabel, "MÃ QR NẠP TIỀN (" + method.toUpperCase() + ")");
            setLabelText(qrHintLabel, "Quét mã để nạp số tiền: " + formattedMoney);
        } else {
            setLabelText(qrTitleLabel, "MÃ QR RÚT TIỀN (" + method.toUpperCase() + ")");
            setLabelText(qrHintLabel, "Quét mã để xác nhận chuyển " + formattedMoney + " về tài khoản.");
        }

        qrRightPane.setVisible(true);
        qrRightPane.setManaged(true);

        formLeftPane.setDisable(true);

        showMessage("Đang chờ quét mã QR giao dịch...");
    }

    /**
     * FXML action mới: Xử lý nút hủy (X). Hiển thị cảnh báo đổi ý trước khi đóng.
     */
    @FXML
    private void handleCancelQR() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Hủy giao dịch");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Bạn có chắc chắn muốn hủy bỏ giao dịch hiện tại và đóng mã QR không?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            resetFormToNormalState();
            showMessage("Đã hủy yêu cầu giao dịch.");
        }
    }

    /**
     * Khôi phục form bên trái sáng lên bình thường và ẩn phần QR bên phải đi
     */
    private void resetFormToNormalState() {
        if (qrRightPane != null) {
            qrRightPane.setVisible(false);
            qrRightPane.setManaged(false);
        }
        if (formLeftPane != null) {
            formLeftPane.setDisable(false);
        }
        if (amountField != null) {
            amountField.clear();
        }
        if (paymentMethodGroup != null && paymentMethodGroup.getSelectedToggle() != null) {
            paymentMethodGroup.getSelectedToggle().setSelected(false);
        }
    }

    /**
     * FXML action mới: Xác nhận đã quét mã QR.
     * Lúc này hệ thống chính thức đẩy lệnh Socket Request lên Server.
     */
    @FXML
    private void handleConfirmQRAction() {
        if (pendingAmount <= 0) {
            showError("Yêu cầu giao dịch không hợp lệ.");
            return;
        }

        this.isProcessingTransaction = true;

        if (isDepositAction) {
            runWalletAction(
                    "Dang nap tien sau khi quet ma QR...",
                    () -> walletApi.depositMoney(pendingAmount)
            );
        } else {
            runWalletAction(
                    "Dang rut tien sau khi quet ma QR...",
                    () -> walletApi.withdrawMoney(pendingAmount)
            );
        }

        if (qrRightPane != null) {
            qrRightPane.setVisible(false);
            qrRightPane.setManaged(false);
        }
    }

    /**
     * FXML action: quay lai Dashboard.
     */
    @FXML
    private void handleBack() {
        SceneNavigator.showDashboard();
    }

    /**
     * Goi GET_USER_PROFILE qua backend.
     *
     * Ham nay tach rieng vi can dung khi:
     * - vua mo man Wallet
     * - bam Refresh
     * - sau khi can dong bo lai user
     */
    private void loadWalletProfile() {
        runWalletAction(
                "Dang tai thong tin vi...",
                walletApi::getUserProfile
        );
    }

    /**
     * Chay request socket tren background thread de JavaFX UI khong bi dung.
     *
     * Supplier tra ve SocketResponse tu ClientWalletApi.
     * Neu response thanh cong va co UserDTO, controller cap nhat:
     * - ClientSession
     * - cac label so du
     * - message tren UI
     */
    private void runWalletAction(String loadingMessage, WalletRequestSupplier requestSupplier) {
        setBusy(true);
        showMessage(loadingMessage);

        Task<SocketResponse> task = new Task<>() {
            @Override
            protected SocketResponse call() {
                return requestSupplier.get();
            }
        };

        task.setOnSucceeded(event -> {
            SocketResponse response = task.getValue();
            handleWalletResponse(response);
            setBusy(false);
        });

        task.setOnFailed(event -> {
            Throwable error = task.getException();
            showError(error == null ? "Khong the ket noi server." : error.getMessage());
            setBusy(false);
        });

        Thread worker = new Thread(task, "wallet-request-thread");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Xu ly response tu Server cho ca 3 action:
     * - GET_USER_PROFILE
     * - DEPOSIT_MONEY
     * - WITHDRAW_MONEY
     */
    private void handleWalletResponse(SocketResponse response) {
        if (response == null) {
            showError("Server khong tra ve phan hoi.");
            if (formLeftPane != null) formLeftPane.setDisable(false);
            return;
        }

        if (!response.isSuccess()) {
            showError(response.getMessage() == null ? "Thao tac vi that bai." : response.getMessage());
            if (formLeftPane != null) formLeftPane.setDisable(false);
            return;
        }

        UserDTO updatedUser = walletApi.parseUser(response);
        if (updatedUser == null) {
            showError("Server tra ve du lieu vi khong hop le.");
            if (formLeftPane != null) formLeftPane.setDisable(false);
            return;
        }

        ClientSession.saveLoginSession(ClientSession.getToken(), updatedUser);
        renderBalances(updatedUser);
        bindCurrentUserToHeader();

        this.pendingAmount = 0.0;
        if (formLeftPane != null) {
            formLeftPane.setDisable(false);
        }
        if (amountField != null) {
            amountField.clear();
        }
        if (paymentMethodGroup != null && paymentMethodGroup.getSelectedToggle() != null) {
            paymentMethodGroup.getSelectedToggle().setSelected(false);
        }

        showMessage("Cap nhat vi thanh cong.");

        if (this.isProcessingTransaction) {
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Giao dịch thành công");
            successAlert.setHeaderText(null);

            if (isDepositAction) {
                successAlert.setContentText("Chúc mừng! Bạn đã nạp tiền vào ví thành công.\nSố dư khả dụng hiện tại: " + moneyFormat.format(updatedUser.getAvailableBalance()));
            } else {
                successAlert.setContentText("Yêu cầu rút tiền thành công!\nHệ thống đang chuyển tiền về tài khoản của bạn.\nSố dư khả dụng hiện tại: " + moneyFormat.format(updatedUser.getAvailableBalance()));
            }

            successAlert.showAndWait();

            this.isProcessingTransaction = false;
        }
    }
    /**
     * Doc va validate amount tu TextField.
     *
     * Chap nhan nguoi dung nhap:
     * - 100000
     * - 100,000
     * - 100.000
     *
     * Controller chuan hoa ve double truoc khi gui DTO sang Server.
     */
    private Double readAmountFromInput() {
        if (amountField == null || isBlank(amountField.getText())) {
            showError("Vui long nhap so tien.");
            return null;
        }

        String raw = amountField.getText().trim()
                .replace(" ", "")
                .replace(",", "")
                .replace(".", "");

        double amount;
        try {
            amount = Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            showError("So tien khong hop le.");
            return null;
        }

        if (amount <= 0) {
            showError("So tien phai lon hon 0.");
            return null;
        }

        return amount;
    }

    /**
     * Render so du len UI.
     */
    private void renderBalances(UserDTO user) {
        if (user == null) {
            return;
        }

        double available = user.getAvailableBalance();
        double frozen = user.getFrozenBalance();

        setLabelText(availableBalanceLabel, moneyFormat.format(available));
        setLabelText(frozenBalanceLabel, moneyFormat.format(frozen));
        setLabelText(totalBalanceLabel, moneyFormat.format(available + frozen));
    }

    /**
     * Khoa/mo cac control khi dang goi server.
     * Tranh user bam lien tuc tao nhieu request nap/rut cung luc.
     */
    private void setBusy(boolean busy) {
        Platform.runLater(() -> {
            boolean transactionAllowed = canUseWalletTransactions();

            if (transactionAllowed) {
                setDisabled(depositButton, busy);
                setDisabled(withdrawButton, busy);
                setDisabled(amountField, busy);
                setDisabled(confirmQRButton, busy);
                setDisabled(cancelQRButton, busy);
            }
        });
    }

    private void setDisabled(javafx.scene.Node node, boolean disabled) {
        if (node != null) {
            node.setDisable(disabled);
        }
    }

    private void showMessage(String message) {
        setLabelText(messageLabel, message);
    }

    private void showError(String message) {
        setLabelText(messageLabel, message);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Loi vi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void setLabelText(Label label, String text) {
        if (label != null) {
            label.setText(text == null ? "" : text);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    /**
     * Functional interface nho de runWalletAction nhan duoc cac method:
     * - walletApi.getUserProfile
     * - walletApi.depositMoney
     * - walletApi.withdrawMoney
     */
    @FunctionalInterface
    private interface WalletRequestSupplier {
        SocketResponse get();
    }
}