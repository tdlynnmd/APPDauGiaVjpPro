package com.auction.controller;

import com.auction.dto.BidTransactionDTO;
import com.auction.dto.PageDTO;
import com.auction.dto.SocketResponse;
import com.auction.enums.UserRole;
import com.auction.network.ClientBidHistoryApi;
import com.auction.util.ClientSession;
import com.auction.util.SceneNavigator;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * MyBidsController la controller phia Client cho man hinh "My Bids".
 *
 * Chuc nang:
 * - Hien thi lich su cac lan dat gia cua Bidder dang dang nhap.
 * - Ho tro phan trang.
 * - Chi cho BIDDER vao man hinh nay.
 *
 * Lien he voi backend:
 * - Controller goi ClientBidHistoryApi.
 * - ClientBidHistoryApi gui action GET_MY_BID_HISTORY.
 * - Server RequestDispatcher lay bidderId tu ClientSession phia Server.
 * - UserController va BidTransactionService tra ve PageDTO<BidTransactionDTO>.
 *
 * Luu y hien tai:
 * - BidTransactionDTO chua co auctionId/itemName.
 * - Vi vay man hinh nay hien thi duoc amount, time, status.
 * - Neu sau nay muon bam vao dong bid de mo auction detail, can mo rong DTO backend.
 */
public class MyBidsController {
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final ClientBidHistoryApi bidHistoryApi = new ClientBidHistoryApi();
    private final ObservableList<BidTransactionDTO> bids = FXCollections.observableArrayList();

    private final NumberFormat moneyFormat =
            NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    private final DateTimeFormatter timeFormatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private int currentPage = 1;
    private int totalPages = 0;
    private long totalElements = 0;

    // =========================
    // FXML contract
    // =========================

    @FXML private Parent rootContainer;

    @FXML private Label pageLabel;
    @FXML private Label messageLabel;
    @FXML private Label totalBidsLabel;

    @FXML private TextField pageSizeField;

    @FXML private TableView<BidTransactionDTO> myBidsTable;
    @FXML private TableColumn<BidTransactionDTO, String> amountColumn;
    @FXML private TableColumn<BidTransactionDTO, String> statusColumn;
    @FXML private TableColumn<BidTransactionDTO, String> timeColumn;

    @FXML private Button refreshButton;
    @FXML private Button previousPageButton;
    @FXML private Button nextPageButton;
    @FXML private Button backButton;

    @FXML
    public void initialize() {
        applyTheme();

        /*
         * Guard phia Client de tranh user sai role vao nham man hinh.
         * Server van la noi check quyen that su bang AuthorizationService.
         */
        if (!isBidderSession()) {
            showMessage("Chi tai khoan Bidder moi duoc xem lich su dat gia.");
            SceneNavigator.showDashboard();
            return;
        }

        initializeDefaults();
        initializeTable();

        loadPage(1);
    }

    /**
     * Ap dung theme hien tai cua app.
     *
     * Neu FXML chua khai bao rootContainer thi bo qua de tranh crash.
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
            System.out.println("Khong the nap theme cho MyBids: " + cssPath);
        }
    }

    /**
     * Kiem tra user hien tai co phai Bidder khong.
     *
     * Backend hien chi cho BIDDER goi GET_MY_BID_HISTORY.
     */
    private boolean isBidderSession() {
        return ClientSession.isLoggedIn()
                && ClientSession.getCurrentUser() != null
                && ClientSession.getCurrentUser().getRole() == UserRole.BIDDER;
    }

    /**
     * Gan gia tri mac dinh cho control phan trang.
     */
    private void initializeDefaults() {
        if (pageSizeField != null && isBlank(pageSizeField.getText())) {
            pageSizeField.setText(String.valueOf(DEFAULT_PAGE_SIZE));
        }

        updatePaginationLabels();
        updateNavigationButtons(false);
        showMessage("Dang tai lich su dat gia...");
    }

    /**
     * Cau hinh TableView.
     *
     * TableView chi bind voi ObservableList bids.
     * Khi bids.setAll(...) duoc goi, UI se tu cap nhat.
     */
    private void initializeTable() {
        if (myBidsTable == null) {
            return;
        }

        myBidsTable.setItems(bids);

        if (amountColumn != null) {
            amountColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(formatMoney(data.getValue().getAmount()))
            );
        }

