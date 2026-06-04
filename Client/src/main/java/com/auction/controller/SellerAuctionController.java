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

public class SellerAuctionController {
    private final ClientAuctionApi auctionApi = new ClientAuctionApi();

    // List dữ liệu gốc nhận về từ máy chủ (Master list ban đầu là sellerAuctions)
    private final ObservableList<AuctionSummaryDTO> sellerAuctions = FXCollections.observableArrayList();
    // Bộ lọc dữ liệu phục vụ tìm kiếm theo tên vật phẩm thời gian thực
    private FilteredList<AuctionSummaryDTO> filteredDataList;

    private AuctionSummaryDTO selectedAuction;

    // Khai báo formatter chuẩn để hiển thị và đọc dữ liệu thân thiện với người dùng
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Quản lý trạng thái phân trang nội bộ cho bảng danh sách
    private int currentPageIndex = 1;
    private int rowsPerPage = 10;

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

    // Các thành phần UI điều khiển tìm kiếm và phân trang bổ sung từ FXML
    @FXML private TextField searchField;
    @FXML private TextField pageSizeField;
    @FXML private Label pageLabel;
    @FXML private Button previousPageButton;
    @FXML private Button nextPageButton;

    // Khai báo thêm các thành phần UI quản lý đóng mở form động theo yêu cầu mới
    @FXML private Button editAuctionButton;
    @FXML private ScrollPane rightSplitPaneContainer;
    @FXML private Button clearFilterButton; // Nút xóa bộ lọc tìm kiếm nhanh
    @FXML private Button searchButton;

    // Khai báo thêm các nhãn quản lý nội dung thông báo bảng trống động theo yêu cầu mới
    @FXML private Label placeholderIcon;
    @FXML private Label placeholderText;

    @FXML
    private void handleSearch() {
        // Gọi lại hàm xử lý tải danh sách đấu giá (hàm này đã có sẵn logic đọc từ searchField)
        handleLoadSellerAuctions();
    }

    // Vai trò: khởi tạo màn quản lý auction của seller.
    @FXML
    public void initialize() {
        applyTheme();

        if (!ClientSession.isLoggedIn()) {
            SceneNavigator.showLogin();
            return;
        }

        initializeSellerAuctionsTable();
        setupSearchAndPaginationLogic(); // Khởi tạo ràng buộc lắng nghe tìm kiếm và kích thước dòng
        setupTablePlaceholderByTheme(SceneNavigator.isAppDarkMode); // Thiết lập thông báo bảng trống tương ứng cho từng giao diện riêng biệt
        handleLoadSellerAuctions();
    }

    // Chức năng: cấu hình bảng danh sách auction.
    private void initializeSellerAuctionsTable() {
        auctionIdColumn.setCellValueFactory(data ->
                new SimpleStringProperty(safeText(data.getValue().getAuctionId())));
        auctionItemNameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(safeText(data.getValue().getItemName())));

        auctionStatusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(safeText(data.getValue().getStatus())));

        // GIỮ NGUYÊN MÀU ĐẶC THÙ CHO TRẠNG THÁI (OPEN: xanh dương, RUNNING: xanh lá, FINISHED: đỏ)
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

        /* ==========================================================================
           LOGIC NÂNG CAO: XỬ LÝ CHỌN SẢN PHẨM KHI FORM ĐANG MỞ
           ========================================================================== */
        sellerAuctionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            // Nếu click ra vùng trống hoặc danh sách bị xóa rỗng thì bỏ qua
            if (newValue == null) return;

            // KIỂM TRA TRẠNG THÁI MỞ CỦA FORM CHỈNH SỬA
            if (rightSplitPaneContainer.isVisible()) {

                // TRƯỜNG HỢP 1: Chọn phải sản phẩm không hợp lệ (Trạng thái khác OPEN)
                String newStatus = safeText(newValue.getStatus()).toUpperCase().trim();
                if (!"OPEN".equals(newStatus)) {
                    showError("Không thể chọn phiên này! Chỉ được phép xem/sửa các phiên đấu giá ở trạng thái OPEN.\nPhiên vừa chọn đang ở trạng thái: " + newStatus);

                    // Khôi phục vùng chọn ngược lại dòng sản phẩm cũ để tránh bị lệch UI
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

                // TRƯỜNG HỢP 2: Chọn sản phẩm OPEN hợp lệ mới
                if (isFormChangedFromDefault()) {
                    // Nếu thông tin trên form hiện tại đang bị sửa đổi dở dang và chưa lưu -> Phải hỏi xác nhận
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Xác nhận chuyển đổi phiên");
                    alert.setHeaderText(null);
                    alert.setContentText("Dữ liệu của phiên hiện tại đã bị thay đổi và chưa được lưu.\nBạn có chắc chắn muốn HỦY các thay đổi này và chuyển sang nạp dữ liệu của phiên mới chọn không?");

                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        // Người dùng đồng ý hủy thay đổi cũ -> Tiến hành nạp dữ liệu phiên mới lên form
                        selectedAuction = newValue;
                        fillAuctionUpdateForm(selectedAuction);
                    } else {
                        // Người dùng bấm Hủy -> Không chuyển nữa, giữ nguyên dòng chọn cũ trên bảng
                        javafx.application.Platform.runLater(() -> {
                            sellerAuctionsTable.getSelectionModel().selectedItemProperty().removeListener((javafx.beans.value.ChangeListener)obs);
                            sellerAuctionsTable.getSelectionModel().select(oldValue);
                            sellerAuctionsTable.getSelectionModel().selectedItemProperty().addListener((javafx.beans.value.ChangeListener)obs);
                        });
                    }
                } else {
                    // Nếu dữ liệu trên form chưa có bất kỳ chỉnh sửa gì -> Tự động nạp luôn thông tin mới mà không hỏi
                    selectedAuction = newValue;
                    fillAuctionUpdateForm(selectedAuction);
                }
            } else {
                // Nếu form đang ĐÓNG: Chỉ gán con trỏ đối tượng chọn như bình thường (Theo thiết kế cũ)
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
            // CẤU HÌNH CHO GIAO DIỆN TỐI (DARK MODE)
            if (placeholderIcon != null) placeholderIcon.setText("🌌");
            if (placeholderText != null) placeholderText.setText("Không tìm thấy dữ liệu phiên trong bóng đêm");
        } else {
            // CẤU HÌNH CHO GIAO DIỆN SÁNG (LIGHT MODE)
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
        // Trường hợp 1: Chưa chọn phiên nào trên bảng
        if (selectedAuction == null) {
            showError("Vui lòng chọn một phiên đấu giá trên bảng trước khi sửa.");
            return;
        }

        // Trường hợp 2: Chọn phiên không hợp lệ (Không phải trạng thái OPEN)
        String status = safeText(selectedAuction.getStatus()).toUpperCase();
        if (!"OPEN".equals(status)) {
            showError("Phiên đấu giá không hợp lệ! Chỉ cho phép sửa đổi phiên ở trạng thái OPEN.\nPhiên hiện tại đang là: " + status);
            return;
        }

        // Trường hợp 3: Chọn phiên hợp lệ -> Mở form sửa ở bên phải, đẩy bảng về bên trái
        rightSplitPaneContainer.setVisible(true);
        rightSplitPaneContainer.setManaged(true);

        // Đổ dữ liệu chuẩn của đối tượng được chọn vào các ô nhập liệu
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
                return; // Người dùng chọn Cancel -> Không đóng form nữa
            }
        }

        // Tắt form và mở rộng bảng toàn màn hình như ban đầu
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
                return; // Dừng lại không reset dữ liệu
            }
        }

        // Nạp lại dữ liệu chuẩn ban đầu vào form
        fillAuctionUpdateForm(selectedAuction);
    }

    /**
     * Chức năng bổ sung: Xử lý xóa nhanh bộ lọc tìm kiếm đưa danh sách về mặc định
     */
    @FXML
    private void handleClearFilter() {
        if (searchField != null) {
            searchField.clear(); // Làm rỗng ô nhập, Listener tự động cập nhật lại bảng
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

        // Đã sửa lỗi: loại bỏ phương thức không hợp lệ, kiểm tra null an toàn cho cả 3 trường nhập liệu
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
        // Đóng gói danh sách gốc sellerAuctions vào FilteredList
        filteredDataList = new FilteredList<>(sellerAuctions, p -> true);

        // Lắng nghe thay đổi chữ trên ô search để lọc theo Item Name tức tức thì
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
            currentPageIndex = 1; // Reset về trang đầu khi gõ từ khóa tìm kiếm mới
            renderPaginatedTable();
        });

        // Lắng nghe thay đổi số lượng dòng hiển thị từ ô nhập kích thước trang
        pageSizeField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.trim().isEmpty()) {
                try {
                    int size = Integer.parseInt(newValue.trim());
                    if (size > 0) {
                        rowsPerPage = size;
                        currentPageIndex = 1; // Reset về trang đầu khi đổi cấu hình phân trang
                        renderPaginatedTable();
                    }
                } catch (NumberFormatException e) {
                    // Bỏ qua lỗi định dạng khi người dùng đang nhập dở chữ cái
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

        // Đảm bảo chỉ mục trang luôn nằm trong vùng an toàn
        if (currentPageIndex > maxPageIdx) currentPageIndex = maxPageIdx;
        if (currentPageIndex < 1) currentPageIndex = 1;

        int fromIndex = (currentPageIndex - 1) * rowsPerPage;
        int toIndex = Math.min(fromIndex + rowsPerPage, totalItems);

        // Trích xuất tập hợp con của trang dữ liệu hiện hành
        List<AuctionSummaryDTO> pageSubList = new ArrayList<>();
        if (fromIndex < totalItems) {
            pageSubList = filteredDataList.subList(fromIndex, toIndex);
        }

        // Ép kiểu chuyển đổi và giữ nguyên tính năng click chọn sắp xếp cột cho bảng
        ObservableList<AuctionSummaryDTO> pageObservableList = FXCollections.observableArrayList(pageSubList);
        SortedList<AuctionSummaryDTO> sortedData = new SortedList<>(pageObservableList);
        sortedData.comparatorProperty().bind(sellerAuctionsTable.comparatorProperty());

        sellerAuctionsTable.setItems(sortedData);

        // Cập nhật nhãn và trạng thái các nút bấm phân trang
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
        currentPageIndex = 1; // Đưa về trang đầu sau khi nạp mới từ server
        renderPaginatedTable(); // Đổ dữ liệu phân trang lên bảng
        showMessage("Đã đồng bộ danh sách phiên đấu giá.");
    }

    // Chức năng: đưa auction đang chọn vào form sửa.
    private void fillAuctionUpdateForm(AuctionSummaryDTO auction) {
        if (auction == null) {
            clearUpdateForm();
            return;
        }

        /*
         * KIỂM TRA BẢO VỆ UX: Chỉ cho phép sửa nếu phiên đấu giá đang ở trạng thái OPEN.
         * Nếu phiên đang chạy (RUNNING) hoặc đã kết thúc (FINISHED), khóa form chỉnh sửa lại.
         */
        String status = safeText(auction.getStatus()).toUpperCase();
        boolean isEditable = status.equals("OPEN");

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

        // SỬA ĐỔI THEO YÊU CẦU MỚI: Hỏi lại người dùng một lần nữa (Xác nhận lần 2) trước khi đẩy lệnh lên Server
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận thay đổi dữ liệu");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Bạn có chắc chắn muốn lưu lại các thay đổi cấu hình này lên hệ thống mạng hay không?");

        Optional<ButtonType> clickResult = confirmAlert.showAndWait();
        if (clickResult.isPresent() && clickResult.get() != ButtonType.OK) {
            return; // Dừng lại không thực thi đẩy gói tin socket nữa
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

        // Sau khi lưu mạng thành công, đóng form bên phải lại và trả bảng về kích thước rộng đầy đủ
        rightSplitPaneContainer.setVisible(false);
        rightSplitPaneContainer.setManaged(false);

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
        setDisabled(previousPageButton, busy);
        setDisabled(nextPageButton, busy);
        setDisabled(searchField, busy);
        setDisabled(pageSizeField, busy);
        setDisabled(editAuctionButton, busy);
        setDisabled(clearFilterButton, busy);
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