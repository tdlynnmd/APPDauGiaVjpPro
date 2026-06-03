package com.auction.controller;

import com.auction.dto.AuctionDetailDTO;
import com.auction.dto.BidTransactionDTO;
import com.auction.dto.SocketResponse;
import com.auction.dto.UserDTO;
import com.auction.enums.UserRole;
import com.auction.network.ClientAuctionApi;
import com.auction.service.ClientSocketService;
import com.auction.service.RealtimeUpdateListener;
import com.auction.util.ClientSession;
import com.auction.util.SceneNavigator;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import javafx.scene.control.TextField;
import javafx.scene.control.TableCell;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * LiveBiddingController la Controller cho man hinh dau gia truc tiep.
 *
 * Nhiem vu chinh:
 * - Nhan auctionId cua phien dau gia can xem truc tiep.
 * - Load chi tiet phien dau gia bang GET_AUCTION_DETAIL.
 * - Dang ky realtime listener voi ClientSocketService.
 * - Gui LIVE_ENTERED de Server dua client vao live room (socket realtime).
 * - Gui PLACE_BID khi nguoi dung dat gia.
 * - Nhan BID_UPDATE de cap nhat gia hien tai.
 * - Nhan TIME_UPDATE de cap nhat thoi gian con lai.
 * - Nhan LIVE_ENTERED / LIVE_EXITED (va VIEWER_COUNT_UPDATED neu co) de cap nhat so nguoi xem.
 * - Tam thoi xu ly STATUS_UPDATED neu Server gui ve.
 * - Gui LIVE_EXITED va go listener khi roi man hinh (khong dung AUCTION_UNSUBSCRIBED).
 *
 * Controller nay chi xu ly UI va dieu phoi request.
 * Server van la noi kiem tra quyen, tien, buoc gia va trang thai phien.
 */
public class LiveBiddingController implements RealtimeUpdateListener {
    private final ClientAuctionApi auctionApi = new ClientAuctionApi();
    private final ClientSocketService socketService = ClientSocketService.getInstance();
    private final ObservableList<BidTransactionDTO> bidHistoryItems = FXCollections.observableArrayList();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /*
     * Ten action realtime theo contract hien tai cua LiveRoomManage ben Server.
     *
     * Controller so sanh bang String de khong phu thuoc qua chat vao luc team
     * dang thong nhat lai ten enum trong ActionType.
     */
    private static final String ACTION_BID_UPDATE = "BID_UPDATE";
    private static final String ACTION_TIME_UPDATE = "TIME_UPDATE";
    private static final String ACTION_VIEWER_COUNT_UPDATED = "VIEWER_COUNT_UPDATED";
    private static final String ACTION_LIVE_ENTERED = "LIVE_ENTERED";
    private static final String ACTION_LIVE_EXITED = "LIVE_EXITED";
    private static final String ACTION_STATUS_UPDATED = "STATUS_UPDATED";

    /*
     * Ten action cu de tuong thich neu trong luc merge van con code server cu.
     */
    private static final String ACTION_BID_UPDATED = "BID_UPDATED";
    private static final String ACTION_AUCTION_ENDED = "AUCTION_ENDED";
    private static final String ACTION_AUCTION_CANCELED = "AUCTION_CANCELED";


    /*
     * auctionId khong lay truc tiep tu FXML.
     * Man hinh truoc se goi setAuctionId(...) sau khi load live-bidding.fxml.
     */
    private String auctionId;

    /*
     * Luu du lieu chi tiet hien tai de validate nhanh truoc khi gui PLACE_BID.
     * Day chi la validate phia Client, Server van phai validate lai.
     */
    private AuctionDetailDTO currentAuctionDetail;

    /*
     * liveRoomJoined = true khi Server da dua client vao live room cua auction hien tai.
     * Dung de tranh exitLiveRoom thua hoac enterLiveRoom lap.
     */
    private boolean liveRoomJoined;

    /*
     * listenerRegistered = true khi controller da dang ky voi ClientSocketService.
     * Dung de tranh add listener lap nhieu lan.
     */
    private boolean listenerRegistered;

    @FXML
    private Parent rootContainer; // Thêm container gốc để hỗ trợ load stylesheet động khi đổi theme

    /**
     * THÊM MỚI: Khai báo để ánh xạ và điều khiển ẩn/hiện lớp phủ Màn hình chờ từ FXML sạch
     */
    @FXML
    private javafx.scene.layout.VBox loadingOverlay;

    /**
     * FXML can co: <Label fx:id="itemNameLabel" ... />
     */
    @FXML
    private Label itemNameLabel;

    /**
     * FXML can co: <Label fx:id="sellerLabel" ... />
     */
    @FXML
    private Label sellerLabel;

    /**
     * FXML can co: <Label fx:id="currentPriceLabel" ... />
     */
    @FXML
    private Label currentPriceLabel;

    /**
     * FXML can co: <Label fx:id="stepPriceLabel" ... />
     */
    @FXML
    private Label stepPriceLabel;

    /**
     * FXML can co: <Label fx:id="statusLabel" ... />
     */
    @FXML
    private Label statusLabel;

    /**
     * FXML can co: <Label fx:id="endTimeLabel" ... />
     */
    @FXML
    private Label endTimeLabel;

    /**
     * FXML can co: <Label fx:id="timeRemainingLabel" ... />
     *
     * Neu FXML chua co label nay, controller se hien TIME_UPDATE tam tren endTimeLabel.
     */
    @FXML
    private Label timeRemainingLabel;

