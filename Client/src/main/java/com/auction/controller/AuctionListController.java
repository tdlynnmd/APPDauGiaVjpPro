package com.auction.controller;

import com.auction.dto.AuctionSummaryDTO;
import com.auction.dto.SocketResponse;
import com.auction.network.ClientAuctionApi;
import com.auction.util.SceneNavigator;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * AuctionListController là Controller phía Client cho màn hình danh sách đấu giá.
 *
 * Vai trò:
 * - Gọi ClientAuctionApi để gửi request GET_ACTIVE_AUCTIONS sang Server.
 * - Nhận SocketResponse từ Server.
 * * - Parse response.body thành List<AuctionSummaryDTO>.
 * * - Hiển thị danh sách phiên đấu giá lên TableView.
 *
 * Lưu ý:
 * - Controller chỉ xử lý giao diện và gọi API phía Client.
 * - Controller không tự làm việc trực tiếp với Socket.
 * - Controller không xử lý nghiệp vụ đấu giá.
 * - Server mới là nơi kiểm tra quyền và lấy dữ liệu thật.
 */
public class AuctionListController {
    private final ClientAuctionApi auctionApi = new ClientAuctionApi();

    /*
     * ObservableList là danh sách dữ liệu mà TableView theo dõi.
     * Khi auctionItems thay đổi, TableView có thể cập nhật lại giao diện.
     */
    private final ObservableList<AuctionSummaryDTO> auctionItems = FXCollections.observableArrayList();

    /*
     * Dùng để format LocalDateTime thành chuỗi dễ đọc trên bảng.
     */
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Hãy đảm bảo bạn đã đặt fx:id="rootPane" cho StackPane ngoài cùng trong Scene Builder.
     */
    @FXML
    private javafx.scene.layout.StackPane rootPane;

    /**
     FXML cần có: <TableView fx:id="auctionTable" ...>
     */
    @FXML
    private TableView<AuctionSummaryDTO> auctionTable;

    /**
     FXML cần có: <TableColumn fx:id="itemNameColumn" ...>
     */
    @FXML
    private TableColumn<AuctionSummaryDTO, String> itemNameColumn;

    /**
     * FXML cần có: <TableColumn fx:id="currentPriceColumn" ...>
     */
    @FXML
    private TableColumn<AuctionSummaryDTO, Number> currentPriceColumn;

    /**
     * FXML cần có: <TableColumn fx:id="statusColumn" ...>
     */
    @FXML
    private TableColumn<AuctionSummaryDTO, String> statusColumn;

    /**
     * FXML cần có: <TableColumn fx:id="endTimeColumn" ...>
     */
    @FXML
    private TableColumn<AuctionSummaryDTO, String> endTimeColumn;

    /**
     * FXML cần có: <Label fx:id="messageLabel" ...>
     Label này dùng để hiển thị trạng thái tải dữ liệu hoặc lỗi nhẹ.
     */
    @FXML
    private Label messageLabel;

    /**
     * initialize() được JavaFX tự động gọi sau khi load auction-list.fxml.
     * Luồng:
     * 1. Cấu hình các cột của TableView.
     * 2. Gắn auctionItems vào TableView.
     * 3. Gọi Server để tải danh sách phiên đấu giá.
     * 4. Tự động kiểm tra trạng thái theme toàn cục để áp màu chuẩn ngay từ lúc mở màn hình.
     */
    @FXML
    public void initialize() {
        // --- Phần kiểm tra và nạp theme đồng bộ khi vừa load giao diện ---
        if (rootPane != null) {
            rootPane.getStylesheets().clear();
            String initialPath = com.auction.util.SceneNavigator.isAppDarkMode
                    ? "/com/auction/client/view/dark.css"
                    : "/com/auction/client/view/light.css";
            try {
                String css = java.util.Objects.requireNonNull(getClass().getResource(initialPath)).toExternalForm();
                rootPane.getStylesheets().add(css);
            } catch (Exception e) {
                System.out.println("Không tìm thấy file CSS khởi tạo tại " + initialPath);
                e.printStackTrace();
            }
        }

        setupTableColumns();
        auctionTable.setItems(auctionItems);
        loadActiveAuctions();

        // Tự động gọi hàm thiết lập Placeholder động dựa trên theme toàn cục hiện tại lúc khởi tạo
        updateTablePlaceholder(com.auction.util.SceneNavigator.isAppDarkMode);
    }

