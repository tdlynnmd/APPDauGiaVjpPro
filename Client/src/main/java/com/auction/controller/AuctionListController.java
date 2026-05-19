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

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * AuctionListController là Controller phía Client cho màn hình danh sách đấu giá.
 *
 * Vai trò:
 * - Gọi ClientAuctionApi để gửi request GET_ACTIVE_AUCTIONS sang Server.
 * - Nhận SocketResponse từ Server.
 *  * - Parse response.body thành List<AuctionSummaryDTO>.
 *  * - Hiển thị danh sách phiên đấu giá lên TableView.
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
     */
    @FXML
    public void initialize() {
        setupTableColumns();
        auctionTable.setItems(auctionItems);
        loadActiveAuctions();
    }

    /**
     * Cấu hình cách mỗi cột lấy dữ liệu từ AuctionSummaryDTO.
     * TableView không tự biết field nào hiển thị ở cột nào,
     * nên ta phải chỉ rõ bằng setCellValueFactory().
     */
    private void setupTableColumns() {
        /*
         Cột tên vật phẩm. Lấy dữ liệu từ AuctionSummaryDTO.getItemName().
         */
        itemNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getItemName())
        );

        /*
         * Cột giá hiện tại. Lấy dữ liệu từ AuctionSummaryDTO.getCurrentPrice().
         */
        currentPriceColumn.setCellValueFactory(cellData ->
                new SimpleDoubleProperty(cellData.getValue().getCurrentPrice())
        );

        /*
         * Cột trạng thái phiên đấu giá. Ví dụ: OPEN, RUNNING, FINISHED.
         */
        statusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatus())
        );

        /*
         * Cột thời gian kết thúc. Nếu endTime null thì hiển thị chuỗi rỗng để tránh lỗi NullPointerException.
         */
        endTimeColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getEndTime() == null) {
                return new SimpleStringProperty("");
            }

            return new SimpleStringProperty(
                    cellData.getValue().getEndTime().format(dateTimeFormatter)
            );
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
            showError("Server không trả về phản hồi hợp lệ.");
            return;
        }

        //Nếu Server trả success=false, hiển thị message do Server gửi về.
        if (!response.isSuccess()) {
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

        if (auctions.isEmpty()) {
            showMessage("Hiện chưa có phiên đấu giá nào đang hoạt động.");
        } else {
            showMessage("Tìm thấy " + auctions.size() + " phiên đấu giá.");
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
         * TODO:
         * Sau khi làm AuctionDetailController, thay đoạn này bằng điều hướng sang màn chi tiết.
         * Cần truyền selectedAuction.getAuctionId() cho màn Auction Detail.
         */
        showInfo("Auction selected: " + selectedAuction.getAuctionId());
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