    /**
     * FXML can co: <Label fx:id="highestBidderLabel" ... />
     */
    @FXML
    private Label highestBidderLabel;

    /**
     * FXML can co: <Label fx:id="viewerCountLabel" ... />
     */
    @FXML
    private Label viewerCountLabel;

    /**
     * FXML can co: <Label fx:id="messageLabel" ... />
     */
    @FXML
    private Label messageLabel;

    /**
     * FXML can co: <TextField fx:id="bidAmountField" ... />
     */
    @FXML
    private TextField bidAmountField;

    @FXML
    private TextField maxAutoBidField;

    @FXML
    private TextField autoBidIncrementField;

    @FXML
    private Button setupAutoBidButton;

    @FXML
    private Button cancelAutoBidButton;

    /**
     * FXML can co: <Button fx:id="placeBidButton" onAction="#handlePlaceBid" ... />
     */
    @FXML
    private Button placeBidButton;

    @FXML
    private Button refreshButton; // ID điều khiển nút làm mới khi hệ thống đang bận

    @FXML
    private Button backButton; // ID điều khiển nút quay lại khi hệ thống đang bận

    /**
     * FXML can co: <TableView fx:id="bidHistoryTable" ... />
     */
    @FXML
    private TableView<BidTransactionDTO> bidHistoryTable;

    /**
     * FXML can co: <TableColumn fx:id="bidderNameColumn" ... />
     */
    @FXML
    private TableColumn<BidTransactionDTO, String> bidderNameColumn;

    /**
     * FXML can co: <TableColumn fx:id="bidAmountColumn" ... />
     */
    @FXML
    private TableColumn<BidTransactionDTO, Number> bidAmountColumn;

    /**
     * FXML can co: <TableColumn fx:id="bidTimeColumn" ... />
     */
    @FXML
    private TableColumn<BidTransactionDTO, String> bidTimeColumn;

    /**
     * FXML can co: <TableColumn fx:id="bidStatusColumn" ... />
     */
    @FXML
    private TableColumn<BidTransactionDTO, String> bidStatusColumn;

    /**
     * Controller con cua bieu do tang gia.
     *
     * JavaFX se inject field nay khi live-bidding.fxml include bid-price-chart.fxml
     * bang fx:id="bidPriceChart".
     */
    @FXML
    private BidPriceChartController bidPriceChartController;

    /**
     * JavaFX tu goi initialize() sau khi load live-bidding.fxml.
     *
     * Tai thoi diem nay auctionId thuong chua duoc truyen vao,
     * nen chi cau hinh UI tinh va cho setAuctionId(...) load du lieu sau.
     */
    @FXML
    public void initialize() {
        applyTheme();
        setupBidHistoryTable();
        setBidControlsDisabled(true);
        showMessage("Chưa chọn phiên đấu giá.");
        Platform.runLater(this::applyTheme);
    }

    private void applyTheme() {
        // Lấy container gốc ngoài cùng (chính là StackPane trong FXML)
        if (rootContainer == null || rootContainer.getScene() == null) return;
        Parent rootStackPane = rootContainer.getScene().getRoot();
        if (rootStackPane == null) return;

        // Xóa CSS cũ và áp dụng CSS mới cho TOÀN BỘ màn hình (bao gồm cả lớp Loading)
        rootStackPane.getStylesheets().clear();
        String css = SceneNavigator.isAppDarkMode ? "/com/auction/client/view/dark.css" : "/com/auction/client/view/light.css";
        try {
            rootStackPane.getStylesheets().add(Objects.requireNonNull(getClass().getResource(css)).toExternalForm());
        } catch (Exception e) {
            System.out.println("Lỗi nạp CSS: " + e.getMessage());
        }
    }

    /**
     * Man hinh truoc goi ham nay de truyen auctionId vao LiveBiddingController.
     *
     * Luong:
     * - Luu auctionId.
     * - Dang ky listener de nhan EVENT.
     * - Load chi tiet phien.
     * - enterLiveRoom tren Server.
     */
    public void setAuctionId(String auctionId) {
        if (!isCurrentUserBidder()) {
            showError("Chỉ tài khoản Người mua mới được vào phòng đấu giá trực tiếp.");
            return;
        }

        if (isBlank(auctionId)) {
            showError("Không tìm thấy mã phiên đấu giá.");
            return;
        }

        if (!isBlank(this.auctionId) && !this.auctionId.equals(auctionId)) {
            cleanupLiveRoom();
        }

        this.auctionId = auctionId.trim();

        /*
         * Truyen auctionId cho controller bieu do.
         *
         * Vai tro:
         * - Cho chart biet can load lich su bid cua phien nao.
         * - Cho chart loc dung BID_UPDATE realtime cua phien hien tai.
         */
        if (bidPriceChartController != null) {
            bidPriceChartController.setAuctionId(this.auctionId);
        }

        registerRealtimeListener();

        // TỐI ƯU: Chạy gộp cả 2 hành động tải dữ liệu và kết nối phòng tuần tự ngầm
        loadAuctionDetailAndEnterRoom();
    }

    /**
     * Cau hinh bang lich su bid.
     */
    private void setupBidHistoryTable() {
        if (bidHistoryTable == null) {
            return;
        }

        bidderNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(safeText(cellData.getValue().getBidderName()))
        );

