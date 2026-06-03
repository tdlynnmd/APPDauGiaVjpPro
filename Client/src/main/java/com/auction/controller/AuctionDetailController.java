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
import javafx.scene.control.TableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.TableCell;
import javafx.application.Platform;

import com.auction.service.ClientSocketService;
import com.auction.service.RealtimeUpdateListener;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * AuctionDetailController là Controller phía Client cho màn hình chi tiết phiên đấu giá.
 */
public class AuctionDetailController implements RealtimeUpdateListener {
    private final ClientAuctionApi auctionApi = new ClientAuctionApi();

    /*
     * auctionId là ID phiên đấu giá đang được xem.
     * Giá trị này không lấy trực tiếp từ FXML.
     * AuctionListController sẽ truyền sang bằng hàm setAuctionId().
     */
    private String auctionId;

    /*
     * Lưu lại dữ liệu chi tiết hiện tại.
     * Khi đặt giá, controller có thể dùng currentAuctionDetail để kiểm tra nhanh dữ liệu đang hiển thị.
     */
    private AuctionDetailDTO currentAuctionDetail;
    private boolean liveRoomJoined;
    private boolean listenerRegistered;

    /*
     * ObservableList là danh sách mà TableView theo dõi.
     * Khi bidHistoryItems thay đổi, bảng lịch sử bid sẽ cập nhật theo.
     */
    private final ObservableList<BidTransactionDTO> bidHistoryItems = FXCollections.observableArrayList();

    /*
     * Format thời gian để hiển thị lên giao diện cho dễ đọc.
     */
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    private Parent rootContainer; // Thêm container gốc để hỗ trợ load stylesheet động khi đổi theme

    /**
     * FXML cần có: <Label fx:id="itemNameLabel" ... />
     */
    @FXML
    private Label itemNameLabel;

    /**
     * FXML cần có: <Label fx:id="sellerLabel" ... />
     */
    @FXML
    private Label sellerLabel;

    /**
     * FXML cần có: <Label fx:id="currentPriceLabel" ... />
     */
    @FXML
    private Label currentPriceLabel;

    /**
     * FXML cần có: <Label fx:id="stepPriceLabel" ... />
     */
    @FXML
    private Label stepPriceLabel;

    /**
     * FXML cần có: <Label fx:id="statusLabel" ... />
     */
    @FXML
    private Label statusLabel;

    /**
     * FXML cần có: <Label fx:id="endTimeLabel" ... />
     */
    @FXML
    private Label endTimeLabel;

    /**
     * FXML cần có: <Label fx:id="messageLabel" ... />
     */
    @FXML
    private Label messageLabel;

    /**
     * FXML cần có: <TextArea fx:id="descriptionTextArea" ... />
     */
    @FXML
    private TextArea descriptionTextArea;

    /**
     * FXML cần có: <ImageView fx:id="itemImageView" ... />
     */
    @FXML
    private ImageView itemImageView;

    /**
     * FXML cần có: <TextField fx:id="bidAmountField" ... />
     */
    @FXML
    private TextField bidAmountField;

    /**
     * FXML cần có: <TableView fx:id="bidHistoryTable" ... />
     */
    @FXML
    private TableView<BidTransactionDTO> bidHistoryTable;

    /**
     * FXML cần có: <TableColumn fx:id="bidderNameColumn" ... />
     */
    @FXML
    private TableColumn<BidTransactionDTO, String> bidderNameColumn;

    /**
     * FXML cần có: <TableColumn fx:id="bidAmountColumn" ... />
     */
    @FXML
    private TableColumn<BidTransactionDTO, Number> bidAmountColumn;

    /**
     * FXML cần có: <TableColumn fx:id="bidTimeColumn" ... />
     */
    @FXML
    private TableColumn<BidTransactionDTO, String> bidTimeColumn;

    /**
     * FXML cần có: <TableColumn fx:id="bidStatusColumn" ... />
     */
    @FXML
    private TableColumn<BidTransactionDTO, String> bidStatusColumn;

    @FXML
    private Button openLiveBiddingButton;

    @FXML
    private Button placeBidButton; // ID điều khiển nút đặt giá khi hệ thống đang bận

    @FXML
    private Button refreshButton; // ID điều khiển nút làm mới khi hệ thống đang bận

