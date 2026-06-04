package com.auction.controller;

import com.auction.dto.AuctionSummaryDTO;
import com.auction.dto.SocketResponse;
import com.auction.network.ClientAuctionApi;
import com.auction.util.ClientSession;
import com.auction.util.SceneNavigator;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Bộ điều khiển (Controller) hoặc lớp tiện ích SellerAuctionController xử lý giao diện Client JavaFX.
 */
public class SellerAuctionController {
    private final ClientAuctionApi auctionApi = new ClientAuctionApi();

    private final ObservableList<AuctionSummaryDTO> sellerAuctions = FXCollections.observableArrayList();
    private FilteredList<AuctionSummaryDTO> filteredDataList;

    private AuctionSummaryDTO selectedAuction;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int currentPageIndex = 1;
    private int rowsPerPage = 10;

    @FXML private Parent rootContainer;
    @FXML private Label messageLabel;

    @FXML private Label headerAvailableBalance;
    @FXML private Label headerFrozenBalance;
    @FXML private Label headerTotalBalance;

    @FXML private TableView<AuctionSummaryDTO> sellerAuctionsTable;
    @FXML private TableColumn<AuctionSummaryDTO, String> auctionIdColumn;
    @FXML private TableColumn<AuctionSummaryDTO, String> auctionItemNameColumn;
    @FXML private TableColumn<AuctionSummaryDTO, String> auctionStatusColumn;
    @FXML private TableColumn<AuctionSummaryDTO, String> auctionStartTimeColumn;
    @FXML private TableColumn<AuctionSummaryDTO, String> auctionEndTimeColumn;
    @FXML private TableColumn<AuctionSummaryDTO, Number> auctionStepPriceColumn;

    @FXML private TextField auctionStepPriceField;
    @FXML private TextField auctionStartTimeField;
    @FXML private TextField auctionEndTimeField;

    @FXML private Button refreshButton;
    @FXML private Button updateAuctionButton;
    @FXML private Button backButton;

    @FXML private TextField searchField;
    @FXML private TextField pageSizeField;
    @FXML private Label pageLabel;
    @FXML private Button previousPageButton;
    @FXML private Button nextPageButton;

    @FXML private Button editAuctionButton;
    @FXML private ScrollPane rightSplitPaneContainer;
    @FXML private Button clearFilterButton;
    @FXML private Button searchButton;

    @FXML private Label placeholderIcon;
    @FXML private Label placeholderText;

    @FXML
    private void handleSearch() {
        handleLoadSellerAuctions();
    }

    @FXML
    public void initialize() {
        com.auction.util.HeaderBalanceHelper.setupHeaderBalance(headerAvailableBalance, headerFrozenBalance, headerTotalBalance);
        applyTheme();

        if (!ClientSession.isLoggedIn()) {
            SceneNavigator.showLogin();
            return;
        }

        initializeSellerAuctionsTable();
        setupSearchAndPaginationLogic();
        setupTablePlaceholderByTheme(SceneNavigator.isAppDarkMode);
        handleLoadSellerAuctions();
    }

    private void initializeSellerAuctionsTable() {
        auctionIdColumn.setCellValueFactory(data ->
                new SimpleStringProperty(safeText(data.getValue().getAuctionId())));
        auctionItemNameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(safeText(data.getValue().getItemName())));

        auctionStatusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(safeText(data.getValue().getStatus())));

        auctionStatusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    String status = item.toUpperCase().trim();
                    if ("OPEN".equals(status)) {
                        setStyle("-fx-text-fill: #3498db !important; -fx-font-weight: bold; -fx-font-size: 16px;");
                    } else if ("RUNNING".equals(status) || "ACTIVE".equals(status)) {
                        setStyle("-fx-text-fill: #2ecc71 !important; -fx-font-weight: bold; -fx-font-size: 16px;");
                    } else if ("FINISHED".equals(status) || "ENDED".equals(status)) {
                        setStyle("-fx-text-fill: #e74c3c !important; -fx-font-weight: bold; -fx-font-size: 16px;");
                    } else {
                        setStyle("-fx-text-fill: #95a5a6 !important; -fx-font-weight: bold;");
                    }
                }
            }
        });

        auctionStartTimeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(formatTime(data.getValue().getStartTime())));
        auctionEndTimeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(formatTime(data.getValue().getEndTime())));

        auctionStepPriceColumn.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getStepPrice()));
        auctionStepPriceColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.0f VNĐ", item.doubleValue()));
                }
            }
        });

        sellerAuctionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) return;

            if (rightSplitPaneContainer.isVisible()) {

                String newStatus = safeText(newValue.getStatus()).toUpperCase().trim();
                if (!"OPEN".equals(newStatus)) {
                    showError("Không thể chọn phiên này! Chỉ được phép xem/sửa các phiên đấu giá ở trạng thái OPEN.\nPhiên vừa chọn đang ở trạng thái: " + newStatus);

                    javafx.application.Platform.runLater(() -> {
                        sellerAuctionsTable.getSelectionModel().selectedItemProperty().removeListener((javafx.beans.value.ChangeListener)obs);
                        if (oldValue != null) {
                            sellerAuctionsTable.getSelectionModel().select(oldValue);
                        } else {
                            sellerAuctionsTable.getSelectionModel().clearSelection();
                        }
                        sellerAuctionsTable.getSelectionModel().selectedItemProperty().addListener((javafx.beans.value.ChangeListener)obs);
                    });
                    return;
                }

                if (isFormChangedFromDefault()) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Xác nhận chuyển đổi phiên");
                    alert.setHeaderText(null);
                    alert.setContentText("Dữ liệu của phiên hiện tại đã bị thay đổi và chưa được lưu.\nBạn có chắc chắn muốn HỦY các thay đổi này và chuyển sang nạp dữ liệu của phiên mới chọn không?");

                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        selectedAuction = newValue;
                        fillAuctionUpdateForm(selectedAuction);
                    } else {
                        javafx.application.Platform.runLater(() -> {
                            sellerAuctionsTable.getSelectionModel().selectedItemProperty().removeListener((javafx.beans.value.ChangeListener)obs);
                            sellerAuctionsTable.getSelectionModel().select(oldValue);
                            sellerAuctionsTable.getSelectionModel().selectedItemProperty().addListener((javafx.beans.value.ChangeListener)obs);
                        });
                    }
                } else {
                    selectedAuction = newValue;
                    fillAuctionUpdateForm(selectedAuction);
                }
            } else {
                selectedAuction = newValue;
            }
        });
    }

    /**
     * Tự động thay đổi nội dung thông báo bảng trống tương ứng theo từng giao diện
     * @param isDarkMode true nếu là Dark Mode, false nếu là Light Mode
     */
    private void setupTablePlaceholderByTheme(boolean isDarkMode) {
        if (isDarkMode) {
            if (placeholderIcon != null) placeholderIcon.setText("🌌");
            if (placeholderText != null) placeholderText.setText("Không tìm thấy dữ liệu phiên trong bóng đêm");
        } else {
            if (placeholderIcon != null) placeholderIcon.setText("☀️");
            if (placeholderText != null) placeholderText.setText("Danh sách phiên đấu giá hiện đang trống");
        }
    }

    /**
     * Chức năng bổ sung: Xử lý sự kiện khi click nút "📝 Sửa Phiên Đấu Giá" ở Top Bar
     * Thực hiện kiểm tra 3 trường hợp nghiệp vụ theo yêu cầu.
     */
    @FXML
    private void handleOpenEditForm() {
        if (selectedAuction == null) {
            showError("Vui lòng chọn một phiên đấu giá trên bảng trước khi sửa.");
            return;
        }

        String status = safeText(selectedAuction.getStatus()).toUpperCase();
        if (!"OPEN".equals(status)) {
            showError("Phiên đấu giá không hợp lệ! Chỉ cho phép sửa đổi phiên ở trạng thái OPEN.\nPhiên hiện tại đang là: " + status);
            return;
        }

        rightSplitPaneContainer.setVisible(true);
        rightSplitPaneContainer.setManaged(true);

        fillAuctionUpdateForm(selectedAuction);
    }

    /**
     * Chức năng bổ sung: Xử lý nút (X) ở góc phải trên của Form
     * Kiểm tra xem dữ liệu trên form có khác dữ liệu chuẩn ban đầu hay không để đưa ra cảnh báo thoát.
     */
    @FXML
    private void handleCloseEditForm() {
        if (isFormChangedFromDefault()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Xác nhận thoát");
            alert.setHeaderText(null);
            alert.setContentText("Dữ liệu đã có sự thay đổi và chưa được lưu. Bạn có chắc chắn muốn đóng form và hủy các thay đổi này?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() != ButtonType.OK) {
                return;
            }
        }

        rightSplitPaneContainer.setVisible(false);
        rightSplitPaneContainer.setManaged(false);
    }

    /**
     * Chức năng bổ sung: Xử lý nút "XOÁ FORM"
     * Chỉ khôi phục dữ liệu chuẩn ban đầu, cảnh báo nếu dữ liệu hiện hành đã bị sửa đổi
     */
    @FXML
    private void handleResetToDefaultData() {
        if (selectedAuction == null) return;

        if (isFormChangedFromDefault()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Xác nhận xóa form");
            alert.setHeaderText(null);
            alert.setContentText("Bạn có chắc chắn muốn hủy bỏ toàn bộ các thay đổi hiện tại và khôi phục lại dữ liệu gốc ban đầu?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() != ButtonType.OK) {
                return;
            }
        }

        fillAuctionUpdateForm(selectedAuction);
    }

    /**
     * Chức năng bổ sung: Xử lý xóa nhanh bộ lọc tìm kiếm đưa danh sách về mặc định
     */
    @FXML
    private void handleClearFilter() {
        if (searchField != null) {
            searchField.clear();
        }
    }

    /**
     * Chức năng bổ sung: Kiểm tra xem các trường nhập liệu trên form có bị thay đổi so với dữ liệu gốc ban đầu hay không
     */
    private boolean isFormChangedFromDefault() {
        if (selectedAuction == null) return false;

        String defaultStepPrice = String.format("%.0f", selectedAuction.getStepPrice());
        String defaultStartTime = formatTime(selectedAuction.getStartTime());
        String defaultEndTime = formatTime(selectedAuction.getEndTime());

        String currentStepPrice = auctionStepPriceField.getText() == null ? "" : auctionStepPriceField.getText().trim();
        String currentStartTime = auctionStartTimeField.getText() == null ? "" : auctionStartTimeField.getText().trim();
        String currentEndTime = auctionEndTimeField.getText() == null ? "" : auctionEndTimeField.getText().trim();

        return !defaultStepPrice.equals(currentStepPrice)
                || !defaultStartTime.equals(currentStartTime)
                || !defaultEndTime.equals(currentEndTime);
    }

    /**
     * Chức năng bổ sung: Ràng buộc bộ lọc tìm kiếm Real-time theo tên vật phẩm
     * và tính toán số hàng hiển thị trên mỗi trang khi người dùng thay đổi.
     */
    private void setupSearchAndPaginationLogic() {
        filteredDataList = new FilteredList<>(sellerAuctions, p -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredDataList.setPredicate(auction -> {
                if (newValue == null || newValue.trim().isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                if (auction.getItemName() != null && auction.getItemName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });
            currentPageIndex = 1;
            renderPaginatedTable();
        });

        pageSizeField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.trim().isEmpty()) {
                try {
                    int size = Integer.parseInt(newValue.trim());
                    if (size > 0) {
                        rowsPerPage = size;
                        currentPageIndex = 1;
                        renderPaginatedTable();
                    }
                } catch (NumberFormatException e) {
                }
            }
        });
    }

    /**
     * Chức năng bổ sung: Cắt lớp mảng dữ liệu (Sublist) dựa trên trang hiện hành
     * để hiển thị lên bảng TableView kèm nhãn trạng thái Trang X / Y.
     */
    private void renderPaginatedTable() {
        int totalItems = filteredDataList.size();
        int maxPageIdx = (int) Math.ceil((double) totalItems / rowsPerPage);
        if (maxPageIdx == 0) maxPageIdx = 1;

        if (currentPageIndex > maxPageIdx) currentPageIndex = maxPageIdx;
        if (currentPageIndex < 1) currentPageIndex = 1;

        int fromIndex = (currentPageIndex - 1) * rowsPerPage;
        int toIndex = Math.min(fromIndex + rowsPerPage, totalItems);

        List<AuctionSummaryDTO> pageSubList = new ArrayList<>();
        if (fromIndex < totalItems) {
            pageSubList = filteredDataList.subList(fromIndex, toIndex);
        }

        ObservableList<AuctionSummaryDTO> pageObservableList = FXCollections.observableArrayList(pageSubList);
        SortedList<AuctionSummaryDTO> sortedData = new SortedList<>(pageObservableList);
        sortedData.comparatorProperty().bind(sellerAuctionsTable.comparatorProperty());

        sellerAuctionsTable.setItems(sortedData);

        pageLabel.setText(String.format("Trang %d / %d", currentPageIndex, maxPageIdx));
        previousPageButton.setDisable(currentPageIndex == 1);
        nextPageButton.setDisable(currentPageIndex == maxPageIdx);
    }

    /**
     * Chức năng bổ sung: Xử lý khi nhấn nút lùi về Trang trước
     */
    @FXML
    private void handlePreviousPage() {
        if (currentPageIndex > 1) {
            currentPageIndex--;
            renderPaginatedTable();
        }
    }

    /**
     * Chức năng bổ sung: Xử lý khi nhấn nút tiến tới Trang sau
     */
    @FXML
    private void handleNextPage() {
        int totalItems = filteredDataList.size();
        int maxPageIdx = (int) Math.ceil((double) totalItems / rowsPerPage);
        if (currentPageIndex < maxPageIdx) {
            currentPageIndex++;
            renderPaginatedTable();
        }
    }

    @FXML
    private void handleLoadSellerAuctions() {
        runRequest(
                "Đang tải danh sách phiên đấu giá...",
                auctionApi::getSellerAuctions,
                this::handleLoadSellerAuctionsResponse
        );
    }

    private void handleLoadSellerAuctionsResponse(SocketResponse response) {
        if (!isSuccessful(response)) {
            showError(response == null ? "Server không trả về phản hồi hợp lệ." : response.getMessage());
            return;
        }

        sellerAuctions.setAll(auctionApi.parseAuctionSummaryList(response));
        currentPageIndex = 1;
        renderPaginatedTable();
        showMessage("Đã đồng bộ danh sách phiên đấu giá.");
    }

    private void fillAuctionUpdateForm(AuctionSummaryDTO auction) {
        if (auction == null) {
            clearUpdateForm();
            return;
        }

        String status = safeText(auction.getStatus()).toUpperCase();
        boolean isEditable = status.equals("OPEN");

        auctionStepPriceField.setText(String.format("%.0f", auction.getStepPrice()));
        auctionStartTimeField.setText(formatTime(auction.getStartTime()));
        auctionEndTimeField.setText(formatTime(auction.getEndTime()));

        auctionStepPriceField.setDisable(!isEditable);
        auctionStartTimeField.setDisable(!isEditable);
        auctionEndTimeField.setDisable(!isEditable);
        updateAuctionButton.setDisable(!isEditable);

        if (!isEditable) {
            showMessage("Phiên đấu giá đang chạy hoặc đã kết thúc, không thể chỉnh sửa.");
        }
    }

    @FXML
    private void handleUpdateAuction() {
        if (selectedAuction == null) {
            showError("Vui lòng chọn một phiên đấu giá cần cập nhật.");
            return;
        }

        Double stepPrice = readStepPrice();
        if (stepPrice == null) return;

        LocalDateTime startTime = readDateTime(auctionStartTimeField, "thời gian bắt đầu");
        if (startTime == null) return;

        LocalDateTime endTime = readDateTime(auctionEndTimeField, "thời gian kết thúc");
        if (endTime == null) return;

        if (!endTime.isAfter(startTime)) {
            showError("Thời gian kết thúc phải sau thời gian bắt đầu.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận thay đổi dữ liệu");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Bạn có chắc chắn muốn lưu lại các thay đổi cấu hình này lên hệ thống mạng hay không?");

        Optional<ButtonType> clickResult = confirmAlert.showAndWait();
        if (clickResult.isPresent() && clickResult.get() != ButtonType.OK) {
            return;
        }

        runRequest(
                "Đang cập nhật phiên đấu giá...",
                () -> auctionApi.updateAuction(selectedAuction.getAuctionId(), stepPrice, startTime, endTime),
                this::handleUpdateAuctionResponse
        );
    }

    private void handleUpdateAuctionResponse(SocketResponse response) {
        if (!isSuccessful(response)) {
            showError(response == null ? "Cập nhật phiên thất bại." : response.getMessage());
            return;
        }

        showInfo(response.getMessage() == null ? "Cập nhật phiên đấu giá thành công." : response.getMessage());

        rightSplitPaneContainer.setVisible(false);
        rightSplitPaneContainer.setManaged(false);

        handleLoadSellerAuctions();
    }

    @FXML
    private void handleClearUpdateForm() {
        selectedAuction = null;
        if (sellerAuctionsTable != null) {
            sellerAuctionsTable.getSelectionModel().clearSelection();
        }
        clearUpdateForm();

        if (auctionStepPriceField != null) auctionStepPriceField.setDisable(false);
        if (auctionStartTimeField != null) auctionStartTimeField.setDisable(false);
        if (auctionEndTimeField != null) auctionEndTimeField.setDisable(false);
        if (updateAuctionButton != null) updateAuctionButton.setDisable(false);
    }

    @FXML
    private void handleBack() {
        SceneNavigator.showDashboard();
    }

    private void runRequest(String loadingMessage, Supplier<SocketResponse> request, Consumer<SocketResponse> onSuccess) {
        setBusy(true);
        showMessage(loadingMessage);

        Task<SocketResponse> task = new Task<>() {
            @Override
            protected SocketResponse call() {
                return request.get();
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);
            onSuccess.accept(task.getValue());
        });

        task.setOnFailed(event -> {
            setBusy(false);
            showError("Không thể kết nối hoặc xử lý phản hồi server.");
        });

        Thread worker = new Thread(task, "seller-auction-request-thread");
        worker.setDaemon(true);
        worker.start();
    }

    private Double readStepPrice() {
        String raw = auctionStepPriceField == null ? "" : auctionStepPriceField.getText().trim();

        if (raw.isEmpty()) {
            showError("Vui lòng nhập bước giá.");
            return null;
        }

        try {
            double value = Double.parseDouble(raw);
            if (value <= 0) {
                showError("Bước giá phải lớn hơn 0.");
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            showError("Bước giá không hợp lệ.");
            return null;
        }
    }

    private LocalDateTime readDateTime(TextField field, String fieldName) {
        String raw = field == null ? "" : field.getText().trim();

        if (raw.isEmpty()) {
            showError("Vui lòng nhập " + fieldName + ".");
            return null;
        }

        try {
            return LocalDateTime.parse(raw, dateTimeFormatter);
        } catch (DateTimeParseException e) {
            showError(fieldName + " không đúng định dạng ngày/tháng/năm giờ:phút. Ví dụ: 20/05/2026 10:30");
            return null;
        }
    }

    private void applyTheme() {
        if (rootContainer == null) return;

        rootContainer.getStylesheets().clear();
        String cssPath = SceneNavigator.isAppDarkMode
                ? "/com/auction/client/view/dark.css"
                : "/com/auction/client/view/light.css";

        try {
            rootContainer.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource(cssPath)).toExternalForm()
            );
        } catch (Exception e) {
            System.out.println("Không thể nạp theme cho SellerAuction: " + cssPath);
        }
    }

    private void clearUpdateForm() {
        if (auctionStepPriceField != null) auctionStepPriceField.clear();
        if (auctionStartTimeField != null) auctionStartTimeField.clear();
        if (auctionEndTimeField != null) auctionEndTimeField.clear();
    }

    private void setBusy(boolean busy) {
        setDisabled(refreshButton, busy);
        setDisabled(previousPageButton, busy);
        setDisabled(nextPageButton, busy);
        setDisabled(searchField, busy);
        setDisabled(pageSizeField, busy);
        setDisabled(editAuctionButton, busy);
        setDisabled(clearFilterButton, busy);
        if (busy) {
            setDisabled(updateAuctionButton, true);
            setDisabled(auctionStepPriceField, true);
            setDisabled(auctionStartTimeField, true);
            setDisabled(auctionEndTimeField, true);
        } else if (selectedAuction != null) {
            fillAuctionUpdateForm(selectedAuction);
        }
        setDisabled(backButton, busy);
        setDisabled(sellerAuctionsTable, busy);
    }

    private void setDisabled(Control control, boolean disabled) {
        if (control != null) control.setDisable(disabled);
    }

    private boolean isSuccessful(SocketResponse response) {
        return response != null && response.isSuccess();
    }

    /**
     * Định dạng thời gian ra chuỗi dd/MM/yyyy HH:mm để người dùng nhìn trực quan trên bảng
     */
    private String formatTime(LocalDateTime time) {
        if (time == null) return "";
        try {
            return time.format(dateTimeFormatter);
        } catch (Exception e) {
            return time.toString();
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private void showMessage(String message) {
        if (messageLabel != null) messageLabel.setText(safeText(message));
    }

    private void showError(String message) {
        showMessage(message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi cấu hình");
        alert.setHeaderText(null);
        alert.setContentText(safeText(message));
        alert.showAndWait();
    }

    private void showInfo(String message) {
        showMessage(message);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(safeText(message));
        alert.showAndWait();
    }
}