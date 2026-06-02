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

/**
 * AuctionDetailController là Controller phía Client cho màn hình chi tiết phiên đấu giá.
 *
 * Nhiệm vụ:
 * - Nhận auctionId từ màn hình danh sách đấu giá.
 * - Gọi ClientAuctionApi để gửi request GET_AUCTION_DETAIL sang Server.
 * - Nhận SocketResponse từ Server.
 * - Parse SocketResponse.body thành AuctionDetailDTO.
 * - Hiển thị thông tin chi tiết phiên đấu giá lên giao diện.
 * - Hiển thị lịch sử đặt giá của phiên đấu giá.
 * - Cho phép Bidder nhập số tiền và gửi request PLACE_BID.
 * - Refresh lại dữ liệu sau khi đặt giá thành công.
 *
 * Lưu ý:
 * - Controller này chỉ xử lý giao diện và gọi API phía Client.
 * - Controller này không tự xử lý nghiệp vụ đấu giá.
 * - Server mới là nơi kiểm tra quyền, kiểm tra số tiền bid và cập nhật dữ liệu thật.
 * - Nút "Vào phòng live" chỉ hiển thị cho Bidder; màn live dùng LIVE_ENTERED / LIVE_EXITED.
 */
public class AuctionDetailController {
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
        this.auctionId = auctionId;
        applyLiveBiddingAccess();
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

        bidTimeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatDateTime(cellData.getValue().getTime()))
        );

        bidStatusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(safeText(cellData.getValue().getStatus()))
        );

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

    /**
     * Load ảnh vật phẩm nếu DTO có imageUrl.
     */
    private void loadItemImage(String imageUrl) {
        if (itemImageView == null) {
            return;
        }

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
        /*
         * Quay lại màn danh sách đấu giá.
         * Không quay về Dashboard nữa vì luồng đúng là:
         * Auction List -> Auction Detail -> Auction List.
         */
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
}