    /**
     * Đổi theme sáng/tối cho màn hình Auction List dựa theo cơ chế đồng bộ toàn app của nhóm.
     * Logic giữ nguyên từ LoginController nhưng không can thiệp thay đổi giao diện/chữ của nút bấm.
     */
    @FXML
    public void toggleTheme(javafx.event.ActionEvent event) {
        if (rootPane == null) return;

        // 1. Xóa sạch các file CSS cũ đang áp trên màn hình này
        rootPane.getStylesheets().clear();

        // 2. Đọc trạng thái từ SceneNavigator để xác định file CSS cần nạp (Giống hệt Login)
        String path = com.auction.util.SceneNavigator.isAppDarkMode
                ? "/com/auction/client/view/light.css"
                : "/com/auction/client/view/dark.css";

        try {
            // 3. Nạp file CSS mới vào bộ nhớ và áp lên tấm nền rootPane
            String css = java.util.Objects.requireNonNull(getClass().getResource(path)).toExternalForm();
            rootPane.getStylesheets().add(css);

            // 4. Đảo ngược trạng thái toàn cục để hệ thống đồng bộ (Giống hệt Login)
            com.auction.util.SceneNavigator.isAppDarkMode = !com.auction.util.SceneNavigator.isAppDarkMode;

            // 5. Cập nhật lại giao diện Placeholder tương ứng với trạng thái Theme vừa đổi
            updateTablePlaceholder(com.auction.util.SceneNavigator.isAppDarkMode);

        } catch (Exception e) {
            System.out.println("Không tìm thấy file CSS tại " + path);
            e.printStackTrace();
        }
    }

    /**
     * Hàm bổ trợ cập nhật Placeholder dựa trên Theme để tách biệt "vibe" hiển thị.
     * Đã được tối ưu hóa thẩm mỹ: Đồng bộ font nghiêng (italic), độ đậm và phối màu chuẩn Vibe Ngày/Đêm.
     */
    private void updateTablePlaceholder(boolean isDarkMode) {
        VBox emptyBox = new VBox(12); // Khoảng cách giữa icon và chữ là 12px
        emptyBox.setStyle("-fx-alignment: center; -fx-padding: 30;");

        Label iconLabel = new Label();
        Label msgLabel = new Label();

        // Gán class CSS chung để chữ tự động thừa hưởng các thuộc tính nền tảng nếu có
        msgLabel.getStyleClass().add("status-message-label");

        if (isDarkMode) {
            // --- VIBE ĐÊM: Sàn đấu ngầm, kịch tính, công nghệ huyền bí ---
            iconLabel.setText("🏴‍☠️🔨");
            iconLabel.setStyle("-fx-font-size: 42px;");

            msgLabel.setText("Sàn đấu ngầm đang trống... Hãy ẩn mình chờ thời cuộc.");
            // SỬA MÀU: Dùng màu vàng cát/vàng Gold mờ (#E5B869) để tiệp với ánh Neon tiêu đề, giảm chói mắt
            msgLabel.setStyle("-fx-font-size: 14px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-font-style: italic; " +
                    "-fx-text-fill: #E5B869;");
        } else {
            // --- VIBE NGÀY: Hiện đại, chuyên nghiệp, Clean UI thương mại điện tử ---
            iconLabel.setText("📦");
            iconLabel.setStyle("-fx-font-size: 38px; -fx-opacity: 0.75;");

            msgLabel.setText("Hiện tại không có phiên đấu giá nào khả dụng. Vui lòng quay lại sau.");
            // ĐỒNG BỘ: Ép font nghiêng (italic), tăng độ đậm (bold) và dùng màu Xanh Thẫm Công Nghệ (#1E3A8A) cực sang
            msgLabel.setStyle("-fx-font-size: 14px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-font-style: italic; " +
                    "-fx-text-fill: #1E3A8A;");
        }

        emptyBox.getChildren().addAll(iconLabel, msgLabel);
        auctionTable.setPlaceholder(emptyBox); // Cập nhật lại vùng hiển thị trống của bảng
    }

