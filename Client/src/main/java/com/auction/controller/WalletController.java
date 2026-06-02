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
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * WalletController la controller phia Client cho man hinh vi cua Bidder.
 *
 * Lien he voi cac class quan trong:
 * - ClientWalletApi:
 *   Tao SocketRequest va gui action GET_USER_PROFILE / DEPOSIT_MONEY / WITHDRAW_MONEY sang Server.
 *
 * - ClientSocketService:
 *   Nam ben trong ClientWalletApi, la noi gui request that qua socket va cho SocketResponse tu Server.
 *
 * - UserController phia Server:
 *   Nhan request wallet, validate amount, roi goi UserService.
 *
 * - UserService phia Server:
 *   Xu ly nghiep vu nap/rut tien, dong bo RAM + database.
 *
 * - AuthorizationService phia Server:
 *   Hien tai chi cho BIDDER goi DEPOSIT_MONEY va WITHDRAW_MONEY.
 *
 * - ClientSession:
 *   Luu user hien tai o phia Client. Sau khi nap/rut thanh cong,
 *   controller cap nhat lai ClientSession bang UserDTO moi Server tra ve.
 */
public class WalletController {
    private final ClientUserApi walletApi = new ClientUserApi();

    private final NumberFormat moneyFormat =
            NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    // =========================
    // FXML contract
    // =========================
    // wallet.fxml can khai bao dung cac fx:id duoi day.

    @FXML private Parent rootContainer;

    @FXML private Label usernameLabel;
    @FXML private Label roleLabel;
    @FXML private Label availableBalanceLabel;
    @FXML private Label frozenBalanceLabel;
    @FXML private Label totalBalanceLabel;
    @FXML private Label messageLabel;

    @FXML private TextField amountField;

    @FXML private Button refreshButton;
    @FXML private Button depositButton;
    @FXML private Button withdrawButton;
    @FXML private Button backButton;

    @FXML
    public void initialize() {
        applyTheme();

        /*
         * Chan tu phia Client de tranh user khong dung role vao nham man hinh.
         * Day chi la guard UI. Server van la noi check quyen that su.
         */
        if (!isWalletViewerSession()) {
            showError("Chi tai khoan Bidder hoac Seller moi duoc xem vi.");
            SceneNavigator.showDashboard();
            return;
        }

        bindCurrentUserToHeader();
        setBusy(false);
        applyWalletModeByRole();
        showMessage("Dang tai thong tin vi...");

        /*
         * Khi mo man hinh, luon lay lai profile tu Server.
         * Khong chi tin vao ClientSession vi so du co the da thay doi sau khi dau gia.
         */
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

        if (!bidder) {
            showMessage("Seller chi duoc xem so du vi. Chuc nang nap/rut hien chi danh cho Bidder.");
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
        loadWalletProfile();
    }

    /**
     * FXML action: bam nut Nap tien.
     *
     * Luong chuan:
     * - Doc amount tu amountField.
     * - Validate client-side de bat loi nhanh.
     * - Gui DEPOSIT_MONEY qua ClientWalletApi.
     * - Server tra UserDTO moi, controller cap nhat UI + ClientSession.
     */
    @FXML
    private void handleDeposit() {
        if (!canUseWalletTransactions()) {
            showError("Seller chi duoc xem vi, khong duoc nap tien.");
            return;
        }
        Double amount = readAmountFromInput();
        if (amount == null) {
            return;
        }

        runWalletAction(
                "Dang nap tien...",
                () -> walletApi.depositMoney(amount)
        );
    }

    /**
     * FXML action: bam nut Rut tien.
     *
     * Luong chuan:
     * - Doc amount tu amountField.
     * - Validate amount > 0.
     * - Gui WITHDRAW_MONEY qua ClientWalletApi.
     * - Server se check so du kha dung va trang thai tai khoan.
     */
    @FXML
    private void handleWithdraw() {
        if (!canUseWalletTransactions()) {
            showError("Seller chi duoc xem vi, khong duoc rut tien trong phien ban hien tai.");
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

        runWalletAction(
                "Dang rut tien...",
                () -> walletApi.withdrawMoney(amount)
        );
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
            return;
        }

        if (!response.isSuccess()) {
            showError(response.getMessage() == null
                    ? "Thao tac vi that bai."
                    : response.getMessage());
            return;
        }

        UserDTO updatedUser = walletApi.parseUser(response);
        if (updatedUser == null) {
            showError("Server tra ve du lieu vi khong hop le.");
            return;
        }

        /*
         * Cap nhat ClientSession bang UserDTO moi.
         * Can giu lai token cu vi saveLoginSession se ghi de ca token va user.
         */
        ClientSession.saveLoginSession(ClientSession.getToken(), updatedUser);

        renderBalances(updatedUser);
        bindCurrentUserToHeader();

        if (amountField != null) {
            amountField.clear();
        }

        showMessage("Cap nhat vi thanh cong.");
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

            setDisabled(depositButton, busy || !transactionAllowed);
            setDisabled(withdrawButton, busy || !transactionAllowed);
            setDisabled(amountField, busy || !transactionAllowed);
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