        if (statusColumn != null) {
            statusColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(safeText(data.getValue().getStatus()))
            );
        }

        if (timeColumn != null) {
            timeColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(formatTime(data.getValue().getTime()))
            );
        }
    }

    /**
     * FXML action: tai lai trang hien tai.
     */
    @FXML
    private void handleRefresh() {
        loadPage(currentPage);
    }

    /**
     * FXML action: ve trang truoc.
     */
    @FXML
    private void handlePreviousPage() {
        if (currentPage > 1) {
            loadPage(currentPage - 1);
        }
    }

    /**
     * FXML action: sang trang tiep theo.
     */
    @FXML
    private void handleNextPage() {
        if (totalPages > 0 && currentPage < totalPages) {
            loadPage(currentPage + 1);
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
     * Tai mot trang lich su bid tu backend.
     *
     * Request duoc chay trong Task de UI JavaFX khong bi dung khi doi socket response.
     */
    private void loadPage(int page) {
        int pageSize = readPageSize();

        setBusy(true);
        showMessage("Dang tai lich su dat gia...");

        Task<SocketResponse> task = new Task<>() {
            @Override
            protected SocketResponse call() {
                return bidHistoryApi.getMyBidHistory(page, pageSize);
            }
        };

        task.setOnSucceeded(event -> {
            handlePageResponse(task.getValue(), page);
            setBusy(false);
        });

        task.setOnFailed(event -> {
            showMessage("Khong the tai lich su dat gia.");
            setBusy(false);
        });

        Thread worker = new Thread(task, "my-bids-request-thread");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Xu ly SocketResponse tra ve tu GET_MY_BID_HISTORY.
     *
     * Neu thanh cong:
     * - parse PageDTO<BidTransactionDTO>
     * - cap nhat currentPage/totalPages/totalElements
     * - do data vao TableView
     */
    private void handlePageResponse(SocketResponse response, int requestedPage) {
        if (response == null) {
            showMessage("Server khong tra ve phan hoi.");
            updateNavigationButtons(false);
            return;
        }

        if (!response.isSuccess()) {
            showMessage(response.getMessage() == null
                    ? "Tai lich su dat gia that bai."
                    : response.getMessage());
            updateNavigationButtons(false);
            return;
        }

        PageDTO<BidTransactionDTO> page = bidHistoryApi.parseMyBidHistoryPage(response);

        currentPage = requestedPage;
        totalPages = page.getTotalPages();
        totalElements = page.getTotalElements();

        List<BidTransactionDTO> data = page.getData() == null ? List.of() : page.getData();
        bids.setAll(data);

        updatePaginationLabels();
        updateNavigationButtons(false);

        if (bids.isEmpty()) {
            showMessage("Ban chua co luot dat gia nao.");
        } else {
            showMessage("Da tai lich su dat gia.");
        }
    }

    /**
     * Doc pageSize tu TextField.
     *
     * Neu nguoi dung nhap sai, fallback ve DEFAULT_PAGE_SIZE.
     */
    private int readPageSize() {
        if (pageSizeField == null || isBlank(pageSizeField.getText())) {
            return DEFAULT_PAGE_SIZE;
        }

        try {
            int value = Integer.parseInt(pageSizeField.getText().trim());
            return value <= 0 ? DEFAULT_PAGE_SIZE : value;
        } catch (NumberFormatException e) {
            return DEFAULT_PAGE_SIZE;
        }
    }

    /**
     * Cap nhat label hien thi trang va tong so bid.
     */
    private void updatePaginationLabels() {
        int displayTotalPages = Math.max(totalPages, 1);

        if (pageLabel != null) {
            pageLabel.setText("Trang " + currentPage + "/" + displayTotalPages);
        }

        if (totalBidsLabel != null) {
            totalBidsLabel.setText("Tong " + totalElements + " luot bid");
        }
    }

    /**
     * Khoa/mo button khi dang goi server va theo trang hien tai.
     */
    private void updateNavigationButtons(boolean busy) {
        setDisabled(previousPageButton, busy || currentPage <= 1);
        setDisabled(nextPageButton, busy || totalPages <= 0 || currentPage >= totalPages);
    }

    /**
     * Khoa/mo cac control khi dang load.
     */
    private void setBusy(boolean busy) {
        setDisabled(refreshButton, busy);
        setDisabled(backButton, busy);
        setDisabled(pageSizeField, busy);
        updateNavigationButtons(busy);
    }

    /**
     * Helper setDisable null-safe.
     */
    private void setDisabled(javafx.scene.Node node, boolean disabled) {
        if (node != null) {
            node.setDisable(disabled);
        }
    }

    /**
     * Format tien theo locale Viet Nam.
     */
    private String formatMoney(double amount) {
        return moneyFormat.format(amount);
    }

    /**
     * Format LocalDateTime server tra ve thanh chuoi hien thi.
     */
    private String formatTime(LocalDateTime time) {
        return time == null ? "" : time.format(timeFormatter);
    }

    /**
     * Hien thi message trang thai tren UI.
     */
    private void showMessage(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message == null ? "" : message);
        }
    }

    /**
     * Check chuoi rong/null.
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Chuyen null thanh chuoi rong de TableView khong hien "null".
     */
    private String safeText(String value) {
        return value == null ? "" : value;
    }
}