    /**
     * initialize() được JavaFX tự động gọi sau khi load auction-detail.fxml.
     *
     * Lưu ý:
     * - Tại thời điểm initialize(), auctionId thường chưa được truyền sang.
     * - Vì vậy initialize() chỉ cấu hình bảng.
     * - Dữ liệu thật sẽ được load trong setAuctionId().
     */
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

    /**
     * Hàm để màn hình khác truyền auctionId vào AuctionDetailController.
     *
     * Luồng dự kiến:
     * - AuctionListController lấy selectedAuction.getAuctionId().
     * - Load auction-detail.fxml.
     * - Lấy controller.
     * - Gọi controller.setAuctionId(auctionId).
     *
     * Sau khi có auctionId, controller mới gọi Server để lấy chi tiết.
     */
    public void setAuctionId(String auctionId) {
        if (!isBlank(this.auctionId) && !this.auctionId.equals(auctionId)) {
            cleanupLiveRoom();
        }
        this.auctionId = auctionId != null ? auctionId.trim() : null;
        applyLiveBiddingAccess();
        registerRealtimeListener();
        loadAuctionDetail();
    }

    /**
     * Cấu hình bảng lịch sử đặt giá.
     * TableView không tự biết field nào của BidTransactionDTO hiển thị ở cột nào,
     * nên ta phải chỉ rõ bằng setCellValueFactory().
     */
    private void setupBidHistoryTable() {
        if (bidHistoryTable == null) {
            return;
        }

        bidderNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(safeText(cellData.getValue().getBidderName()))
        );