        // ĐÃ SỬA: Thay đổi sang kiểu Object bọc Number để đồng bộ chuẩn với JavaFX TableColumn<..., Number>
        bidAmountColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getAmount())
        );

        // Định dạng cột số tiền hiển thị có phân tách hàng nghìn giống AD
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

        // ĐÃ SỬA: Thêm CSS động đổi màu chữ Trạng thái cho từng hàng trong TableView giống AD
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
                    if (item.equalsIgnoreCase("ACCEPTED")) {
                        getStyleClass().add("status-accepted");
                    } else if (item.equalsIgnoreCase("REFUNDED")) {
                        getStyleClass().add("status-refunded");
                    }
                }
            }
        });

        // THÊM MỚI: Thiết lập VBox chứa Icon và Dòng chữ khi bảng Realtime trống rỗng
        javafx.scene.layout.VBox emptyPlaceholderBox = new javafx.scene.layout.VBox(8); // Spacing giữa icon và chữ
        emptyPlaceholderBox.setAlignment(javafx.geometry.Pos.CENTER);

        Label iconLabel = new Label();
        iconLabel.getStyleClass().add("table-empty-icon");

        Label textLabel = new Label();
        textLabel.getStyleClass().add("table-empty-placeholder");

        // Thay đổi ngôn ngữ, nội dung và kí tự icon tùy biến theo chế độ hiển thị
        if (com.auction.util.SceneNavigator.isAppDarkMode) {
            iconLabel.setText("🏴‍☠️🔨"); // Biểu tượng thống kê sàn giao dịch ban đêm
            textLabel.setText("Hiện tại chưa kiếm được mối giao dịch!");
        } else {
            iconLabel.setText("📋✨"); // Biểu tượng búa đấu giá ban ngày
            textLabel.setText("Hiện tại chưa có người dùng nào đấu giá!");
        }

        emptyPlaceholderBox.getChildren().addAll(iconLabel, textLabel);
        bidHistoryTable.setPlaceholder(emptyPlaceholderBox);

        bidHistoryTable.setItems(bidHistoryItems);
    }

    /**
     * Dang ky controller nay vao ClientSocketService de nhan EVENT realtime.
     */
    private void registerRealtimeListener() {
        if (listenerRegistered) {
            return;
        }

        socketService.addRealtimeListener(this);
        listenerRegistered = true;
    }

    /**
     * TỐI ƯU HỢP NHẤT: Chạy ngầm tiến trình kéo dữ liệu ban đầu và tham gia vào live room để tránh đứng hình UI
     */
    private void loadAuctionDetailAndEnterRoom() {
        setBusy(true);
        showMessage("Đang đồng bộ dữ liệu và kết nối phòng trực tiếp...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // 1. Tải chi tiết phiên đấu giá trước
                SocketResponse detailResponse = auctionApi.getAuctionDetail(auctionId);
                if (detailResponse != null && detailResponse.isSuccess()) {
                    currentAuctionDetail = auctionApi.parseAuctionDetail(detailResponse);
                }

                // 2. Tiến hành kết nối vào phòng live trực tiếp
                if (!liveRoomJoined) {
                    SocketResponse enterResponse = auctionApi.enterLiveRoom(auctionId);
                    if (enterResponse != null && enterResponse.isSuccess()) {
                        liveRoomJoined = true;

                        // Khôi phục cấu hình AutoBid nếu server trả về dữ liệu
                        AuctionDetailDTO enterDetail = auctionApi.parseAuctionDetail(enterResponse);
                        if (enterDetail != null && enterDetail.getActiveAutoBidMaxBid() > 0) {
                            Platform.runLater(() -> {
                                if (maxAutoBidField != null) {
                                    maxAutoBidField.setText(String.valueOf(enterDetail.getActiveAutoBidMaxBid()));
                                }
                                if (autoBidIncrementField != null) {
                                    autoBidIncrementField.setText(String.valueOf(enterDetail.getActiveAutoBidIncrement()));
                                }
                                showMessage("✅ Đã khôi phục cấu hình AutoBid của bạn.");
                            });
                        }
                    }
                }
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);
            if (currentAuctionDetail != null) {
                displayAuctionDetail(currentAuctionDetail);
                if (liveRoomJoined) {
                    showMessage("Đã kết nối vào phòng đấu giá trực tuyến thành công.");

                    if (loadingOverlay != null) {
                        Platform.runLater(() -> {
                            loadingOverlay.setVisible(false);
                            loadingOverlay.setManaged(false);
                        });
                    }
                } else {
                    showError("Kết nối Realtime thất bại, vui lòng bấm làm mới!");
                }
            } else {
                showError("Không thể đọc dữ liệu chi tiết phiên đấu giá.");
            }
        });

        task.setOnFailed(event -> {
            setBusy(false);
            showError("Không thể hoàn tất kết nối mạng đến phòng live.");
        });

        executeThread(task, "live-init-flow");
    }

    /**
     * Load chi tiet phien dau gia de hien thi lan dau hoac refresh sau khi co thay doi.
     *
     * ĐÃ SỬA: Chuyển hoàn toàn tác vụ kéo dữ liệu mạng chạy ngầm để không block luồng UI hoặc luồng Socket.
     */
    private void loadAuctionDetail() {
        if (isBlank(auctionId)) {
            return;
        }

        Task<SocketResponse> task = new Task<>() {
            @Override
            protected SocketResponse call() {
                return auctionApi.getAuctionDetail(auctionId);
            }
        };

        task.setOnSucceeded(event -> {
            SocketResponse response = task.getValue();
            if (response != null && response.isSuccess()) {
                AuctionDetailDTO detail = auctionApi.parseAuctionDetail(response);
                if (detail != null) {
                    currentAuctionDetail = detail;
                    displayAuctionDetail(detail);
                }
            }
        });

        executeThread(task, "bg-load-detail");
    }

    /**
     * Dua AuctionDetailDTO len UI.
     */
    private void displayAuctionDetail(AuctionDetailDTO detail) {
        setLabelText(itemNameLabel, detail.getItemName());
        setLabelText(sellerLabel, "Người bán: " + safeText(detail.getSellerUsername()));
        setLabelText(currentPriceLabel, "Giá hiện tại: " + formatMoney(detail.getCurrentPrice()) + " VNĐ");
        setLabelText(stepPriceLabel, "Bước giá: " + formatMoney(detail.getStepPrice()) + " VNĐ");
        setLabelText(endTimeLabel, "Kết thúc: " + formatDateTime(detail.getEndTime()));

        // ĐÃ SỬA: Vì AuctionDetailDTO không có trường người dẫn đầu, ta lấy tên phần tử đầu tiên trong lịch sử bidHistory làm người dẫn đầu
        String leaderName = "Chưa có";
        if (detail.getBidHistory() != null && !detail.getBidHistory().isEmpty()) {
            BidTransactionDTO latestBid = detail.getBidHistory().get(0);
            if (latestBid != null && !isBlank(latestBid.getBidderName())) {
                leaderName = latestBid.getBidderName();
            }
        }
        setLabelText(highestBidderLabel, "Đang dẫn đầu: " + leaderName);

        if (highestBidderLabel != null && !highestBidderLabel.getStyleClass().contains("lb-info-leader")) {
            highestBidderLabel.getStyleClass().add("lb-info-leader");
        }

        // ĐÃ SỬA: Thay đổi chi tiết trạng thái đổi màu chữ động theo OPEN - RUNNING - FINISHED giống như AD
        if (statusLabel != null) {
            statusLabel.setText("Trạng thái: " + safeText(detail.getStatus()));
            statusLabel.getStyleClass().removeAll("status-open", "status-running", "status-finished");
            statusLabel.getStyleClass().add("status-" + safeText(detail.getStatus()).toLowerCase());
        }

        // ĐÃ SỬA: Giải quyết trường hợp phiên kết thúc sẵn từ đầu, thay vì ghi đếm ngược lỗi thời gian thì ghi rõ trạng thái
        if (isTerminalStatus(detail.getStatus()) && timeRemainingLabel != null) {
            timeRemainingLabel.setText("Phiên đã kết thúc");
        }

        loadBidHistory(detail.getBidHistory());
        setBidControlsDisabled(!canPlaceBid(detail.getStatus()));
    }

    /**
     * Cap nhat bang lich su dat gia.
     */
    private void loadBidHistory(List<BidTransactionDTO> bidHistory) {
        if (bidHistory == null) {
            bidHistory = Collections.emptyList();
        }

        bidHistoryItems.setAll(bidHistory);
    }

    /**
     * FXML can co: <Button onAction="#handlePlaceBid" ... />
     *
     * Luong dat gia:
     * - Doc so tien nguoi dung nhap.
     * - Validate nhanh theo currentPrice + stepPrice.
     * - Gui PLACE_BID sang Server.
     * - Neu thanh cong, refresh chi tiet de cap nhat lich su bid.
     */
    @FXML
    private void handlePlaceBid() {
        if (isBlank(auctionId)) {
            showError("Không tìm thấy phiên đấu giá để thực hiện.");
            return;
        }

        if (currentAuctionDetail != null && !canPlaceBid(currentAuctionDetail.getStatus())) {
            showError("Phiên đấu giá hiện không cho phép đặt giá.");
            return;
        }

        Double amount = readBidAmount();

        if (amount == null) {
            return;
        }

        if (currentAuctionDetail != null) {
            double minimumAmount = currentAuctionDetail.getCurrentPrice() + currentAuctionDetail.getStepPrice();

            if (amount < minimumAmount) {
                showError("Giá đặt tối thiểu tiếp theo phải là " + formatMoney(minimumAmount) + " VNĐ.");
                return;
            }
        }

        setBusy(true);
        showMessage("Đang xử lý yêu cầu đặt giá trực tiếp...");

        // ĐÃ SỬA: Đưa API đặt giá chạy ngầm tránh lag đứng giao diện chính
        Task<SocketResponse> task = new Task<>() {
            @Override
            protected SocketResponse call() {
                return auctionApi.placeBid(auctionId, amount);
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);
            SocketResponse response = task.getValue();

            if (response == null || !response.isSuccess()) {
                showError(response == null ? "Hệ thống không phản hồi." : response.getMessage());
                return;
            }

            if (bidAmountField != null) {
                bidAmountField.clear();
            }

            showMessage(response.getMessage() == null ? "Đặt giá thành công!" : response.getMessage());

            /*
             * BID_UPDATE event se cap nhat gia realtime.
             * Refresh them chi tiet de bang lich su bid hien thi day du hon.
             */
            loadAuctionDetail();
        });

        task.setOnFailed(event -> {
            setBusy(false);
            showError("Giao dịch đặt giá thất bại do lỗi mạng.");
        });

        executeThread(task, "live-place-bid");
    }

    /**
     * FXML can co: <Button onAction="#handleRefresh" ... />
     */
    @FXML
    private void handleRefresh() {
        // CẬP NHẬT: Kích hoạt hiển thị lại Màn hình chờ khi bấm nút Làm mới để che luồng đồng bộ lại dữ liệu
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(true);
            loadingOverlay.setManaged(true);
        }
        loadAuctionDetailAndEnterRoom();
    }

    /**
     * FXML can co: <Button onAction="#handleBack" ... />
     *
     * Truoc khi quay lai danh sach, controller phai roi live room va go listener.
     */
    @FXML
    private void handleBack() {
        cleanupLiveRoom();
        SceneNavigator.showAuctionList();
    }

    @FXML
    private void handleSetupAutoBid() {
        if (isBlank(auctionId)) {
            showError("Không tìm thấy mã phiên đấu giá.");
            return;
        }

        Double maxBid = readAmount(maxAutoBidField, "Vui lòng nhập giá tối đa mong muốn.");
        Double increment = readAmount(autoBidIncrementField, "Vui lòng nhập bước tăng tự động.");

        if (maxBid == null || increment == null) {
            return;
        }

        if (currentAuctionDetail != null) {
            double minimumAmount = currentAuctionDetail.getCurrentPrice() + currentAuctionDetail.getStepPrice();
            if (maxBid < minimumAmount) {
                showError("Giá đặt tối đa tự động phải lớn hơn hoặc bằng " + formatMoney(minimumAmount) + " VNĐ.");
                return;
            }
        }

        setBusy(true);
        showMessage("Đang thiết lập cấu hình tự động đặt giá...");

        // ĐÃ SỬA: Chạy ngầm tác vụ đăng ký AutoBid qua Task
        Task<SocketResponse> task = new Task<>() {
            @Override
            protected SocketResponse call() {
                return auctionApi.setupAutoBid(auctionId, maxBid, increment);
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);
            SocketResponse response = task.getValue();
            if (response == null || !response.isSuccess()) {
                showError(response == null ? "Không thể cài đặt tự động đấu giá." : response.getMessage());
                return;
            }
            showMessage(response.getMessage());
        });

        task.setOnFailed(event -> {
            setBusy(false);
            showError("Lỗi kết nối máy chủ khi cài đặt Auto-bid.");
        });

        executeThread(task, "live-setup-autobid");
    }

    private Double readAmount(TextField field, String emptyMessage) {
        if (field == null || isBlank(field.getText())) {
            showError(emptyMessage);
            return null;
        }

        try {
            // Tối ưu hóa: làm sạch khoảng trắng và định dạng dấu phẩy trước khi ép kiểu số
            String cleanText = field.getText().trim().replace(" ", "").replace(",", "");
            double amount = Double.parseDouble(cleanText);
            if (amount <= 0) {
                showError("Số tiền nhập vào phải lớn hơn 0.");
                return null;
            }
            return amount;
        } catch (NumberFormatException e) {
            showError("Số tiền nhập vào không hợp lệ.");
            return null;
        }
    }

    @FXML
    private void handleCancelAutoBid() {
        if (isBlank(auctionId)) {
            showError("Không xác định được phiên.");
            return;
        }

        setBusy(true);
        showMessage("Đang hủy cấu hình tự động đặt giá...");

        // ĐÃ SỬA: Ép kiểu chuẩn Task<SocketResponse> để khớp kiểu trả về từ API, hết lỗi clash method
        Task<SocketResponse> task = new Task<>() {
            @Override
            protected SocketResponse call() throws Exception {
                return auctionApi.cancelAutoBid(auctionId);
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);
            if (maxAutoBidField != null) maxAutoBidField.clear();
            if (autoBidIncrementField != null) autoBidIncrementField.clear();
            showMessage("Đã hủy cấu hình tự động đấu giá thành công.");
        });

        task.setOnFailed(event -> {
            setBusy(false);
            showError("Hủy Auto-bid thất bại do lỗi kết nối.");
        });

        executeThread(task, "live-cancel-autobid");
    }

    /**
     * ClientSocketService goi ham nay khi nhan duoc SocketResponse type = EVENT.
     *
     * Luu y:
     * - Ham nay chay tren thread doc socket.
     * - Moi thao tac cap nhat JavaFX UI phai dua vao Platform.runLater(...).
     */
    @Override
    public void onRealtimeUpdate(SocketResponse event) {
        if (event == null || isBlank(auctionId)) {
            return;
        }

        String action = getActionName(event);

        if (ACTION_BID_UPDATE.equals(action) || ACTION_BID_UPDATED.equals(action)) {
            handleBidUpdatedEvent(event);
            return;
        }

        if (ACTION_TIME_UPDATE.equals(action)) {
            handleTimeUpdateEvent(event);
            return;
        }

        if (ACTION_VIEWER_COUNT_UPDATED.equals(action)
                || ACTION_LIVE_ENTERED.equals(action)
                || ACTION_LIVE_EXITED.equals(action)) {
            handleViewerCountUpdatedEvent(event);
            return;
        }

        if (ACTION_STATUS_UPDATED.equals(action)
                || ACTION_AUCTION_ENDED.equals(action)
                || ACTION_AUCTION_CANCELED.equals(action)) {
            handleAuctionStatusEvent(event);
        }
    }

    /**
     * Xu ly event BID_UPDATE/BID_UPDATED.
     *
     * LiveRoomManage hien gui body dang BidTransactionDTO:
     * - bidderName
     * - amount
     * - time
     * - status
     */
    private void handleBidUpdatedEvent(SocketResponse event) {
        JsonObject body = getBodyAsObject(event);

        if (body == null || !isEventForCurrentAuction(body)) {
            return;
        }

        Double currentPrice = readDouble(body, "currentPrice", "amount", "highestPrice");
        String highestBidderName = readString(body, "highestBidderName", "bidderName");

        if (currentPrice == null) {
            return;
        }

        Platform.runLater(() -> {
            setLabelText(currentPriceLabel, "Giá hiện tại: " + formatMoney(currentPrice) + " VNĐ");
            setLabelText(highestBidderLabel, "Đang dẫn đầu: " + (isBlank(highestBidderName) ? "Chưa có" : highestBidderName));

            // THÊM MỚI: Đảm bảo khi cập nhật Realtime, nhãn vẫn giữ class CSS để không mất màu tối ưu
            if (highestBidderLabel != null && !highestBidderLabel.getStyleClass().contains("lb-info-leader")) {
                highestBidderLabel.getStyleClass().add("lb-info-leader");
            }

            showMessage(safeText(event.getMessage()));
            if (currentAuctionDetail != null) {
                currentAuctionDetail.setCurrentPrice(currentPrice);
            }
        });

        // ĐÃ CẢI TIẾN: Nhận thông tin bid mới từ event và cập nhật trực tiếp vào UI TableView.
        // Điều này giúp tránh việc gửi thêm HTTP/Socket request kéo chi tiết phiên (loadAuctionDetail),
        // ngăn chặn hoàn toàn "bão query" làm nghẽn/overload server khi có autobid dồn dập.
        if (body.has("bidTransaction") && !body.get("bidTransaction").isJsonNull()) {
            try {
                BidTransactionDTO newBid = com.auction.utils.GsonProvider.getGson().fromJson(body.get("bidTransaction"), BidTransactionDTO.class);
                if (newBid != null) {
                    Platform.runLater(() -> {
                        // Cập nhật lại thời gian kết thúc nếu có thay đổi (chống bắn tỉa - anti-sniping)
                        if (newBid.getNewEndTime() != null) {
                            setLabelText(endTimeLabel, "Kết thúc: " + formatDateTime(newBid.getNewEndTime()));
                            if (currentAuctionDetail != null) {
                                currentAuctionDetail.setEndTime(newBid.getNewEndTime());
                            }
                        }
                        // Cập nhật lại bước giá live nếu có thay đổi
                        if (newBid.getLiveStepPrice() > 0) {
                            setLabelText(stepPriceLabel, "Bước giá: " + formatMoney(newBid.getLiveStepPrice()) + " VNĐ");
                            if (currentAuctionDetail != null) {
                                currentAuctionDetail.setLiveStepPrice(newBid.getLiveStepPrice());
                            }
                        }

                        // Tránh thêm trùng lặp
                        boolean exists = bidHistoryItems.stream()
                                .anyMatch(b -> b.getBidId() != null && b.getBidId().equals(newBid.getBidId()));
                        if (!exists) {
                            // Thêm vào đầu bảng vì danh sách hiển thị DESC
                            bidHistoryItems.add(0, newBid);
                            // Capped tối đa 15 dòng lịch sử như server
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

    /**
     * Xu ly event TIME_UPDATE.
     *
     * Event nay do LiveRoomManage gui khi Server co countdown realtime.
     */
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
            // ĐÃ SỬA: Sửa lỗi hiển thị "Còn lại: --" khi phiên kết thúc hẳn từ luồng đếm ngược
            if (secondsRemaining <= 0 || (currentAuctionDetail != null && isTerminalStatus(currentAuctionDetail.getStatus()))) {
                if (timeRemainingLabel != null) {
                    timeRemainingLabel.setText("Phiên đã kết thúc");
                }
            } else {
                String timeText = "Còn lại: " + formatSeconds(secondsRemaining);
                if (timeRemainingLabel != null) {
                    timeRemainingLabel.setText(timeText);
                } else {
                    setLabelText(endTimeLabel, timeText);
                }
            }
        });
    }

    /**
     * Cap nhat so nguoi xem live room.
     *
     * Server gui qua LIVE_ENTERED / LIVE_EXITED (field currentViewerCount)
     * hoac VIEWER_COUNT_UPDATED (field viewerCount) tuy phien ban contract.
     */
    private void handleViewerCountUpdatedEvent(SocketResponse event) {
        JsonObject body = getBodyAsObject(event);

        if (body == null || !isEventForCurrentAuction(body)) {
            return;
        }

        Integer viewerCount = readInteger(
                body,
                "currentViewerCount",
                "viewerCount",
                "viewCount",
                "count"
        );

        if (viewerCount == null) {
            return;
        }

        Platform.runLater(() ->
                setLabelText(viewerCountLabel, "Người xem: " + viewerCount)
        );
    }

    /**
     * Xu ly event STATUS_UPDATED, AUCTION_ENDED va AUCTION_CANCELED.
     *
     * Server dang chot lai format STATUS_UPDATED, nen ham nay xu ly phong thu.
     */
    private void handleAuctionStatusEvent(SocketResponse event) {
        JsonObject body = getBodyAsObject(event);

        if (body == null || !isEventForCurrentAuction(body)) {
            return;
        }

        String status = readString(body, "status", "newStatus");
        if (isBlank(status)) {
            if (ACTION_AUCTION_ENDED.equals(getActionName(event))) {
                status = "FINISHED";
            } else if (ACTION_AUCTION_CANCELED.equals(getActionName(event))) {
                status = "CANCELED";
            }
        }
        String finalStatus = status;
        String message = readString(body, "message");
        Double finalPrice = readDouble(body, "finalPrice", "highestPrice", "currentPrice");

        Platform.runLater(() -> {
            if (!isBlank(finalStatus)) {
                // Đổi màu text trạng thái chính (Top Bar) theo OPEN - RUNNING - FINISHED
                if (statusLabel != null) {
                    statusLabel.setText("Trạng thái: " + finalStatus);
                    statusLabel.getStyleClass().removeAll("status-open", "status-running", "status-finished");
                    statusLabel.getStyleClass().add("status-" + finalStatus.toLowerCase());
                }

                // ĐÃ SỬA: Sửa lỗi "Còn lại: --" khi trạng thái chuyển đổi đột ngột sang FINISHED/CANCELED bằng realtime event
                if (isTerminalStatus(finalStatus) && timeRemainingLabel != null) {
                    timeRemainingLabel.setText("Phiên đã kết thúc");
                }

                if (currentAuctionDetail != null) {
                    currentAuctionDetail.setStatus(finalStatus);
                }
                setBidControlsDisabled(!canPlaceBid(finalStatus));
            }

            if (finalPrice != null) {
                setLabelText(currentPriceLabel, "Giá hiện tại: " + formatMoney(finalPrice) + " VNĐ");
            }

            showMessage(!isBlank(message) ? message : safeText(event.getMessage()));

            if (isTerminalStatus(finalStatus) || ACTION_AUCTION_ENDED.equals(getActionName(event))
                    || ACTION_AUCTION_CANCELED.equals(getActionName(event))) {
                setBidControlsDisabled(true);

                /*
                 * Neu Server da ket thuc/huy room, client khong can gui LIVE_EXITED nua.
                 */
                liveRoomJoined = false;
                unregisterRealtimeListener();
            }

            if ("FINISHED".equalsIgnoreCase(finalStatus)) {
                showInfo(!isBlank(message) ? message : safeText(event.getMessage()));
            }
        });
    }

    /**
     * Goi khi roi man hinh hoac chuyen sang auctionId khac.
     */
    public void dispose() {
        cleanupLiveRoom();
    }

    /**
     * Roi live room tren Server (LIVE_EXITED) va go listener tren Client.
     *
     * ĐÃ SỬA: Đưa tiến trình thoát phòng mạng lên một Thread độc lập chạy ngầm để người dùng
     * bấm nút Back được quay ra màn hình danh sách ngay lập tức, không bị trễ mạng.
     */
    private void cleanupLiveRoom() {
        if (liveRoomJoined && !isBlank(auctionId)) {
            String targetId = this.auctionId;
            liveRoomJoined = false;

            Thread exitWorker = new Thread(() -> {
                try {
                    auctionApi.exitLiveRoom(targetId);
                } catch (Exception e) {
                    System.err.println("[LiveBiddingController] Khong the exit live room auctionId = " + targetId);
                }
            }, "live-exit-worker");
            exitWorker.setDaemon(true);
            exitWorker.start();
        }
        /*
         * Don dep controller chart khi roi phong live.
         *
         * Vai tro:
         * - Go realtime listener cua chart.
         * - Tranh controller chart cu van nhan BID_UPDATE sau khi da quay lai man khac.
         */
        if (bidPriceChartController != null) {
            bidPriceChartController.dispose();
        }
        unregisterRealtimeListener();
    }

    /**
     * Go controller nay khoi danh sach realtime listener.
     */
    private void unregisterRealtimeListener() {
        if (!listenerRegistered) {
            return;
        }

        socketService.removeRealtimeListener(this);
        listenerRegistered = false;
    }

    /**
     * Doc va parse so tien bid nguoi dung nhap.
     */
    private Double readBidAmount() {
        if (bidAmountField == null || isBlank(bidAmountField.getText())) {
            showError("Vui lòng nhập số tiền hợp lệ để đấu giá.");
            return null;
        }

        try {
            // Tối ưu hóa: làm sạch chuỗi nhập liệu tự động trước khi ép kiểu số
            String rawAmount = bidAmountField.getText().trim()
                    .replace(" ", "")
                    .replace(",", "");
            double amount = Double.parseDouble(rawAmount);

            if (amount <= 0) {
                showError("Số tiền đặt giá phải lớn hơn 0.");
                return null;
            }

            return amount;
        } catch (NumberFormatException e) {
            showError("Số tiền nhập vào không đúng định dạng số.");
            return null;
        }
    }

    /**
     * Lay ten action trong SocketResponse mot cach an toan.
     */
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

    /**
     * Lay body dang JsonObject de doc linh hoat nhieu format event khac nhau.
     */
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

    /**
     * Kiem tra body event co thuoc dung auction dang hien thi khong.
     *
     * Mot so event cua LiveRoomManage hien tai khong gui auctionId trong body
     * vi message da duoc broadcast theo room. Truong hop do controller cho qua.
     */
    private boolean isEventForCurrentAuction(JsonObject body) {
        String eventAuctionId = readString(body, "auctionId", "roomId");

        return isBlank(eventAuctionId) || isCurrentAuction(eventAuctionId);
    }

    /**
     * Doc String tu JsonObject theo nhieu ten field co the co.
     */
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

    /**
     * Doc Double tu JsonObject theo nhieu ten field co the co.
     */
    private Double readDouble(JsonObject body, String... fieldNames) {
        if (body == null || fieldNames == null) {
            return null;
        }

        for (String fieldName : fieldNames) {
            if (body.has(fieldName) && !body.get(fieldName).isJsonNull()) {
                try {
                    return body.get(fieldName).getAsDouble();
                } catch (Exception ignored) {
                    return null;
                }
            }
        }

        return null;
    }

    /**
     * Doc Long tu JsonObject theo nhieu ten field co the co.
     */
    private Long readLong(JsonObject body, String... fieldNames) {
        if (body == null || fieldNames == null) {
            return null;
        }

        for (String fieldName : fieldNames) {
            if (body.has(fieldName) && !body.get(fieldName).isJsonNull()) {
                try {
                    return body.get(fieldName).getAsLong();
                } catch (Exception ignored) {
                    return null;
                }
            }
        }

        return null;
    }

    /**
     * Doc Integer tu JsonObject theo nhieu ten field co the co.
     */
    private Integer readInteger(JsonObject body, String... fieldNames) {
        if (body == null || fieldNames == null) {
            return null;
        }

        for (String fieldName : fieldNames) {
            if (body.has(fieldName) && !body.get(fieldName).isJsonNull()) {
                try {
                    return body.get(fieldName).getAsInt();
                } catch (Exception ignored) {
                    return null;
                }
            }
        }

        return null;
    }

    /**
     * Kiem tra event co thuoc dung phien dang hien thi khong.
     */
    private boolean isCurrentAuction(String eventAuctionId) {
        return !isBlank(eventAuctionId) && eventAuctionId.equalsIgnoreCase(auctionId);
    }

    /**
     * Kiem tra trang thai ket thuc de khoa UI dat gia.
     */
    private boolean isTerminalStatus(String status) {
        String safeStatus = safeText(status);

        return "FINISHED".equalsIgnoreCase(safeStatus)
                || "CANCELED".equalsIgnoreCase(safeStatus)
                || "PAID".equalsIgnoreCase(safeStatus);
    }

    /**
     * Chi cho dat gia khi phien dang RUNNING.
     */
    private boolean canPlaceBid(String status) {
        return "RUNNING".equalsIgnoreCase(safeText(status));
    }

    /**
     * Bat/tat input dat gia.
     */
    private void setBidControlsDisabled(boolean disabled) {
        if (bidAmountField != null) {
            bidAmountField.setDisable(disabled);
        }
        if (placeBidButton != null) {
            placeBidButton.setDisable(disabled);
        }
        if (maxAutoBidField != null) {
            maxAutoBidField.setDisable(disabled);
        }
        if (autoBidIncrementField != null) {
            autoBidIncrementField.setDisable(disabled);
        }
        if (setupAutoBidButton != null) {
            setupAutoBidButton.setDisable(disabled);
        }
        if (cancelAutoBidButton != null) {
            cancelAutoBidButton.setDisable(disabled);
        }
    }

    /**
     * Tiện ích vô hiệu hoá điều hướng khi các Task ngầm đang tải dữ liệu qua mạng
     */
    private void setBusy(boolean busy) {
        Platform.runLater(() -> {
            boolean active = canPlaceBid(currentAuctionDetail == null ? "RUNNING" : currentAuctionDetail.getStatus());
            if (active) {
                setBidControlsDisabled(busy);
            }
            if (backButton != null) {
                backButton.setDisable(busy);
            }
            if (refreshButton != null) {
                refreshButton.setDisable(busy);
            }
        });
    }

    /**
     * Helper khởi chạy Thread công nhân (Worker Daemon) cho Task
     */
    private void executeThread(Task<?> task, String threadName) {
        Thread worker = new Thread(task, threadName);
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Gan text cho Label nhung kiem tra null de tranh loi khi FXML chua gan fx:id.
     */
    private void setLabelText(Label label, String text) {
        if (label != null) {
            label.setText(safeText(text));
        }
    }

    /**
     * Format tien de hien thi.
     */
    private String formatMoney(double value) {
        return String.format("%,.0f", value);
    }

    /**
     * Format so giay con lai thanh dang mm:ss hoac hh:mm:ss.
     */
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

    /**
     * Format LocalDateTime de hien thi.
     */
    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return "";
        }

        return value.format(dateTimeFormatter);
    }

    /**
     * Khong dung String.isBlank() de tranh loi neu IDE compile nham language level thap.
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Tranh hien thi null len giao dien.
     */
    private String safeText(String value) {
        return value == null ? "" : value;
    }

    /**
     * Hien thi message nhe tren man hinh.
     */
    private void showMessage(String message) {
        if (messageLabel != null) {
            messageLabel.setText(safeText(message));
        }
    }

    /**
     * Hien thi loi.
     */
    private void showError(String message) {
        showMessage(message);

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi cấu hình");
            alert.setHeaderText(null);
            alert.setContentText(safeText(message));
            alert.showAndWait();
        });
    }

    private void showInfo(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thông báo");
            alert.setHeaderText(null);
            alert.setContentText(safeText(message));
            alert.showAndWait();
        });
    }

    private boolean isCurrentUserBidder() {
        if (!ClientSession.isLoggedIn()) {
            return false;
        }

        UserDTO user = ClientSession.getCurrentUser();
        return user != null && user.getRole() == UserRole.BIDDER;
    }
}