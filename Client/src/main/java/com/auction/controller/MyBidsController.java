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
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Bộ điều khiển (Controller) hoặc lớp tiện ích MyBidsController xử lý giao diện Client JavaFX.
 */
public class MyBidsController {
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final ClientBidHistoryApi bidHistoryApi = new ClientBidHistoryApi();
    private final ObservableList<BidTransactionDTO> bids = FXCollections.observableArrayList();

    private final NumberFormat moneyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private int currentPage = 1;
    private int totalPages = 0;
    private long totalElements = 0;

    @FXML private Parent rootContainer;
    @FXML private Label pageLabel;
    @FXML private Label messageLabel;
    @FXML private Label totalBidsLabel;

    @FXML private Label headerAvailableBalance;
    @FXML private Label headerFrozenBalance;
    @FXML private Label headerTotalBalance;

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
        com.auction.util.HeaderBalanceHelper.setupHeaderBalance(headerAvailableBalance, headerFrozenBalance, headerTotalBalance);
        applyTheme();

        if (!isBidderSession()) {
            showMessage("Chỉ tài khoản Bidder mới được xem lịch sử đặt giá.");
            SceneNavigator.showDashboard();
            return;
        }

        initializeDefaults();
        initializeTable();

        if (pageSizeField != null) {
            pageSizeField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null || newVal.trim().isEmpty()) {
                    return;
                }
                try {
                    int parsedSize = Integer.parseInt(newVal.trim());
                    if (parsedSize > 0) {
                        currentPage = 1;
                        loadPage(currentPage);
                    }
                } catch (NumberFormatException e) {
                }
            });
        }

        updateTablePlaceholder(SceneNavigator.isAppDarkMode);
        loadPage(1);

        autoRefreshTimeline = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(3), event -> {
            if (myBidsTable == null || myBidsTable.getScene() == null || myBidsTable.getScene().getWindow() == null) {
                stopAutoRefresh();
            } else {
                loadPageSilently(currentPage);
            }
        }));
        autoRefreshTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        autoRefreshTimeline.play();
    }

    private javafx.animation.Timeline autoRefreshTimeline;

    private void loadPageSilently(int page) {
        int pageSize = readPageSize();
        Task<SocketResponse> task = new Task<>() {
            @Override
            protected SocketResponse call() {
                return bidHistoryApi.getMyBidHistory(page, pageSize);
            }
        };

        task.setOnSucceeded(event -> {
            SocketResponse response = task.getValue();
            if (response != null && response.isSuccess()) {
                BidTransactionDTO selected = (myBidsTable != null) ? myBidsTable.getSelectionModel().getSelectedItem() : null;
                String selectedBidId = selected != null ? selected.getBidId() : null;

                PageDTO<BidTransactionDTO> pageData = bidHistoryApi.parseMyBidHistoryPage(response);
                totalPages = pageData.getTotalPages();
                totalElements = pageData.getTotalElements();
                
                int displayPage = page;
                if (displayPage > totalPages && totalPages > 0) {
                    displayPage = totalPages;
                }
                currentPage = displayPage;

                List<BidTransactionDTO> data = pageData.getData() == null ? List.of() : pageData.getData();
                bids.setAll(sortBidsByTimeDesc(data));

                if (selectedBidId != null) {
                    final String selBidId = selectedBidId;
                    Platform.runLater(() -> selectBidInTable(selBidId));
                }

                updatePaginationLabels();
                updateNavigationButtons(false);
            }
        });

        Thread thread = new Thread(task, "my-bids-silent-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private List<BidTransactionDTO> sortBidsByTimeDesc(List<BidTransactionDTO> list) {
        if (list == null) return List.of();
        java.util.ArrayList<BidTransactionDTO> sorted = new java.util.ArrayList<>(list);
        sorted.sort((b1, b2) -> {
            if (b1.getTime() == null && b2.getTime() == null) return 0;
            if (b1.getTime() == null) return 1;
            if (b2.getTime() == null) return -1;
            return b2.getTime().compareTo(b1.getTime());
        });
        return sorted;
    }

    private void stopAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
            autoRefreshTimeline = null;
        }
    }

    private void applyTheme() {
        if (rootContainer == null) return;
        rootContainer.getStylesheets().clear();
        String cssPath = SceneNavigator.isAppDarkMode
                ? "/com/auction/client/view/dark.css"
                : "/com/auction/client/view/light.css";
        try {
            String css = Objects.requireNonNull(getClass().getResource(cssPath)).toExternalForm();
            rootContainer.getStylesheets().add(css);
        } catch (Exception e) {
            System.out.println("Không thể nạp theme cho MyBids: " + cssPath);
        }
    }

    private boolean isBidderSession() {
        return ClientSession.isLoggedIn()
                && ClientSession.getCurrentUser() != null
                && ClientSession.getCurrentUser().getRole() == UserRole.BIDDER;
    }

    private void initializeDefaults() {
        if (pageSizeField != null && isBlank(pageSizeField.getText())) {
            pageSizeField.setText(String.valueOf(DEFAULT_PAGE_SIZE));
        }
        updatePaginationLabels();
        updateNavigationButtons(false);
        showMessage("Đang tải lịch sử đặt giá...");
    }

    private void initializeTable() {
        if (myBidsTable == null) return;
        myBidsTable.setItems(bids);

        if (amountColumn != null) {
            amountColumn.setCellValueFactory(data -> new SimpleStringProperty(formatMoney(data.getValue().getAmount())));
            amountColumn.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                        setStyle("-fx-alignment: center-right; -fx-font-weight: bold;");
                    }
                }
            });
        }
        if (statusColumn != null) {
            statusColumn.setCellValueFactory(data -> new SimpleStringProperty(safeText(data.getValue().getStatus())));
            statusColumn.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        setStyle("-fx-alignment: center; -fx-font-weight: bold; -fx-font-size: 14px;");

                        getStyleClass().removeAll("status-running", "status-open", "status-finished");

                        String status = item.toUpperCase().trim();
                        // Tự động phân tách hệ màu thông minh: ACCEPT/SUCCESS -> XANH LÁ, REFUND/FAILED -> ĐỎ
                        if (status.contains("ACCEPT")) {
                            setStyle(getStyle() + "-fx-text-fill: #2ecc71 !important;");
                        } else if (status.contains("REFUND")) {
                            setStyle(getStyle() + "-fx-text-fill: #e74c3c !important;");
                        } else {
                            setStyle(getStyle() + "-fx-text-fill: #e67e22 !important;");
                        }
                    }
                }
            });
        }
        if (timeColumn != null) {
            timeColumn.setCellValueFactory(data -> new SimpleStringProperty(formatTime(data.getValue().getTime())));
            timeColumn.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        getStyleClass().remove("datetime"); // Xóa class tránh lỗi lặp UI khi cuộn bảng
                    } else {
                        setText(item);
                        setStyle("-fx-alignment: center;");

                        // Gắn class định danh "datetime" để FXML và CSS nhận diện in đậm, đổi màu giống chữ thường
                        if (!getStyleClass().contains("datetime")) {
                            getStyleClass().add("datetime");
                        }
                    }
                }
            });
        }
    }

    /**
     * KHẮC PHỤC SỐ 3: Nút Làm mới đóng vai trò F5 Reload, bảo toàn cấu hình người dùng
     */
    @FXML
    private void handleRefresh() {
        int pageSize = readPageSize();

        loadPage(currentPage);
    }

    @FXML
    private void handlePreviousPage() {
        if (currentPage > 1) {
            loadPage(currentPage - 1);
        }
    }

    @FXML
    private void handleNextPage() {
        if (totalPages > 0 && currentPage < totalPages) {
            loadPage(currentPage + 1);
        }
    }

    @FXML
    private void handleBack() {
        stopAutoRefresh();
        SceneNavigator.showDashboard();
    }

    /**
     * Tải trang dữ liệu bất đồng bộ từ Server qua luồng Socket ngầm
     */
    private void loadPage(int page) {
        int pageSize = readPageSize();

        setBusy(true);
        showMessage("Đang tải lịch sử đặt giá...");

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
            showMessage("Không thể tải lịch sử đặt giá từ hệ thống.");
            setBusy(false);
        });

        Thread worker = new Thread(task, "my-bids-request-thread");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Nhận và xử lý gói tin SocketResponse trả về từ Server phân trang
     */
    private void handlePageResponse(SocketResponse response, int requestedPage) {
        BidTransactionDTO selected = (myBidsTable != null) ? myBidsTable.getSelectionModel().getSelectedItem() : null;
        String selectedBidId = selected != null ? selected.getBidId() : null;

        if (response == null) {
            showMessage("Server không phản hồi. Vui lòng kiểm tra kết nối.");
            updateNavigationButtons(false);
            return;
        }

        if (!response.isSuccess()) {
            showMessage(response.getMessage() == null ? "Tải lịch sử đặt giá thất bại." : response.getMessage());
            updateNavigationButtons(false);
            return;
        }

        PageDTO<BidTransactionDTO> pageData = bidHistoryApi.parseMyBidHistoryPage(response);

        currentPage = requestedPage;
        totalPages = pageData.getTotalPages();
        totalElements = pageData.getTotalElements();

        if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages;
        }

        List<BidTransactionDTO> data = pageData.getData() == null ? List.of() : pageData.getData();
        bids.setAll(sortBidsByTimeDesc(data));

        if (selectedBidId != null) {
            final String selBidId = selectedBidId;
            Platform.runLater(() -> selectBidInTable(selBidId));
        }

        updatePaginationLabels();
        updateNavigationButtons(false);

        if (bids.isEmpty()) {
            showMessage("Bạn chưa thực hiện lượt đặt giá nào.");
        } else {
            showMessage("Đã cập nhật dữ liệu lịch sử đặt giá mới nhất.");
        }
    }

    /**
     * Đọc và kiểm tra tính an toàn của ô nhập kích thước trang (Page Size)
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

    private void updatePaginationLabels() {
        int displayTotalPages = Math.max(totalPages, 1);
        if (pageLabel != null) {
            pageLabel.setText("Trang " + currentPage + " / " + displayTotalPages);
        }
        if (totalBidsLabel != null) {
            totalBidsLabel.setText("Tổng cộng: " + totalElements + " lượt bid");
        }
    }

    private void updateNavigationButtons(boolean busy) {
        if (previousPageButton != null) {
            boolean disablePrev = busy || currentPage <= 1;
            previousPageButton.setDisable(disablePrev);
            previousPageButton.setStyle(disablePrev ? "-fx-opacity: 0.35;" : "-fx-opacity: 1.0; -fx-cursor: hand;");
        }

        if (nextPageButton != null) {
            boolean disableNext = busy || totalPages <= 0 || currentPage >= totalPages;
            nextPageButton.setDisable(disableNext);
            nextPageButton.setStyle(disableNext ? "-fx-opacity: 0.35;" : "-fx-opacity: 1.0; -fx-cursor: hand;");
        }
    }

    private void setBusy(boolean busy) {
        setDisabled(refreshButton, busy);
        setDisabled(backButton, busy);
        setDisabled(pageSizeField, busy);
        updateNavigationButtons(busy);
    }

    private void setDisabled(javafx.scene.Node node, boolean disabled) {
        if (node != null) {
            node.setDisable(disabled);
        }
    }

    private String formatMoney(double amount) {
        return moneyFormat.format(amount);
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "" : time.format(timeFormatter);
    }

    private void showMessage(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message == null ? "" : message);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    /**
     * Cập nhật thông báo bảng trống (Placeholder) linh hoạt theo chế độ Sáng/Tối
     * và đồng bộ màu sắc chất lượng cao chuẩn Clean UI.
     */
    private void updateTablePlaceholder(boolean isDarkMode) {
        if (myBidsTable == null) return;

        VBox emptyBox = new VBox(10);
        emptyBox.setStyle("-fx-alignment: center; -fx-padding: 40;");

        Label iconLabel = new Label();
        Label msgLabel = new Label();

        if (isDarkMode) {
            iconLabel.setText("🚫🔒");
            iconLabel.setStyle("-fx-font-size: 36px; -fx-opacity: 0.8;");

            msgLabel.setText("Thông tin mật vẫn chưa được lộ ra...");
            msgLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-font-style: italic; -fx-text-fill: #E5B869;");
        } else {
            iconLabel.setText("📋✨");
            iconLabel.setStyle("-fx-font-size: 32px; -fx-opacity: 0.6;");

            String cleanMessage = "Chưa có dữ liệu lịch sử đặt giá nào được ghi nhận.";
            msgLabel.setText(cleanMessage);
            msgLabel.setStyle("-fx-font-size: 13.5px; -fx-font-style: italic; -fx-text-fill: #4B5563;");
        }

        emptyBox.getChildren().addAll(iconLabel, msgLabel);

        myBidsTable.setPlaceholder(emptyBox);
    }

    private void selectBidInTable(String bidId) {
        if (myBidsTable == null || bidId == null || bidId.trim().isEmpty()) {
            return;
        }
        for (BidTransactionDTO bid : bids) {
            if (bidId.equals(bid.getBidId())) {
                myBidsTable.getSelectionModel().select(bid);
                return;
            }
        }
    }
}