        // ĐÃ SỬA: Thay thế sang SimpleObjectProperty để tương thích chuẩn với kiểu dữ liệu Column Number, tránh ClassCastException
        bidAmountColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getAmount())
        );

        // ĐÃ CẬP NHẬT: Định dạng tiền tệ đẹp (ví dụ: 5,200,000) hiển thị trong cell thay vì số thập phân thô
        bidAmountColumn.setCellFactory(col -> new TableCell<BidTransactionDTO, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatMoney(item.doubleValue()));
                }
            }
        });

        bidTimeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatDateTime(cellData.getValue().getTime()))
        );

        bidStatusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(safeText(cellData.getValue().getStatus()))
        );

        // ĐÃ CẬP NHẬT: Logic gán Class CSS động cho cột Trạng thái dựa vào giá trị thực tế của bản ghi dữ liệu
        bidStatusColumn.setCellFactory(col -> new TableCell<BidTransactionDTO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    getStyleClass().removeAll("status-accepted", "status-refunded");
                } else {
                    setText(item);
                    getStyleClass().removeAll("status-accepted", "status-refunded");

                    // SỬA LỖI: Đồng bộ màu sắc động không bị đơn điệu khi cuộn bảng lịch sử
                    if (item.equalsIgnoreCase("ACCEPTED")) {
                        getStyleClass().add("status-accepted");
                    } else if (item.equalsIgnoreCase("REFUNDED")) {
                        getStyleClass().add("status-refunded");
                    }
                }
            }
        });

        javafx.scene.layout.VBox emptyPlaceholderBox = new javafx.scene.layout.VBox(8); // 8 là khoảng cách (spacing) giữa icon và chữ
        emptyPlaceholderBox.setAlignment(javafx.geometry.Pos.CENTER);

        // 1. Tạo Label làm Icon (Sử dụng biểu tượng emoji Unicode để không lo bị lỗi crash load file ảnh)
        Label iconLabel = new Label();
        iconLabel.getStyleClass().add("table-empty-icon");

        // 2. Tạo Label làm nội dung văn bản
        Label textLabel = new Label();
        textLabel.getStyleClass().add("table-empty-placeholder");

        // Cấu hình nội dung chữ và icon tương ứng theo Light / Dark Mode
        if (com.auction.util.SceneNavigator.isAppDarkMode) {
            iconLabel.setText("🏴‍☠️🔨"); // Icon biểu đồ giao dịch cho chế độ tối
            textLabel.setText("Hiện tại chưa kiếm được mối giao dịch!");
        } else {
            iconLabel.setText("📋✨"); // Icon búa đấu giá cho chế độ sáng
            textLabel.setText("Hiện tại chưa có người dùng nào đấu giá!");
        }

        // Thêm cả hai thành phần vào VBox (Icon xếp trên, Text xếp dưới)
        emptyPlaceholderBox.getChildren().addAll(iconLabel, textLabel);

        // Gán VBox làm Placeholder cho TableView
        bidHistoryTable.setPlaceholder(emptyPlaceholderBox);
        bidHistoryTable.setItems(bidHistoryItems);
    }

    /**
     * Gửi request GET_AUCTION_DETAIL sang Server.
     * Đây là điểm nối chính giữa màn hình chi tiết và Server.
     */
    private void loadAuctionDetail() {
        if (isBlank(auctionId)) {
            showError("Không tìm thấy auctionId của phiên đấu giá.");
            return;
        }

        setBusy(true); // Khóa các nút điều hướng để tránh xung đột dữ liệu ngầm
        showMessage("Đang tải chi tiết phiên đấu giá...");

        // ĐÃ SỬA: Đưa luồng mạng của API getAuctionDetail chạy ngầm thông qua Task để không làm treo UI chính
        Task<SocketResponse> task = new Task<>() {
            @Override
            protected SocketResponse call() {
                return auctionApi.getAuctionDetail(auctionId);
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);
            SocketResponse response = task.getValue();

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
            applyLiveBiddingAccess(); // Cập nhật lại quyền hiển thị nút live room theo dữ liệu phiên mới nhất
            showMessage("Đã tải chi tiết phiên đấu giá.");
            enterLiveRoomIfNeeded();
        });

        task.setOnFailed(event -> {
            setBusy(false);
            Throwable error = task.getException();
            showError(error == null ? "Không thể kết nối mạng đến máy chủ." : error.getMessage());
        });

        Thread thread = new Thread(task, "load-detail-worker");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Đưa dữ liệu từ AuctionDetailDTO lên các control trong FXML.
     */
    private void displayAuctionDetail(AuctionDetailDTO detail) {
        // Cập nhật tên vật phẩm đấu giá
        setLabelText(itemNameLabel, detail.getItemName());

        // Vấn đề 3: Vì FXML đã có chữ "Người bán: ", ở đây ta chỉ đổ chuỗi tên thuần túy
        setLabelText(sellerLabel, safeText(detail.getSellerUsername()));

        // Vấn đề 3: Vì FXML đã có chữ "Giá hiện tại: " và "Bước giá: ", ta chỉ đổ số tiền định dạng vào
        setLabelText(currentPriceLabel, formatMoney(detail.getCurrentPrice()) + " VNĐ");
        setLabelText(stepPriceLabel, formatMoney(detail.getStepPrice()) + " VNĐ");

        // Vấn đề 3 & 4: "Kết thúc: ..." chỉ đổ chuỗi thời gian, chữ cứng nằm tại FXML
        setLabelText(endTimeLabel, "Kết thúc: " + formatDateTime(detail.getEndTime()));

        // Vấn đề 3: Chỉ hiển thị chuỗi trạng thái thuần túy (Ví dụ: "FINISHED"), bỏ chữ thừa "Trạng thái:"
        String status = detail.getStatus() != null ? detail.getStatus().toUpperCase() : "";
        setLabelText(statusLabel, status);

        // SỬA LỖI: Cập nhật màu sắc trạng thái phiên động chuẩn xác (OPEN -> Xanh dương, RUNNING -> Xanh lá, FINISHED -> Đỏ)
        if (statusLabel != null) {
            statusLabel.getStyleClass().removeAll("ad-info-status", "ad-status-open", "ad-status-running", "ad-status-finished");
            statusLabel.getStyleClass().add("ad-info-status");
            if ("OPEN".equals(status)) {
                statusLabel.getStyleClass().add("ad-status-open");
            } else if ("RUNNING".equals(status)) {
                statusLabel.getStyleClass().add("ad-status-running");
            } else if ("FINISHED".equals(status)) {
                statusLabel.getStyleClass().add("ad-status-finished");
            }
        }

        if (descriptionTextArea != null) {
            descriptionTextArea.setText(safeText(detail.getItemDescription()));
        }

        // Gọi hàm xử lý ảnh an toàn chống mất ảnh khi dữ liệu trống
        loadItemImage(detail.getImageUrl());
        loadBidHistory(detail.getBidHistory());
    }

    /**
     * Load ảnh vật phẩm nếu DTO có imageUrl.
     */
    private void loadItemImage(String imageUrl) {
        if (itemImageView == null) {
            return;
        }

        // Nếu chuỗi imageUrl trả về từ server bị trống hoặc null
        if (isBlank(imageUrl)) {
            try {
                // Nạp lại ảnh Logo_DHCN mặc định an toàn từ thư mục resources của hệ thống
                Image defaultImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/Logo_DHCN.png")));
                itemImageView.setImage(defaultImg);
            } catch (Exception e) {
                // Giữ nguyên hoặc không làm mất ảnh nếu không tìm thấy resource
                System.err.println("Không thể nạp ảnh mặc định từ classpath: " + e.getMessage());
            }
            return;
        }

        // Nếu có đường dẫn ảnh từ server truyền xuống
        try {
            Image image = new Image(imageUrl, true); // true để load bất đồng bộ không gây lag UI
            itemImageView.setImage(image);
        } catch (Exception e) {
            try {
                // Nếu đường dẫn ảnh bị lỗi, lập tức phòng thủ bằng ảnh logo mặc định
                Image defaultImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/Logo_DHCN.png")));
                itemImageView.setImage(defaultImg);
            } catch (Exception ex) {
                System.err.println("Lỗi nạp ảnh phòng thủ: " + ex.getMessage());
            }
            showMessage("Không thể tải ảnh sản phẩm. Đang hiển thị ảnh mặc định.");
        }
    }

    /**
     * Cập nhật bảng lịch sử đặt giá.
     */
    private void loadBidHistory(List<BidTransactionDTO> bidHistory) {
        if (bidHistory == null) {
            bidHistory = Collections.emptyList();
        }

        bidHistoryItems.setAll(bidHistory);
    }

    /**
     * FXML cần có: <Button onAction="#handlePlaceBid" ... />
     *
     * Luồng đặt giá:
     * - Đọc số tiền từ bidAmountField.
     * - Kiểm tra dữ liệu nhập cơ bản ở Client.
     * - Gửi PLACE_BID sang Server.
     * - Nếu thành công, refresh lại chi tiết phiên.
     */
    @FXML
    private void handlePlaceBid() {
        if (isBlank(auctionId)) {
            showError("Không tìm thấy phiên đấu giá để đặt giá.");
            return;
        }

        Double amount = readBidAmount();

        if (amount == null) {
            return;
        }

        if (amount <= 0) {
            showError("Số tiền đặt giá phải lớn hơn 0.");
            return;
        }

        /*
         * Kiểm tra nhanh ở Client để người dùng biết lỗi sớm.
         * Server vẫn là nơi kiểm tra thật, vì Client không đáng tin tuyệt đối.
         */
        if (currentAuctionDetail != null) {
            double minimumAmount = currentAuctionDetail.getCurrentPrice() + currentAuctionDetail.getStepPrice();

            if (amount < minimumAmount) {
                showError("Giá đặt tối thiểu là " + formatMoney(minimumAmount) + " VNĐ.");
                return;
            }
        }

        setBusy(true);
        showMessage("Đang gửi yêu cầu đặt giá sang Server...");

        // ĐÃ SỬA: Đưa API đặt giá chạy ngầm tránh lag đứng khung hình UI
        Task<SocketResponse> task = new Task<>() {
            @Override
            protected SocketResponse call() {
                return auctionApi.placeBid(auctionId, amount);
            }
        };

        task.setOnSucceeded(event -> {
            SocketResponse response = task.getValue();

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

            // Tải lại chi tiết phiên đấu giá để lấy cập nhật bảng lịch sử
            loadAuctionDetail();
        });

        task.setOnFailed(event -> {
            setBusy(false);
            Throwable error = task.getException();
            showError(error == null ? "Đặt giá thất bại do mất kết nối mạng." : error.getMessage());
        });

        Thread thread = new Thread(task, "place-bid-worker");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * FXML can co: <Button onAction="#handleOpenLiveBidding" ... />
     *
     * Luong vao phong live bidding:
     * - Kiem tra auctionId hien tai co hop le khong.
     * - Chuyen sang man live-bidding.fxml thong qua SceneNavigator.
     * - SceneNavigator se truyen auctionId sang LiveBiddingController.
     * - LiveBiddingController dung auctionId nay de enterLiveRoom tren Server.
     */
    @FXML
    private void handleOpenLiveBidding() {
        if (!isCurrentUserBidder()) {
            showError("Chi tai khoan Bidder moi duoc vao phong live bidding.");
            return;
        }

        if (isBlank(auctionId)) {
            showError("Khong tim thay phien dau gia de vao phong live bidding.");
            return;
        }

        SceneNavigator.showLiveBidding(auctionId);
    }

    private void applyLiveBiddingAccess() {
        if (openLiveBiddingButton == null) {
            return;
        }

        boolean bidder = isCurrentUserBidder();
        openLiveBiddingButton.setVisible(bidder);
        openLiveBiddingButton.setManaged(bidder);
    }

    private boolean isCurrentUserBidder() {
        if (!ClientSession.isLoggedIn()) {
            return false;
        }

        UserDTO user = ClientSession.getCurrentUser();
        return user != null && user.getRole() == UserRole.BIDDER;
    }

    /**
     * Đọc và parse số tiền người dùng nhập.
     */
    private Double readBidAmount() {
        if (bidAmountField == null || isBlank(bidAmountField.getText())) {
            showError("Vui lòng nhập số tiền muốn đặt.");
            return null;
        }

        try {
            // Tối ưu hóa: dọn khoảng trắng và định dạng dấu phẩy tự động trước khi parse dữ liệu số
            String rawAmount = bidAmountField.getText().trim()
                    .replace(" ", "")
                    .replace(",", "");
            return Double.parseDouble(rawAmount);
        } catch (NumberFormatException e) {
            showError("Số tiền đặt giá không hợp lệ.");
            return null;
        }
    }

    /**
     * FXML cần có: <Button onAction="#handleRefresh" ... />
     */
    @FXML
    private void handleRefresh() {
        loadAuctionDetail();
    }

    /**
     * FXML cần có: <Button onAction="#handleBack" ... />
     *
     * Hiện SceneNavigator chưa có showAuctionList().
     * Tạm thời quay về Dashboard để code compile được.
     * Sau khi bổ sung điều hướng màn Auction List, đổi thành SceneNavigator.showAuctionList().
     */
    @FXML
    private void handleBack() {
        cleanupLiveRoom();
        SceneNavigator.showAuctionList();
    }

    /**
     * Hàm tiện ích khóa/mở form điều hướng khi tác vụ nền đang tải mạng
     */
    private void setBusy(boolean busy) {
        Platform.runLater(() -> {
            if (bidAmountField != null) bidAmountField.setDisable(busy);
            if (placeBidButton != null) placeBidButton.setDisable(busy);
            if (refreshButton != null) refreshButton.setDisable(busy);
            if (openLiveBiddingButton != null) openLiveBiddingButton.setDisable(busy);
        });
    }

    /**
     * Gán text cho Label nhưng kiểm tra null để tránh lỗi nếu FXML chưa gắn fx:id.
     */
    private void setLabelText(Label label, String text) {
        if (label != null) {
            label.setText(safeText(text));
        }
    }

    /**
     * Format tiền để hiển thị.
     */
    private String formatMoney(double value) {
        return String.format("%,.0f", value);
    }

    /**
     * Format LocalDateTime để hiển thị.
     */
    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return "";
        }

        return value.format(dateTimeFormatter);
    }

    /**
     * Tránh hiển thị null lên giao diện.
     */
    private String safeText(String value) {
        return value == null ? "" : value;
    }

    /**
     * Không dùng String.isBlank() để tránh lỗi nếu IDE compile nhầm language level thấp.
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Hiển thị message nhẹ trên màn hình.
     */
    private void showMessage(String message) {
        if (messageLabel != null) {
            messageLabel.setText(safeText(message));
        }
    }

    /**
     * Hiển thị lỗi.
     */
    private void showError(String message) {
        showMessage(message);

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText(null);
            alert.setContentText(safeText(message));
            alert.showAndWait();
        });
    }

    /**
     * Hiển thị thông báo thông thường.
     */
    private void showInfo(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thông báo");
            alert.setHeaderText(null);
            alert.setContentText(safeText(message));
            alert.showAndWait();
        });
    }

    @Override
    public void onRealtimeUpdate(SocketResponse event) {
        if (event == null || isBlank(auctionId)) {
            return;
        }

        String action = getActionName(event);

        if ("BID_UPDATE".equals(action) || "BID_UPDATED".equals(action)) {
            handleBidUpdatedEvent(event);
            return;
        }

        if ("TIME_UPDATE".equals(action)) {
            handleTimeUpdateEvent(event);
            return;
        }

        if ("STATUS_UPDATED".equals(action) || "AUCTION_ENDED".equals(action) || "AUCTION_CANCELED".equals(action)) {
            handleAuctionStatusEvent(event);
        }
    }

    private void handleBidUpdatedEvent(SocketResponse event) {
        JsonObject body = getBodyAsObject(event);
        if (body == null || !isEventForCurrentAuction(body)) {
            return;
        }

        Double currentPrice = readDouble(body, "currentPrice", "amount", "highestPrice");
        if (currentPrice == null) {
            return;
        }

        Platform.runLater(() -> {
            setLabelText(currentPriceLabel, "Giá hiện tại: " + formatMoney(currentPrice) + " VNĐ");
            showMessage(safeText(event.getMessage()));
            if (currentAuctionDetail != null) {
                currentAuctionDetail.setCurrentPrice(currentPrice);
            }
        });

        if (body.has("bidTransaction") && !body.get("bidTransaction").isJsonNull()) {
            try {
                BidTransactionDTO newBid = com.auction.utils.GsonProvider.getGson().fromJson(body.get("bidTransaction"), BidTransactionDTO.class);
                if (newBid != null) {
                    Platform.runLater(() -> {
                        if (newBid.getNewEndTime() != null) {
                            setLabelText(endTimeLabel, "Kết thúc: " + formatDateTime(newBid.getNewEndTime()));
                            if (currentAuctionDetail != null) {
                                currentAuctionDetail.setEndTime(newBid.getNewEndTime());
                            }
                        }
                        if (newBid.getLiveStepPrice() > 0) {
                            setLabelText(stepPriceLabel, "Bước giá: " + formatMoney(newBid.getLiveStepPrice()) + " VNĐ");
                            if (currentAuctionDetail != null) {
                                currentAuctionDetail.setLiveStepPrice(newBid.getLiveStepPrice());
                            }
                        }

                        boolean exists = bidHistoryItems.stream()
                                .anyMatch(b -> b.getBidId() != null && b.getBidId().equals(newBid.getBidId()));
                        if (!exists) {
                            bidHistoryItems.add(0, newBid);
                            if (bidHistoryItems.size() > 15) {
                                bidHistoryItems.remove(15);
                            }
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("[handleBidUpdatedEvent] ❌ Không thể phân tích bidTransaction: " + e.getMessage());
            }
        }
    }

    private void handleTimeUpdateEvent(SocketResponse event) {
        JsonObject body = getBodyAsObject(event);
        if (body == null || !isEventForCurrentAuction(body)) {
            return;
        }

        Long secondsRemaining = readLong(body, "secondsRemaining", "remainingSeconds", "timeRemaining");
        if (secondsRemaining == null) {
            return;
        }

        Platform.runLater(() -> {
            String timeText = "Còn lại: " + formatSeconds(secondsRemaining);
            setLabelText(endTimeLabel, timeText);
        });
    }

    private void handleAuctionStatusEvent(SocketResponse event) {
        JsonObject body = getBodyAsObject(event);
        if (body == null || !isEventForCurrentAuction(body)) {
            return;
        }

        String status = readString(body, "status", "newStatus");
        String message = readString(body, "message");
        Double finalPrice = readDouble(body, "finalPrice", "highestPrice", "currentPrice");

        Platform.runLater(() -> {
            if (!isBlank(status)) {
                String upperStatus = status.toUpperCase();
                // ĐÃ SỬA: Đồng bộ lược bỏ chữ thừa khi cập nhật qua socket realtime
                setLabelText(statusLabel, upperStatus);

                // SỬA LỖI: Đồng bộ màu sắc động khi cập nhật trạng thái phiên qua Realtime socket
                if (statusLabel != null) {
                    statusLabel.getStyleClass().removeAll("ad-info-status", "ad-status-open", "ad-status-running", "ad-status-finished");
                    statusLabel.getStyleClass().add("ad-info-status");
                    if ("OPEN".equals(upperStatus)) {
                        statusLabel.getStyleClass().add("ad-status-open");
                    } else if ("RUNNING".equals(upperStatus)) {
                        statusLabel.getStyleClass().add("ad-status-running");
                    } else if ("FINISHED".equals(upperStatus)) {
                        statusLabel.getStyleClass().add("ad-status-finished");
                    }
                }
            }

            if (finalPrice != null) {
                setLabelText(currentPriceLabel, "Giá hiện tại: " + formatMoney(finalPrice) + " VNĐ");
            }

            showMessage(!isBlank(message) ? message : safeText(event.getMessage()));

            if (isTerminalStatus(status) || "AUCTION_ENDED".equals(getActionName(event))
                    || "AUCTION_CANCELED".equals(getActionName(event))) {
                if (bidAmountField != null) bidAmountField.setDisable(true);
                if (placeBidButton != null) placeBidButton.setDisable(true);

                liveRoomJoined = false;
                unregisterRealtimeListener();
            }

            if ("FINISHED".equalsIgnoreCase(status)) {
                showInfo(!isBlank(message) ? message : safeText(event.getMessage()));
            }
        });
    }

    private void enterLiveRoomIfNeeded() {
        if (!liveRoomJoined && !isBlank(auctionId)) {
            Task<SocketResponse> enterTask = new Task<>() {
                @Override
                protected SocketResponse call() {
                    return auctionApi.enterLiveRoom(auctionId);
                }
            };
            enterTask.setOnSucceeded(e -> {
                SocketResponse resp = enterTask.getValue();
                if (resp != null && resp.isSuccess()) {
                    liveRoomJoined = true;
                    System.out.println("[AuctionDetail] Đã tham gia phòng live của phiên: " + auctionId);
                }
            });
            Thread t = new Thread(enterTask, "detail-enter-live-room");
            t.setDaemon(true);
            t.start();
        }
    }

    private void registerRealtimeListener() {
        if (listenerRegistered) {
            return;
        }
        ClientSocketService.getInstance().addRealtimeListener(this);
        listenerRegistered = true;
    }

    private void unregisterRealtimeListener() {
        if (!listenerRegistered) {
            return;
        }
        ClientSocketService.getInstance().removeRealtimeListener(this);
        listenerRegistered = false;
    }

    private void cleanupLiveRoom() {
        if (liveRoomJoined && !isBlank(auctionId)) {
            String targetId = this.auctionId;
            liveRoomJoined = false;
            Thread exitWorker = new Thread(() -> {
                try {
                    auctionApi.exitLiveRoom(targetId);
                } catch (Exception e) {
                    System.err.println("[AuctionDetailController] Không thể exit live room: " + targetId);
                }
            }, "detail-exit-worker");
            exitWorker.setDaemon(true);
            exitWorker.start();
        }
        unregisterRealtimeListener();
    }

    private String getActionName(SocketResponse event) {
        if (event == null) {
            return "";
        }
        try {
            return safeText(event.getAction());
        } catch (Exception e) {
            return "";
        }
    }

    private JsonObject getBodyAsObject(SocketResponse event) {
        if (event == null || event.getBody() == null || event.getBody().isJsonNull()) {
            return null;
        }
        JsonElement body = event.getBody();
        if (!body.isJsonObject()) {
            return null;
        }
        return body.getAsJsonObject();
    }

    private boolean isEventForCurrentAuction(JsonObject body) {
        String eventAuctionId = readString(body, "auctionId", "roomId");
        return isBlank(eventAuctionId) || eventAuctionId.equalsIgnoreCase(auctionId);
    }

    private String readString(JsonObject body, String... fieldNames) {
        if (body == null || fieldNames == null) {
            return "";
        }
        for (String fieldName : fieldNames) {
            if (body.has(fieldName) && !body.get(fieldName).isJsonNull()) {
                return body.get(fieldName).getAsString();
            }
        }
        return "";
    }

    private Double readDouble(JsonObject body, String... fieldNames) {
        if (body == null || fieldNames == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            if (body.has(fieldName) && !body.get(fieldName).isJsonNull()) {
                try {
                    return body.get(fieldName).getAsDouble();
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private Long readLong(JsonObject body, String... fieldNames) {
        if (body == null || fieldNames == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            if (body.has(fieldName) && !body.get(fieldName).isJsonNull()) {
                try {
                    return body.get(fieldName).getAsLong();
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private boolean isTerminalStatus(String status) {
        String safeStatus = safeText(status);
        return "FINISHED".equalsIgnoreCase(safeStatus)
                || "CANCELED".equalsIgnoreCase(safeStatus)
                || "PAID".equalsIgnoreCase(safeStatus);
    }

    private String formatSeconds(long totalSeconds) {
        long safeSeconds = Math.max(totalSeconds, 0);
        long hours = safeSeconds / 3600;
        long minutes = (safeSeconds % 3600) / 60;
        long seconds = safeSeconds % 60;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }
}