    /**
     * Cấu hình cách mỗi cột lấy dữ liệu từ AuctionSummaryDTO.
     * TableView không tự biết field nào hiển thị ở cột nào,
     * nên ta phải chỉ rõ bằng setCellValueFactory().
     * NÂNG CẤP: Format tiền tệ VNĐ, căn lề tự động, nhuộm màu trạng thái, đồng bộ chữ đậm nghiêng của ngày giờ.
     */
    private void setupTableColumns() {
        /*
         Cột tên vật phẩm. Lấy dữ liệu từ AuctionSummaryDTO.getItemName().
         */
        itemNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getItemName())
        );
        itemNameColumn.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold;"); // Làm đậm tên vật phẩm để làm nổi bật thông tin cốt lõi
                }
            }
        });

        /*
         * Cột giá hiện tại. Lấy dữ liệu từ AuctionSummaryDTO.getCurrentPrice().
         */
        currentPriceColumn.setCellValueFactory(cellData ->
                new SimpleDoubleProperty(cellData.getValue().getCurrentPrice())
        );
        currentPriceColumn.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            private final java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,###");
            @Override
            protected void updateItem(Number price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    // Sửa lỗi hiển thị 2.5E7 bằng formatter và thêm hậu tố mệnh giá VNĐ
                    setText(formatter.format(price.doubleValue()) + " VNĐ");
                    setStyle("-fx-font-weight: bold; -fx-alignment: center-right;"); // Căn phải cột tiền tệ theo quy chuẩn kế toán
                }
            }
        });

        /*
         * Cột trạng thái phiên đấu giá. Ví dụ: OPEN, RUNNING, FINISHED.
         */
        statusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatus())
        );
        statusColumn.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                } else {
                    setText(status);
                    setStyle("-fx-alignment: center; -fx-font-weight: bold;"); // Căn giữa chữ trạng thái

                    // Thêm các class CSS đặc trưng tương ứng với từng trạng thái để nhuộm màu riêng biệt
                    getStyleClass().removeAll("status-running", "status-open", "status-finished");
                    if ("RUNNING".equalsIgnoreCase(status)) {
                        getStyleClass().add("status-running");
                    } else if ("OPEN".equalsIgnoreCase(status)) {
                        getStyleClass().add("status-open");
                    } else if ("FINISHED".equalsIgnoreCase(status)) {
                        getStyleClass().add("status-finished");
                    }
                }
            }
        });

        /*
         * Cột thời gian kết thúc. Nếu endTime null thì hiển thị chuỗi rỗng để tránh lỗi NullPointerException.
         */
        java.time.format.DateTimeFormatter vnFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
        endTimeColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getEndTime() == null) {
                return new SimpleStringProperty("");
            }

            // Đảo cấu trúc định dạng thời gian sang dạng Việt Nam (HH:mm dd/MM/yyyy) cho dễ đọc
            return new SimpleStringProperty(
                    cellData.getValue().getEndTime().format(vnFormatter)
            );
        });
        endTimeColumn.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: center;"); // Căn giữa cột thời gian cho bố cục đối xứng gọn gàng
                }
            }
        });
    }

    /**
     * Gửi request GET_ACTIVE_AUCTIONS sang Server và cập nhật TableView.
     * Đây là điểm nối chính giữa màn hình Auction List và Server.
     */
    private void loadActiveAuctions() {
        showMessage("Đang tải danh sách phiên đấu giá...");

        // ClientAuctionApi tạo SocketRequest(GET_ACTIVE_AUCTIONS), gửi sang Server và nhận về SocketResponse.
        SocketResponse response = auctionApi.getActiveAuctions();

        //Nếu response null, tức là API không trả được kết quả hợp lệ.
        if (response == null) {
            // showError("Server không trả về phản hồi hợp lệ.");
            // return;
        }

        //Nếu Server trả success=false, hiển thị message do Server gửi về.
        if (response != null && !response.isSuccess()) {
            showError(response.getMessage());
            return;
        }

        // GET_ACTIVE_AUCTIONS thành công thì response.body là List<AuctionSummaryDTO>.
        List<AuctionSummaryDTO> auctions = auctionApi.parseAuctionSummaryList(response);

        /*
         * Cập nhật ObservableList.
         * TableView đang theo dõi auctionItems nên bảng sẽ được cập nhật theo.
         */
        auctionItems.setAll(auctions);

        // =========================================================================
        // ĐOẠN CHÈN ĐỂ TEST: Bơm dữ liệu giả vào bảng
        // =========================================================================
        if (auctionItems.isEmpty()) {
            // Chuyển 1L, 2L, 3L thành "1", "2", "3" để khớp với kiểu String của Constructor DTO
            auctionItems.add(new AuctionSummaryDTO("1", "Siêu xe Lamborghini Aventador SVJ", 25000000.0, "RUNNING", java.time.LocalDateTime.now().plusHours(5)));
            auctionItems.add(new AuctionSummaryDTO("2", "Đồng hồ Rolex Submariner Gold 18K", 45000.0, "OPEN", java.time.LocalDateTime.now().plusDays(2)));
            auctionItems.add(new AuctionSummaryDTO("3", "Bức tranh quý của danh họa VjpPro", 1200.0, "FINISHED", java.time.LocalDateTime.now().minusMinutes(30)));
        }
        // =========================================================================

        if (auctionItems.isEmpty()) {
            showMessage("Hiện chưa có phiên đấu giá nào đang hoạt động.");
        } else {
            showMessage("Tìm thấy " + auctionItems.size() + " phiên đấu giá.");
        }
    }

    /**
     FXML cần có <Button text="Refresh" onAction="#handleRefresh" ... />
     Khi người dùng bấm Refresh, Client gọi lại Server để lấy danh sách mới.
     */
    @FXML
    private void handleRefresh() {
        loadActiveAuctions();
    }

    /**
     FXML cần có: <Button text="View Detail" onAction="#handleViewDetail" ... />
     Giai đoạn này ta chỉ kiểm tra đã chọn phiên chưa.
     Sau khi có AuctionDetailController, hàm này sẽ truyền auctionId sang màn chi tiết.
     */
    @FXML
    private void handleViewDetail() {
        AuctionSummaryDTO selectedAuction = auctionTable.getSelectionModel().getSelectedItem();

        if (selectedAuction == null) {
            showError("Vui lòng chọn một phiên đấu giá.");
            return;
        }

        /*
         * Chuyển sang màn chi tiết phiên đấu giá.
         * SceneNavigator sẽ load auction-detail.fxml,
         * sau đó truyền selectedAuction.getAuctionId() vào AuctionDetailController.
         */
        SceneNavigator.showAuctionDetail(selectedAuction.getAuctionId());
    }

    /**
     * FXML cần có: <Button text="Back" onAction="#handleBack" ... />
     * Quay về Dashboard.
     */
    @FXML
    private void handleBack() {
        SceneNavigator.showDashboard();
    }

    //Hiển thị message nhẹ trên màn hình.
    private void showMessage(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
        }
    }

    /**
     * Hiển thị lỗi.
     * Vừa đưa lên messageLabel nếu có,
     * vừa bật Alert để người dùng nhìn thấy rõ.
     */
    private void showError(String message) {
        showMessage(message);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    //Hiển thị thông báo thông thường.
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}