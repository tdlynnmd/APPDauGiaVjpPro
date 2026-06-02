package com.auction.controller;

import com.auction.dto.AuctionDetailDTO;
import com.auction.dto.BidTransactionDTO;
import com.auction.dto.SocketResponse;
import com.auction.dto.UserDTO;
import com.auction.enums.UserRole;
import com.auction.network.ClientAuctionApi;
import com.auction.util.ClientSession;
import com.auction.util.SceneNavigator;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AuctionDetailController {
    private final ClientAuctionApi auctionApi = new ClientAuctionApi();
    private String auctionId;
    private AuctionDetailDTO currentAuctionDetail;
    private final ObservableList<BidTransactionDTO> bidHistoryItems = FXCollections.observableArrayList();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private Parent rootContainer; // Khai báo để nhận diện applyTheme sau này nếu cần
    @FXML private Label itemNameLabel;
    @FXML private Label sellerLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label stepPriceLabel;
    @FXML private Label statusLabel;
    @FXML private Label endTimeLabel;
    @FXML private Label messageLabel;
    @FXML private TextArea descriptionTextArea;
    @FXML private ImageView itemImageView;
    @FXML private TextField bidAmountField;

    @FXML private TableView<BidTransactionDTO> bidHistoryTable;
    @FXML private TableColumn<BidTransactionDTO, String> bidderNameColumn;
    @FXML private TableColumn<BidTransactionDTO, Number> bidAmountColumn; // Kiểu Number
    @FXML private TableColumn<BidTransactionDTO, String> bidTimeColumn;
    @FXML private TableColumn<BidTransactionDTO, String> bidStatusColumn;

    @FXML private Button openLiveBiddingButton;
    @FXML private Button placeBidButton; // Nên thêm FXID này vào FXML để khóa khi đang gửi lệnh
    @FXML private Button refreshButton;

    @FXML
    public void initialize() {
        applyTheme();
        setupBidHistoryTable();
        applyLiveBiddingAccess();
        showMessage("Chưa chọn phiên đấu giá.");
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
            System.out.println("Khong the nap theme cho Auction Detail: " + cssPath);
        }
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
        applyLiveBiddingAccess();
        loadAuctionDetail();
    }

    /**
     * SỬA: Đổi sang dùng SimpleObjectProperty kết hợp với getAmount()
     * để tránh lỗi ép kiểu hiển thị lớp Number của TableColumn trong JavaFX.
     */
    private void setupBidHistoryTable() {
        if (bidHistoryTable == null) return;

        bidderNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(safeText(cellData.getValue().getBidderName()))
        );

        // Đã sửa đổi an toàn cho kiểu dữ liệu Number
        bidAmountColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getAmount())
        );

        bidTimeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatDateTime(cellData.getValue().getTime()))
        );

        bidStatusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(safeText(cellData.getValue().getStatus()))
        );

        bidHistoryTable.setItems(bidHistoryItems);
    }

    /**
     * TỐI ƯU: Đưa luồng tải chi tiết đấu giá chạy ngầm (Background Task)
     * Giúp UI luôn mượt mà khi gọi Socket tương tác mạng.
     */
    private void loadAuctionDetail() {
        if (isBlank(auctionId)) {
            showError("Không tìm thấy auctionId của phiên đấu giá.");
            return;
        }

        setBusy(true);
        showMessage("Đang tải chi tiết phiên đấu giá...");

        Task<SocketResponse> task = new Task<>() {
            @Override
            protected SocketResponse call() {
                return auctionApi.getAuctionDetail(auctionId);
            }
        };

        task.setOnSucceeded(event -> {
            SocketResponse response = task.getValue();
            setBusy(false);

            if (response == null) {
                showError("Server không trả về phản hồi hợp lệ.");
                return;
            }
            if (!response.isSuccess()) {
                showError(response.getMessage());
                return;
            }

            AuctionDetailDTO detail = auctionApi.parseAuctionDetail(response);
            if (detail == null) {
                showError("Không thể đọc dữ liệu chi tiết phiên đấu giá.");
                return;
            }

            currentAuctionDetail = detail;
            displayAuctionDetail(detail);
            applyLiveBiddingAccess(); // Đồng bộ lại quyền truy cập live room dựa theo dữ liệu mới
            showMessage("Đã tải chi tiết phiên đấu giá.");
        });

        task.setOnFailed(event -> {
            setBusy(false);
            Throwable error = task.getException();
            showError(error == null ? "Không thể kết nối đến server." : error.getMessage());
        });

        Thread worker = new Thread(task, "load-auction-detail-thread");
        worker.setDaemon(true);
        worker.start();
    }

    private void displayAuctionDetail(AuctionDetailDTO detail) {
        setLabelText(itemNameLabel, detail.getItemName());
        setLabelText(sellerLabel, "Người bán: " + safeText(detail.getSellerUsername()));
        setLabelText(currentPriceLabel, "Giá hiện tại: " + formatMoney(detail.getCurrentPrice()) + " VNĐ");
        setLabelText(stepPriceLabel, "Bước giá: " + formatMoney(detail.getStepPrice()) + " VNĐ");
        setLabelText(statusLabel, "Trạng thái: " + safeText(detail.getStatus()));
        setLabelText(endTimeLabel, "Kết thúc: " + formatDateTime(detail.getEndTime()));

        if (descriptionTextArea != null) {
            descriptionTextArea.setText(safeText(detail.getItemDescription()));
        }

        loadItemImage(detail.getImageUrl());
        loadBidHistory(detail.getBidHistory());
    }

    private void loadItemImage(String imageUrl) {
        if (itemImageView == null) return;
        if (isBlank(imageUrl)) {
            itemImageView.setImage(null);
            return;
        }
        try {
            Image image = new Image(imageUrl, true);
            itemImageView.setImage(image);
        } catch (Exception e) {
            itemImageView.setImage(null);
            showMessage("Không thể tải ảnh vật phẩm.");
        }
    }

    private void loadBidHistory(List<BidTransactionDTO> bidHistory) {
        if (bidHistory == null) {
            bidHistory = Collections.emptyList();
        }
        bidHistoryItems.setAll(bidHistory);
    }

    /**
     * TỐI ƯU: Đưa luồng Đặt giá (Place Bid) chạy trên Background Task ngầm.
     * Khóa form nhập/nút bấm khi đang xử lý để ngăn chặn việc người dùng double-click gửi nhiều yêu cầu trùng lặp.
     */
    @FXML
    private void handlePlaceBid() {
        if (isBlank(auctionId)) {
            showError("Không tìm thấy phiên đấu giá để đặt giá.");
            return;
        }

        Double amount = readBidAmount();
        if (amount == null) return;

        if (amount <= 0) {
            showError("Số tiền đặt giá phải lớn hơn 0.");
            return;
        }

        if (currentAuctionDetail != null) {
            double minimumAmount = currentAuctionDetail.getCurrentPrice() + currentAuctionDetail.getStepPrice();
            if (amount < minimumAmount) {
                showError("Giá đặt tối thiểu là " + formatMoney(minimumAmount) + " VNĐ.");
                return;
            }
        }

        setBusy(true);
        showMessage("Đang gửi yêu cầu đặt giá lên hệ thống...");

        Task<SocketResponse> task = new Task<>() {
            @Override
            protected SocketResponse call() {
                return auctionApi.placeBid(auctionId, amount);
            }
        };

        task.setOnSucceeded(event -> {
            SocketResponse response = task.getValue();
            // Không tắt setBusy ở đây vì ngay sau đó ta gọi loadAuctionDetail() để kéo dữ liệu mới

            if (response == null) {
                setBusy(false);
                showError("Server không trả về phản hồi hợp lệ.");
                return;
            }
            if (!response.isSuccess()) {
                setBusy(false);
                showError(response.getMessage());
                return;
            }

            if (bidAmountField != null) {
                bidAmountField.clear();
            }

            showInfo(response.getMessage() == null ? "Đặt giá thành công!" : response.getMessage());

            // Tải lại chi tiết phiên đấu giá để cập nhật bảng lịch sử và giá mới
            loadAuctionDetail();
        });

        task.setOnFailed(event -> {
            setBusy(false);
            Throwable error = task.getException();
            showError(error == null ? "Đặt giá thất bại, không thể kết nối mạng." : error.getMessage());
        });

        Thread worker = new Thread(task, "place-bid-thread");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void handleOpenLiveBidding() {
        if (!isCurrentUserBidder()) {
            showError("Chỉ tài khoản Bidder mới được vào phòng live bidding.");
            return;
        }
        if (isBlank(auctionId)) {
            showError("Không tìm thấy phiên đấu giá để vào phòng live bidding.");
            return;
        }
        SceneNavigator.showLiveBidding(auctionId);
    }

    private void applyLiveBiddingAccess() {
        if (openLiveBiddingButton == null) return;
        boolean bidder = isCurrentUserBidder();
        openLiveBiddingButton.setVisible(bidder);
        openLiveBiddingButton.setManaged(bidder);
    }

    private boolean isCurrentUserBidder() {
        if (!ClientSession.isLoggedIn()) return false;
        UserDTO user = ClientSession.getCurrentUser();
        return user != null && user.getRole() == UserRole.BIDDER;
    }

    private Double readBidAmount() {
        if (bidAmountField == null || isBlank(bidAmountField.getText())) {
            showError("Vui lòng nhập số tiền muốn đặt.");
            return null;
        }
        try {
            // Loại bỏ khoảng trắng và chuẩn hóa định dạng số nhập vào trước khi parse
            String rawAmount = bidAmountField.getText().trim()
                    .replace(" ", "")
                    .replace(",", "");
            return Double.parseDouble(rawAmount);
        } catch (NumberFormatException e) {
            showError("Số tiền đặt giá không hợp lệ. Vui lòng chỉ nhập số ký tự liền nhau.");
            return null;
        }
    }

    @FXML
    private void handleRefresh() {
        loadAuctionDetail();
    }

    @FXML
    private void handleBack() {
        SceneNavigator.showAuctionList();
    }

    /**
     * Hàm tiện ích điều khiển trạng thái vô hiệu hóa của các nút điều hướng
     * để tránh xung đột dữ liệu khi ứng dụng đang thực thi lệnh Socket ngầm.
     */
    private void setBusy(boolean busy) {
        Platform.runLater(() -> {
            if (bidAmountField != null) bidAmountField.setDisable(busy);
            if (placeBidButton != null) placeBidButton.setDisable(busy);
            if (refreshButton != null) refreshButton.setDisable(busy);
            if (openLiveBiddingButton != null) openLiveBiddingButton.setDisable(busy);
        });
    }

    private void setLabelText(Label label, String text) {
        if (label != null) {
            label.setText(safeText(text));
        }
    }

    private String formatMoney(double value) {
        return String.format("%,.0f", value);
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) return "";
        return value.format(dateTimeFormatter);
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void showMessage(String message) {
        if (messageLabel != null) {
            messageLabel.setText(safeText(message));
        }
    }

    private void showError(String message) {
        showMessage(message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi hệ thống");
        alert.setHeaderText(null);
        alert.setContentText(safeText(message));
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(safeText(message));
        alert.showAndWait();
    }
}