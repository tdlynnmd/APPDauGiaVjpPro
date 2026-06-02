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
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * MyBidsController - Phiên bản đã gia cố logic bảo mật mảng,
 * tích hợp lắng nghe thời gian thực ô tự điền số dòng và sửa lỗi đứng TableView.
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

    @FXML private TextField pageSizeField; // Ô tự điền số lượng dòng tối đa/trang

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

        // Bảo vệ màn hình phía Client tránh sai quyền truy cập
        if (!isBidderSession()) {
            showMessage("Chỉ tài khoản Bidder mới được xem lịch sử đặt giá.");
            SceneNavigator.showDashboard();
            return;
        }

        initializeDefaults();
        initializeTable();

        // KHẮC PHỤC SỐ 2: Lắp bộ lắng nghe thay đổi số lượng dòng thời gian thực chuẩn SIM
        if (pageSizeField != null) {
            pageSizeField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null || newVal.trim().isEmpty()) {
                    return; // Người dùng đang xóa trắng để gõ, tạm thời bỏ qua chờ gõ xong
                }
                try {
                    int parsedSize = Integer.parseInt(newVal.trim());
                    if (parsedSize > 0) {
                        // Khi đổi số lượng dòng hiển thị, ép hệ thống quay về trang đầu tiên
                        currentPage = 1;
                        loadPage(currentPage);
                    }
                } catch (NumberFormatException e) {
                    // Nếu gõ chữ bừa bãi, không xử lý để tránh lỗi hệ thống
                }
            });
        }

        updateTablePlaceholder(SceneNavigator.isAppDarkMode);
        // Tải trang đầu tiên khi vừa mở màn hình
        loadPage(1);
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
                        setStyle("-fx-alignment: center-right; -fx-font-weight: bold;"); // Căn phải, chữ đậm
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
                    } else {
                        setText(item);
                        setStyle("-fx-alignment: center; -fx-font-weight: bold;"); // Căn giữa

                        // Xóa các class màu cũ tránh ăn nhầm khi cuộn bảng
                        getStyleClass().removeAll("status-running", "status-open", "status-finished");
                        if ("SUCCESS".equalsIgnoreCase(item)) {
                            setStyle(getStyle() + "-fx-text-fill: #10B981;"); // Màu xanh lá nếu bid thành công
                        } else if ("FAILED".equalsIgnoreCase(item)) {
                            setStyle(getStyle() + "-fx-text-fill: #EF4444;"); // Màu đỏ nếu bid thất bại
                        } else {
                            setStyle(getStyle() + "-fx-text-fill: #F59E0B;"); // Màu cam cho các trạng thái khác (Pending...)
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
                    } else {
                        setText(item);
                        setStyle("-fx-alignment: center; -fx-text-fill: -fx-secondary-text-color;"); // Căn giữa, màu chữ phụ dịu mắt
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
        int pageSize = readPageSize(); // Đọc giá trị an toàn hiện hành đang gõ trong ô nhập

        // Giữ nguyên trang hiện tại để người dùng không bị văng về trang 1 một cách vô lý
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
        // KHẮC PHỤC SỐ 1: Xóa sạch bộ nhớ chọn dòng cũ của TableView để tránh lỗi đứng trang hiển thị
        if (myBidsTable != null) {
            myBidsTable.getSelectionModel().clearSelection();
        }

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

        // Đồng bộ các thông số phân trang từ gói tin Server trả về
        currentPage = requestedPage;
        totalPages = pageData.getTotalPages();
        totalElements = pageData.getTotalElements();

        // Đảm bảo an toàn biên số trang hiện tại phòng hờ dữ liệu biến động
        if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages;
        }

        List<BidTransactionDTO> data = pageData.getData() == null ? List.of() : pageData.getData();
        bids.setAll(data); // Nạp mảng dữ liệu trang mới vào TableView

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
            // Phòng vệ tuyệt đối: Tránh số âm và số 0 gây crash chia cho 0 bên Server
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
        // Vô hiệu hóa và tự động thay đổi độ mờ (Opacity) trực quan cho nút lùi (◀) chuẩn phong cách SIM
        if (previousPageButton != null) {
            boolean disablePrev = busy || currentPage <= 1;
            previousPageButton.setDisable(disablePrev);
            previousPageButton.setStyle(disablePrev ? "-fx-opacity: 0.35;" : "-fx-opacity: 1.0; -fx-cursor: hand;");
        }

        // Vô hiệu hóa và tự động thay đổi độ mờ (Opacity) trực quan cho nút tiến (▶) chuẩn phong cách SIM
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

        // Tạo một khung VBox nhỏ để căn chỉnh chữ nằm chính giữa bảng gọn gàng
        VBox emptyBox = new VBox(10);
        emptyBox.setStyle("-fx-alignment: center; -fx-padding: 40;");

        Label iconLabel = new Label();
        Label msgLabel = new Label();

        if (isDarkMode) {
            // --- CHẾ ĐỘ TỐI (DARK MODE) ---
            iconLabel.setText("🚫🔒"); // Biểu tượng đặc vụ / bảo mật
            iconLabel.setStyle("-fx-font-size: 36px; -fx-opacity: 0.8;");

            msgLabel.setText("Thông tin mật vẫn chưa được lộ ra...");
            // Sử dụng màu vàng hổ phách/cam nhạt (#E5B869) đồng bộ với màu Header cột tối của bạn
            msgLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-font-style: italic; -fx-text-fill: #E5B869;");
        } else {
            // --- CHẾ ĐỘ SÁNG (LIGHT MODE) ---
            iconLabel.setText("📋✨"); // Biểu tượng lịch sử sạch sẽ
            iconLabel.setStyle("-fx-font-size: 32px; -fx-opacity: 0.6;");

            String cleanMessage = "Chưa có dữ liệu lịch sử đặt giá nào được ghi nhận.";
            msgLabel.setText(cleanMessage);
            // Sử dụng màu xám tối thanh lịch (#4B5563) phối hợp hài hòa trên nền bảng trắng
            msgLabel.setStyle("-fx-font-size: 13.5px; -fx-font-style: italic; -fx-text-fill: #4B5563;");
        }

        emptyBox.getChildren().addAll(iconLabel, msgLabel);

        // Đẩy toàn bộ cụm giao diện trống này vào TableView
        myBidsTable.setPlaceholder(emptyBox);
    }
}