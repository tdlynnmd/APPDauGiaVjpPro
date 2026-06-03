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
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SellerAuctionController {
    private final ClientAuctionApi auctionApi = new ClientAuctionApi();
    private final ObservableList<AuctionSummaryDTO> sellerAuctions = FXCollections.observableArrayList();

    private AuctionSummaryDTO selectedAuction;

    // Khai báo formatter chuẩn để hiển thị và đọc dữ liệu thân thiện với người dùng
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Parent rootContainer;
    @FXML private Label messageLabel;

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

    // Vai trò: khởi tạo màn quản lý auction của seller.
    @FXML
    public void initialize() {
        applyTheme();

        if (!ClientSession.isLoggedIn()) {
            SceneNavigator.showLogin();
            return;
        }

        initializeSellerAuctionsTable();
        handleLoadSellerAuctions();
    }

    // Chức năng: cấu hình bảng danh sách auction.
    private void initializeSellerAuctionsTable() {
        sellerAuctionsTable.setItems(sellerAuctions);

        auctionIdColumn.setCellValueFactory(data ->
                new SimpleStringProperty(safeText(data.getValue().getAuctionId())));
        auctionItemNameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(safeText(data.getValue().getItemName())));

        auctionStatusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(safeText(data.getValue().getStatus())));
        // Tô màu trạng thái phiên cho sinh động (ACTIVE = Xanh lá, kết thúc = Đỏ, chờ = Cam)
        auctionStatusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("ACTIVE".equalsIgnoreCase(item) || "OPEN".equalsIgnoreCase(item)) {
                        setTextFill(javafx.scene.paint.Color.valueOf("#2ecc71"));
                    } else if ("FINISHED".equalsIgnoreCase(item) || "ENDED".equalsIgnoreCase(item) || "CANCELED".equalsIgnoreCase(item)) {
                        setTextFill(javafx.scene.paint.Color.valueOf("#e74c3c"));
                    } else {
                        setTextFill(javafx.scene.paint.Color.valueOf("#ff9f43"));
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
        // Định dạng cột bước giá hiển thị dấu phân cách hàng nghìn trực quan
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
            selectedAuction = newValue;
            fillAuctionUpdateForm(newValue);
        });
    }

    // Chức năng: tải danh sách auction do seller tạo.
    @FXML
    private void handleLoadSellerAuctions() {
        runRequest(
                "Đang tải danh sách phiên đấu giá...",
                auctionApi::getSellerAuctions,
                this::handleLoadSellerAuctionsResponse
        );
    }

    // Vai trò: xử lý phản hồi danh sách auction.
    private void handleLoadSellerAuctionsResponse(SocketResponse response) {
        if (!isSuccessful(response)) {
            showError(response == null ? "Server không trả về phản hồi hợp lệ." : response.getMessage());
            return;
        }

        sellerAuctions.setAll(auctionApi.parseAuctionSummaryList(response));
        showMessage("Đã đồng bộ danh sách phiên đấu giá.");
    }

    // Chức năng: đưa auction đang chọn vào form sửa.
    private void fillAuctionUpdateForm(AuctionSummaryDTO auction) {
        if (auction == null) {
            clearUpdateForm();
            return;
        }

        /*
         * KIỂM TRA BẢO VỆ UX: Chỉ cho phép sửa nếu phiên chưa bắt đầu (ví dụ trạng thái: CREATED, PENDING, UPCOMING)
         * Nếu phiên đang diễn ra hoặc đã kết thúc, ta khóa form chỉnh sửa lại để đảm bảo tính minh bạch.
         */
        String status = safeText(auction.getStatus()).toUpperCase();
        boolean isEditable = status.equals("CREATED") || status.equals("PENDING") || status.equals("UPCOMING") || status.isEmpty();

        auctionStepPriceField.setText(String.format("%.0f", auction.getStepPrice()));
        auctionStartTimeField.setText(formatTime(auction.getStartTime()));
        auctionEndTimeField.setText(formatTime(auction.getEndTime()));

        // Khóa hoặc mở khóa động các ô nhập liệu tùy theo trạng thái phiên
        auctionStepPriceField.setDisable(!isEditable);
        auctionStartTimeField.setDisable(!isEditable);
        auctionEndTimeField.setDisable(!isEditable);
        updateAuctionButton.setDisable(!isEditable);

        if (!isEditable) {
            showMessage("Phiên đấu giá đang chạy hoặc đã kết thúc, không thể chỉnh sửa.");
        }
    }

    // Chức năng: cập nhật auction đã chọn.
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

        runRequest(
                "Đang cập nhật phiên đấu giá...",
                () -> auctionApi.updateAuction(selectedAuction.getAuctionId(), stepPrice, startTime, endTime),
                this::handleUpdateAuctionResponse
        );
    }

    // Vai trò: xử lý phản hồi cập nhật auction.
    private void handleUpdateAuctionResponse(SocketResponse response) {
        if (!isSuccessful(response)) {
            showError(response == null ? "Cập nhật phiên thất bại." : response.getMessage());
            return;
        }

        showInfo(response.getMessage() == null ? "Cập nhật phiên đấu giá thành công." : response.getMessage());
        handleLoadSellerAuctions();
    }

    // Chức năng: xóa dữ liệu form cập nhật.
    @FXML
    private void handleClearUpdateForm() {
        selectedAuction = null;
        if (sellerAuctionsTable != null) {
            sellerAuctionsTable.getSelectionModel().clearSelection();
        }
        clearUpdateForm();

        // Mở khóa lại form khi reset khôi phục trạng thái ban đầu
        if (auctionStepPriceField != null) auctionStepPriceField.setDisable(false);
        if (auctionStartTimeField != null) auctionStartTimeField.setDisable(false);
        if (auctionEndTimeField != null) auctionEndTimeField.setDisable(false);
        if (updateAuctionButton != null) updateAuctionButton.setDisable(false);
    }

    // Vai trò: quay lại dashboard.
    @FXML
    private void handleBack() {
        SceneNavigator.showDashboard();
    }

    // Vai trò: chạy request socket ngoài UI thread.
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

    // Vai trò: đọc và kiểm tra bước giá.
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

    // Vai trò: đọc và kiểm tra thời gian ISO.
    private LocalDateTime readDateTime(TextField field, String fieldName) {
        String raw = field == null ? "" : field.getText().trim();

        if (raw.isEmpty()) {
            showError("Vui lòng nhập " + fieldName + ".");
            return null;
        }

        try {
            // Đọc định dạng ngày Việt Nam thân thiện thay vì ép định dạng chuỗi ISO thô cứng
            return LocalDateTime.parse(raw, dateTimeFormatter);
        } catch (DateTimeParseException e) {
            showError(fieldName + " không đúng định dạng ngày/tháng/năm giờ:phút. Ví dụ: 20/05/2026 10:30");
            return null;
        }
    }

    // Vai trò: áp dụng theme hiện tại.
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
        // Nếu đang bận thì nút update bị disable hoàn toàn, nếu rảnh thì phụ thuộc vào trạng thái phiên ở fillForm
        if (busy) {
            setDisabled(updateAuctionButton, true);
            setDisabled(auctionStepPriceField, true);
            setDisabled(auctionStartTimeField, true);
            setDisabled(auctionEndTimeField, true);
        } else if (selectedAuction != null) {
            // Khôi phục lại đúng trạng thái khóa/mở khóa dựa vào loại phiên khi kết thúc tiến trình chạy mạng
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
        alert.setTitle("Lỗi");
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