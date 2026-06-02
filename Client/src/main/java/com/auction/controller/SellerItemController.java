package com.auction.controller;

import com.auction.dto.CreateItemRequest;
import com.auction.dto.ItemDetailDTO;
import com.auction.dto.ItemSummaryDTO;
import com.auction.dto.SocketResponse;
import com.auction.dto.UpdateItemRequest;
import com.auction.network.ClientAuctionApi;
import com.auction.network.ClientItemApi;
import com.auction.util.SceneNavigator;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import com.auction.dto.AuctionSummaryDTO;
import javafx.scene.control.TableColumn;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * SellerItemController là controller phía Client cho màn Seller quản lý vật phẩm.
 *
 * Vai trò chính:
 * - Quản lý danh sách item của Seller.
 * - Tạo item mới.
 * - Xem chi tiết item.
 * - Cập nhật item.
 * - Xóa/ẩn item.
 * - Chọn item để tạo phiên đấu giá.
 * - Gửi request qua ClientItemApi / ClientAuctionApi, không xử lý socket trực tiếp.
 *
 * Controller này được thiết kế để người làm FXML có thể dựng màn hoàn chỉnh.
 * Các fx:id/onAction bên dưới là contract giữa FXML và controller.
 */
public class SellerItemController {
    private final ClientAuctionApi auctionApi = new ClientAuctionApi();
    private final ClientItemApi itemApi = new ClientItemApi();

    private final ObservableList<ItemSummaryDTO> sellerItems = FXCollections.observableArrayList();
    private final ObservableList<AuctionSummaryDTO> sellerAuctions = FXCollections.observableArrayList();

    private ItemSummaryDTO selectedItem;
    private javafx.collections.transformation.FilteredList<ItemSummaryDTO> filteredItems;
    private boolean showAllStatusesMode = false; // false: Chỉ hiện ACTIVE | true: Hiện tất cả
    private boolean isNameAscending = true;
    private boolean isPriceAscending = true;
    private boolean isTypeAscending = true;

    // =========================================================================
    // KHAI BÁO BIẾN ĐIỀU KHIỂN LUỒNG FORM RIÊNG BIỆT: TẠO RIÊNG - SỬA RIÊNG
    // =========================================================================
    @FXML private ScrollPane rightSplitPaneContainer;
    @FXML private VBox formCreateContainer;     // Form chuyên biệt phục vụ việc TẠO MỚI
    @FXML private VBox formEditContainer;       // Form chuyên biệt phục vụ việc CHỈNH SỬA & ĐẤU GIÁ

    // =========================
    // Root / Header / Status UI
    // =========================

    @FXML private StackPane rootContainer;
    @FXML private Label dynamicTitleLabel;
    @FXML private Label formTitleLabel;
    @FXML private Label selectedItemIdLabel;
    @FXML private Label itemNameLabel;
    @FXML private Label messageLabel;
    @FXML private Button btnSubmit;

    // =========================
    // Item list UI
    // =========================
    // FXML nên khai báo TableView này để Seller chọn item cụ thể.

    @FXML private TableView<ItemSummaryDTO> sellerItemsTable;
    @FXML private TableColumn<ItemSummaryDTO, String> itemIdColumn;
    @FXML private TableColumn<ItemSummaryDTO, String> itemNameColumn;
    @FXML private TableColumn<ItemSummaryDTO, String> itemTypeColumn;
    @FXML private TableColumn<ItemSummaryDTO, Number> startingPriceColumn;
    @FXML private TableColumn<ItemSummaryDTO, String> statusColumn;

    // =========================
    // Item form UI
    // =========================
    // Có thể dùng ComboBox hoặc TextField cho itemType.
    // Nếu FXML có cả hai, ComboBox được ưu tiên.

    @FXML private ComboBox<String> itemTypeComboBox;
    @FXML private TextField itemTypeField;
    @FXML private TextField itemNameField;
    @FXML private TextField startingPriceField;
    @FXML private TextField descriptionField;
    @FXML private TextField yearCreatedField;
    @FXML private TextField imageUrlField;

    // Field riêng cho ART.
    @FXML private VBox artFieldsBox;
    @FXML private TextField painterField;
    @FXML private TextField artStyleField;

    // Field riêng cho ELECTRONICS.
    @FXML private VBox electronicsFieldsBox;
    @FXML private TextField brandField;
    @FXML private TextField warrantyMonthsField;

    // Field riêng cho VEHICLES.
    @FXML private VBox vehicleFieldsBox;
    @FXML private TextField modelField;
    @FXML private TextField engineTypeField;
    @FXML private TextField licensePlateField;
    @FXML private TextField kmAgeField;

    // Các field phụ cho update/delete/detail.
    @FXML private TextField updateItemIdField;
    @FXML private TextField deleteItemIdField;
    @FXML private TextField detailItemIdField;

    // =========================================================================
    // CÁC THÀNH PHẦN INPUT ĐỘC LẬP DÀNH RIÊNG CHO FORM CHỈNH SỬA (PREFIX: edit)
    // =========================================================================
    @FXML private ComboBox<String> editItemTypeComboBox;
    @FXML private TextField editItemNameField;
    @FXML private TextField editStartingPriceField;
    @FXML private TextField editYearCreatedField;
    @FXML private TextField editDescriptionField;
    @FXML private TextField editImageUrlField;

    @FXML private VBox editArtFieldsBox;
    @FXML private TextField editPainterField;
    @FXML private TextField editArtStyleField;

    @FXML private VBox editElectronicsFieldsBox;
    @FXML private TextField editBrandField;
    @FXML private TextField editWarrantyMonthsField;

    @FXML private VBox editVehicleFieldsBox;
    @FXML private TextField editModelField;
    @FXML private TextField editEngineTypeField;
    @FXML private TextField editLicensePlateField;
    @FXML private TextField editKmAgeField;

    // =========================
    // Auction form UI
    // =========================

    @FXML private TextField itemIdField;
    @FXML private TextField stepPriceField;
    @FXML private TextField startTimeField;
    @FXML private TextField endTimeField;

    @FXML private VBox auctionConfigContainer;
    @FXML private Label auctionItemIdLabel;
    @FXML private Label auctionItemNameLabel;

    private com.auction.dto.ItemSummaryDTO currentEditingItem = null;

    private void applyStatusFilterAndSort() {
        if (filteredItems == null) return;

        // 1. BỘ LỌC ẨN / HIỆN THEO CHẾ ĐỘ XEM (Giữ nguyên logic chuẩn của bạn)
        filteredItems.setPredicate(item -> {
            if (item == null || item.getStatus() == null) return false;
            String status = item.getStatus().toUpperCase();
            if (showAllStatusesMode) {
                return true;
            } else {
                return "ACTIVE".equals(status);
            }
        });

        // 2. BỘ SẮP XẾP SỬA ĐỔI TOÀN DIỆN THEO YÊU CẦU MỚI
        SortedList<ItemSummaryDTO> sortedItems = new SortedList<>(filteredItems);

        // Kiểm tra xem người dùng đang kích hoạt sắp xếp ở cột nào trên TableView
        if (sellerItemsTable.getSortOrder().isEmpty()) {
            // TRƯỜNG HỢP MẶC ĐỊNH HOẶC KHI ẤN CỘT TRẠNG THÁI: Sắp xếp theo ACTIVE ➔ SOLD ➔ INACTIVE
            sortedItems.setComparator((item1, item2) -> {
                if (item1 == null || item2 == null) return 0;
                String s1 = item1.getStatus() == null ? "" : item1.getStatus().toUpperCase();
                String s2 = item2.getStatus() == null ? "" : item2.getStatus().toUpperCase();
                if (s1.equals(s2)) return 0;

                int p1 = "ACTIVE".equals(s1) ? 1 : (("SOLD".equals(s1) || "CLOSE".equals(s1) || "CLOSED".equals(s1)) ? 2 : 3);
                int p2 = "ACTIVE".equals(s2) ? 1 : (("SOLD".equals(s2) || "CLOSE".equals(s2) || "CLOSED".equals(s2)) ? 2 : 3);
                return Integer.compare(p1, p2);
            });
        } else {
            // Lấy ra cột đang được yêu cầu sắp xếp
            TableColumn<ItemSummaryDTO, ?> currentSortColumn = sellerItemsTable.getSortOrder().get(0);

            if (currentSortColumn == itemNameColumn) {
                // SẮP XẾP THEO TÊN (BẢNG CHỮ CÁI)
                sortedItems.setComparator((item1, item2) -> {
                    if (item1 == null || item2 == null) return 0;
                    String name1 = item1.getItemName() == null ? "" : item1.getItemName();
                    String name2 = item2.getItemName() == null ? "" : item2.getItemName();

                    int comp = name1.compareToIgnoreCase(name2);
                    return isNameAscending ? comp : -comp;
                });
            }
            else if (currentSortColumn == startingPriceColumn) {
                // SẮP XẾP THEO GIÁ TIỀN
                sortedItems.setComparator((item1, item2) -> {
                    if (item1 == null || item2 == null) return 0;
                    int comp = Double.compare(item1.getStartingPrice(), item2.getStartingPrice());
                    return isPriceAscending ? comp : -comp;
                });
            }
            else if (currentSortColumn == itemTypeColumn) {
                // SẮP XẾP THỂ LOẠI THEO TRẠNG THÁI VẬT PHẨM
                sortedItems.setComparator((item1, item2) -> {
                    if (item1 == null || item2 == null) return 0;
                    String s1 = item1.getStatus() == null ? "" : item1.getStatus().toUpperCase();
                    String s2 = item2.getStatus() == null ? "" : item2.getStatus().toUpperCase();
                    if (s1.equals(s2)) return 0;

                    int p1 = "ACTIVE".equals(s1) ? 1 : (("SOLD".equals(s1) || "CLOSE".equals(s1) || "CLOSED".equals(s1)) ? 2 : 3);
                    int p2 = "ACTIVE".equals(s2) ? 1 : (("SOLD".equals(s2) || "CLOSE".equals(s2) || "CLOSED".equals(s2)) ? 2 : 3);

                    int comp = Integer.compare(p1, p2);
                    return isTypeAscending ? comp : -comp;
                });
            }
        }

        sellerItemsTable.setItems(sortedItems);
    }
    // Bang xem những auction của seller
    @FXML private TableView<AuctionSummaryDTO> sellerAuctionsTable;
    @FXML private TableColumn<AuctionSummaryDTO, String> auctionIdColumn;
    @FXML private TableColumn<AuctionSummaryDTO, String> auctionItemNameColumn;
    @FXML private TableColumn<AuctionSummaryDTO, String> auctionStatusColumn;
    @FXML private TableColumn<AuctionSummaryDTO, String> auctionStartTimeColumn;
    @FXML private TableColumn<AuctionSummaryDTO, String> auctionEndTimeColumn;
    @FXML private TableColumn<AuctionSummaryDTO, Number> auctionStepPriceColumn;

    private AuctionSummaryDTO selectedAuction;

    @FXML
    public void initialize() {
        // 1. Khởi tạo các thành phần điều khiển và cấu trúc bảng dữ liệu trước (ĐỂ TRÁNH RESET CÁC CONTROL)
        initializeItemTypeControl();
        initializeSellerItemsTable();
        initializeSellerAuctionsTable();
        fillDefaultTimeIfEmpty();

        // 2. Áp dụng theme hệ thống và nạp giao diện placeholder động tương ứng cho bảng
        applyTheme();

        // 3. Khởi tạo ban đầu: Ẩn hết toàn bộ Panel bên phải đi cho thoáng màn hình
        if (rightSplitPaneContainer != null) { rightSplitPaneContainer.setVisible(false); rightSplitPaneContainer.setManaged(false); }
        if (formCreateContainer != null) { formCreateContainer.setVisible(false); formCreateContainer.setManaged(false); }
        if (formEditContainer != null) { formEditContainer.setVisible(false); formEditContainer.setManaged(false); }
        if (auctionConfigContainer != null) { auctionConfigContainer.setVisible(false); auctionConfigContainer.setManaged(false); }

        // 4. Tải dữ liệu từ database lên bảng
        updateTypeSpecificFieldsVisibility(readItemType()); // Đưa hàm này xuống sát phần load dữ liệu
        handleLoadSellerItems();
    }

    /**
     * Áp dụng theme hiện tại của ứng dụng dựa trên cấu hình hệ thống toàn cục.
     */
    private void applyTheme() {
        if (rootContainer == null) {
            return;
        }

        // Xóa bỏ các stylesheet cũ để tránh xung đột
        rootContainer.getStylesheets().clear();

        String cssPath = SceneNavigator.isAppDarkMode
                ? "/com/auction/client/view/dark.css"
                : "/com/auction/client/view/light.css";

        try {
            String css = Objects.requireNonNull(getClass().getResource(cssPath)).toExternalForm();
            rootContainer.getStylesheets().add(css);
        } catch (Exception e) {
            System.out.println("Không thể nạp theme cho SIM: " + cssPath);
        }

        // --- ĐOẠN CODE TỰ ĐỘNG THÔNG BÁO CHẾ ĐỘ MÀU CHUẨN PHONG CÁCH SIM ---
        if (SceneNavigator.isAppDarkMode) {
            setLabelText(dynamicTitleLabel, "QUẢN LÝ VẬT PHẨM");
            showMessage("● SYSTEM DARK MODE SIGNED ⚡");
        } else {
            setLabelText(dynamicTitleLabel, "QUẢN LÝ VẬT PHẨM");
            showMessage("✓ Hệ thống đã sẵn sàng.");
        }

        // --- CẬP NHẬT: TỰ ĐỘNG ĐỔI ICON VÀ CHỮ KHI BẢNG RỖNG ---
        if (sellerItemsTable != null) { // Lưu ý: Hãy thay 'sellerItemsTable' bằng đúng tên biến TableView trong code của bạn
            // Tạo một Layout VBox chứa Icon xếp trên Chữ
            javafx.scene.layout.VBox placeholderBox = new javafx.scene.layout.VBox();
            placeholderBox.setAlignment(javafx.geometry.Pos.CENTER);
            placeholderBox.setSpacing(10); // Tạo khoảng cách thông thoáng giữa Icon và Chữ

            // Label làm Icon hiển thị lớn ở trên
            javafx.scene.control.Label iconLabel = new javafx.scene.control.Label();
            iconLabel.getStyleClass().add("placeholder-icon"); // Định danh lớp CSS để chỉnh cỡ font và màu sắc

            // Label làm dòng chữ thông báo ở dưới
            javafx.scene.control.Label textLabel = new javafx.scene.control.Label();
            textLabel.getStyleClass().add("placeholder-text"); // Định danh lớp CSS cho chữ

            // Gán nội dung động tương ứng với từng Theme
            if (SceneNavigator.isAppDarkMode) {
                iconLabel.setText("🏴‍☠️🔨");
                textLabel.setText("Hàng tồn hiện không còn trong kho");
            } else {
                iconLabel.setText("📦"); // Icon hộp quà giống hệt như giao diện mẫu bạn gửi
                textLabel.setText("Không có sản phẩm nào trong danh sách");
            }

            // Đưa các Label vào Box và thiết lập làm Placeholder cho bảng
            placeholderBox.getChildren().addAll(iconLabel, textLabel);
            sellerItemsTable.setPlaceholder(placeholderBox);
        }
    }
    /**
     * ComboBox giúp FXML tránh cho người dùng gõ sai item type.
     */
    private void initializeItemTypeControl() {
        if (itemTypeComboBox != null) {
            itemTypeComboBox.setItems(FXCollections.observableArrayList(
                    "ART",
                    "ELECTRONICS",
                    "VEHICLES"
            ));

            itemTypeComboBox.valueProperty().addListener((obs, oldValue, newValue) ->
                    updateTypeSpecificFieldsVisibility(newValue)
            );
        }

        if (itemTypeField != null) {
            itemTypeField.textProperty().addListener((obs, oldValue, newValue) -> {
                if (itemTypeComboBox == null) {
                    updateTypeSpecificFieldsVisibility(newValue);
                }
            });
        }
    }

    /**
     * TableView là phần quan trọng để FXML có màn quản lý item hoàn chỉnh.
     * Người dùng chọn item trong bảng, controller sẽ tự load detail và fill form.
     */
    private void initializeSellerItemsTable() {
        if (sellerItemsTable == null) {
            return;
        }

        filteredItems = new javafx.collections.transformation.FilteredList<>(sellerItems, p -> true);
        applyStatusFilterAndSort();
        sellerItemsTable.setFixedCellSize(48);

        if (itemIdColumn != null) {
            itemIdColumn.setCellValueFactory(data -> new SimpleStringProperty(safeText(data.getValue().getItemId())));
        }

        if (itemNameColumn != null) {
            itemNameColumn.setCellValueFactory(data -> new SimpleStringProperty(safeText(data.getValue().getItemName())));

            itemNameColumn.setCellFactory(column -> new TableCell<ItemSummaryDTO, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                    }
                }
            });

            if (itemIdColumn != null) {
                itemIdColumn.setSortable(false);
            }

            sellerItemsTable.setOnSort(event -> {
                // Ngăn chặn JavaFX chạy bộ sort Alphabet mặc định của nó
                event.consume();

                if (!sellerItemsTable.getSortOrder().isEmpty()) {
                    TableColumn<ItemSummaryDTO, ?> col = sellerItemsTable.getSortOrder().get(0);

                    // Đảo chiều tăng/giảm sau mỗi lần click
                    if (col == itemNameColumn) isNameAscending = !isNameAscending;
                    if (col == startingPriceColumn) isPriceAscending = !isPriceAscending;
                    if (col == itemTypeColumn) isTypeAscending = !isTypeAscending;

                    // Gọi hàm xử lý sắp xếp tùy biến
                    applyStatusFilterAndSort();
                    sellerItemsTable.refresh();
                }
            });
        }

        if (itemTypeColumn != null) {
            itemTypeColumn.setCellValueFactory(data -> new SimpleStringProperty(safeText(data.getValue().getItemType())));
        }

        if (startingPriceColumn != null) {
            startingPriceColumn.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getStartingPrice()));

            startingPriceColumn.setCellFactory(column -> new TableCell<ItemSummaryDTO, Number>() {
                @Override
                protected void updateItem(Number price, boolean empty) {
                    super.updateItem(price, empty);
                    if (empty || price == null) {
                        setText(null);
                    } else {
                        java.text.DecimalFormat df = new java.text.DecimalFormat("#,###");
                        setText(df.format(price.doubleValue()) + " VNĐ");
                    }
                }
            });
        }

        if (statusColumn != null) {
            statusColumn.setCellValueFactory(data -> new SimpleStringProperty(safeText(data.getValue().getStatus())));

            statusColumn.setCellFactory(column -> new TableCell<ItemSummaryDTO, String>() {
                @Override
                protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);
                    if (empty || status == null) {
                        setText(null);
                        getStyleClass().removeAll("status-active", "status-open", "status-close");
                    } else {
                        setText(status);
                        getStyleClass().removeAll("status-active", "status-open", "status-close");

                        if ("ACTIVE".equalsIgnoreCase(status)) {
                            getStyleClass().add("status-active");
                        } else if ("OPEN".equalsIgnoreCase(status)) {
                            getStyleClass().add("status-open");
                        } else if ("CLOSE".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
                            getStyleClass().add("status-close");
                        }
                    }
                }
            });

            // =========================================================================
            // LOGIC ẤN VÀO TIÊU ĐỀ: ĐẢO CHẾ ĐỘ ẨN/HIỆN & ĐỒNG BỘ ĐÓNG FORM KHI ITEM BIẾN MẤT
            // =========================================================================
            statusColumn.setGraphic(new Label("Trạng Thái 🔄"));
            statusColumn.getGraphic().setStyle("-fx-cursor: hand; -fx-text-fill: -fx-primary-color;");
            statusColumn.setText("");
            statusColumn.setSortable(false); // Khóa tính năng tự sort Alphabet của JavaFX

            statusColumn.getGraphic().setOnMouseClicked(event -> {
                // 1. Đảo chế độ hiển thị biến showAllStatusesMode
                showAllStatusesMode = !showAllStatusesMode;

                // 2. Chạy bộ lọc để ẩn/hiện các dòng tương ứng trên bảng
                applyStatusFilterAndSort();

                // 3. LOGIC QUAN TRỌNG: Kiểm tra nếu chuyển về chế độ "Chỉ hiện ACTIVE"
                if (!showAllStatusesMode) {
                    showMessage("Chế độ: Chỉ hiển thị các sản phẩm ACTIVE");

                    // Nếu sản phẩm đang chọn bị ẩn đi (không phải ACTIVE), ta phải đóng form lại ngay để tránh kẹt dữ liệu ẩn
                    if (this.selectedItem != null && !"ACTIVE".equalsIgnoreCase(this.selectedItem.getStatus())) {

                        // Ẩn form Sửa và form Đấu giá đi vì sản phẩm đó đã biến mất khỏi bảng
                        if (formEditContainer != null) { formEditContainer.setVisible(false); formEditContainer.setManaged(false); }
                        if (auctionConfigContainer != null) { auctionConfigContainer.setVisible(false); auctionConfigContainer.setManaged(false); }

                        // Thu gọn luôn thanh trượt bên phải
                        checkAndCollapseRightContainer();

                        // Reset trạng thái lưu trữ text gốc
                        this.originalFormText = "";
                        this.currentEditingItem = null;
                    }
                } else {
                    showMessage("Chế độ: Hiện tất cả trạng thái");
                }

                // 4. Ép bảng chọn lại sản phẩm ACTIVE đầu tiên (nếu có) để giao diện mượt mà, không bị trống dòng chọn
                if (sellerItemsTable != null && !sellerItemsTable.getItems().isEmpty()) {
                    var firstVisibleItem = sellerItemsTable.getItems().get(0);
                    sellerItemsTable.getSelectionModel().select(firstVisibleItem);
                } else {
                    this.selectedItem = null;
                }

                sellerItemsTable.refresh();
            });
        }

        // =========================================================================
        // LISTENER CHỌN DÒNG TRÊN BẢNG (Giữ nguyên logic Cảnh báo chuẩn của bạn)
        // =========================================================================
        sellerItemsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem == null) return;

            // --- CẢNH BÁO FORM TẠO PHIÊN ĐẤU GIÁ ---
            if (auctionConfigContainer != null && auctionConfigContainer.isVisible()) {
                if (isAnyFieldFilled(stepPriceField, startTimeField, endTimeField)) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Xác nhận thay đổi");
                    alert.setHeaderText("Thông tin tạo phiên đấu giá đang nhập dở!");
                    alert.setContentText("Bạn có chắc chắn muốn hủy bỏ phiên nhập hiện tại để chọn sản phẩm khác không?");

                    if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                        javafx.application.Platform.runLater(() -> sellerItemsTable.getSelectionModel().select(oldItem));
                        return;
                    }
                }
                if (stepPriceField != null) stepPriceField.clear();
            }

            // --- CẢNH BÁO FORM SỬA ---
            if (formEditContainer != null && formEditContainer.isVisible()) {
                if (isEditFormDirty()) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Xác nhận thay đổi");
                    alert.setHeaderText("Dữ liệu chỉnh sửa chưa được lưu!");
                    alert.setContentText("Bạn có chắc chắn muốn bỏ qua thay đổi hiện tại và chuyển sang sản phẩm mới không?");

                    if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                        javafx.application.Platform.runLater(() -> sellerItemsTable.getSelectionModel().select(oldItem));
                        return;
                    }
                }
            }

            // --- TIẾN HÀNH ĐỔI SẢN PHẨM ---
            this.selectedItem = newItem;
            this.currentEditingItem = newItem;

            if (auctionConfigContainer != null && auctionConfigContainer.isVisible()) {
                if (auctionItemIdLabel != null) auctionItemIdLabel.setText("Item ID: " + newItem.getItemId());
                if (auctionItemNameLabel != null) auctionItemNameLabel.setText("Vật phẩm: " + newItem.getItemName());
                fillDefaultTimeIfEmpty();
            }

            if (formEditContainer != null && formEditContainer.isVisible()) {
                loadItemDetailIntoForm(newItem.getItemId(), true);

                this.originalFormText = (editItemNameField.getText() == null ? "" : editItemNameField.getText()) + "|"
                        + (editStartingPriceField.getText() == null ? "" : editStartingPriceField.getText()) + "|"
                        + (editYearCreatedField.getText() == null ? "" : editYearCreatedField.getText()) + "|"
                        + (editDescriptionField.getText() == null ? "" : editDescriptionField.getText()) + "|"
                        + (editImageUrlField.getText() == null ? "" : editImageUrlField.getText());
            }

            if (formCreateContainer != null && formCreateContainer.isVisible()) {
                formCreateContainer.setVisible(false);
                formCreateContainer.setManaged(false);

                if (formEditContainer != null) {
                    formEditContainer.setVisible(true);
                    formEditContainer.setManaged(true);
                    loadItemDetailIntoForm(newItem.getItemId(), true);

                    this.originalFormText = (editItemNameField.getText() == null ? "" : editItemNameField.getText()) + "|"
                            + (editStartingPriceField.getText() == null ? "" : editStartingPriceField.getText()) + "|"
                            + (editYearCreatedField.getText() == null ? "" : editYearCreatedField.getText()) + "|"
                            + (editDescriptionField.getText() == null ? "" : editDescriptionField.getText()) + "|"
                            + (editImageUrlField.getText() == null ? "" : editImageUrlField.getText());
                }
            }
        });
    }

    /**
     * Hành động khi bấm nút tạo: Chỉ mở form Tạo mới, ẩn sạch form Sửa đi.
     */
    @FXML
    private void handleOpenFormForCreate() {
        rightSplitPaneContainer.setVisible(true);
        rightSplitPaneContainer.setManaged(true);
        formCreateContainer.setVisible(true);
        formCreateContainer.setManaged(true);

        // Ẩn các form khác
        formEditContainer.setVisible(false);
        formEditContainer.setManaged(false);
        if (auctionConfigContainer != null) {
            auctionConfigContainer.setVisible(false);
            auctionConfigContainer.setManaged(false);
        }
    }

    /**
     * NÚT SỬA ngoài bảng: Nếu chưa chọn item nào trên tableview thì bắn lỗi ngay.
     * Nếu đã chọn dòng, tự động đóng form tạo (nếu mở) và mở form sửa ra đẩy bảng lại.
     */
    private String originalFormText = "";

    @FXML
    private void handleOpenFormForEdit() {
        // 1. Kiểm tra xem người dùng đã chọn dòng nào trên TableView chưa
        var selectedItem = sellerItemsTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            // Báo lỗi tương tự như form cũ của bạn bằng Alert hoặc MessageLabel
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Thông báo");
            alert.setHeaderText(null);
            alert.setContentText("Vui lòng chọn một vật phẩm từ bảng danh sách trước khi thực hiện sửa!");
            alert.showAndWait();
            return;
        }

        this.currentEditingItem = selectedItem;

        // 2. Nếu đã chọn item, mở form sửa và ẩn form khác
        rightSplitPaneContainer.setVisible(true);
        rightSplitPaneContainer.setManaged(true);
        formEditContainer.setVisible(true);
        formEditContainer.setManaged(true);
        formCreateContainer.setVisible(false);
        formCreateContainer.setManaged(false);
        if (auctionConfigContainer != null) {
            auctionConfigContainer.setVisible(false);
            auctionConfigContainer.setManaged(false);
        }

        // [ĐÃ CẬP NHẬT]: Chuỗi được chụp sau khi form sửa đã hiển thị và điền đầy đủ ký tự gốc
        this.originalFormText = (editItemNameField.getText() == null ? "" : editItemNameField.getText()) + "|"
                + (editStartingPriceField.getText() == null ? "" : editStartingPriceField.getText()) + "|"
                + (editYearCreatedField.getText() == null ? "" : editYearCreatedField.getText()) + "|"
                + (editDescriptionField.getText() == null ? "" : editDescriptionField.getText()) + "|"
                + (editImageUrlField.getText() == null ? "" : editImageUrlField.getText());

        showMessage("Đang mở Form chỉnh sửa vật phẩm: " + selectedItem.getItemName());
    }

    /**
     * Hàm dùng chung xử lý đóng form Tạo và trượt form Sửa ra kèm load dữ liệu
     */
    private void activateEditFormWorkflow(ItemSummaryDTO item) {
        // Form sửa tự hiểu và ẩn form tạo đi
        setNodeVisible(formCreateContainer, false);

        // Mở rộng thanh bên phải và bật form Sửa lên độc lập
        setNodeVisible(rightSplitPaneContainer, true);
        setNodeVisible(formEditContainer, true);

        setSelectedItem(item);
        loadItemDetailIntoForm(item.getItemId(), false);
        showMessage("Đang mở Form chỉnh sửa vật phẩm: " + item.getItemName());
    }

    /**
     * Nút (X) trên Form Tạo: Đóng luôn, xóa sạch dữ liệu nhập dở, không cần cảnh báo.
     */
    @FXML
    private void handleCloseCreateForm() {
        // Chỉ hỏi khi có ít nhất 1 ô được điền ký tự bất kỳ
        if (isAnyFieldFilled(itemNameField, startingPriceField, yearCreatedField, descriptionField, imageUrlField, painterField, artStyleField, brandField, warrantyMonthsField, modelField, engineTypeField, licensePlateField, kmAgeField)) {
            if (!confirmExit()) {
                return; // Người dùng chọn Không -> Giữ nguyên form, không thoát
            }
        }

        // Thực hiện đóng nếu form trống hoặc đồng ý thoát
        rightSplitPaneContainer.setVisible(false);
        rightSplitPaneContainer.setManaged(false);
        formCreateContainer.setVisible(false);
        formCreateContainer.setManaged(false);

        handleClearItemForm(); // Xóa sạch chữ trong form tạo
        handleLoadSellerItems(); // Load lại bảng dữ liệu
    }

    /**
     * Nút (X) trên Form Sửa: Có cảnh báo nếu phát hiện người dùng đã chỉnh sửa dữ liệu dở dang chưa lưu.
     */
    @FXML
    private void handleCloseEditForm() {
        // === THÊM 3 DÒNG NÀY ĐỂ SỬA LỖI ===
        // Nếu form chỉnh sửa vốn dĩ đang đóng/ẩn thì thoát luôn, không so sánh chữ, không cảnh báo
        if (formEditContainer == null || !formEditContainer.isVisible()) {
            return;
        }

        // 1. Gom tất cả chữ hiện tại người dùng đang nhập trên màn hình lại (Bọc check null an toàn)
        String currentFormText = (editItemNameField.getText() == null ? "" : editItemNameField.getText()) + "|"
                + (editStartingPriceField.getText() == null ? "" : editStartingPriceField.getText()) + "|"
                + (editYearCreatedField.getText() == null ? "" : editYearCreatedField.getText()) + "|"
                + (editDescriptionField.getText() == null ? "" : editDescriptionField.getText()) + "|"
                + (editImageUrlField.getText() == null ? "" : editImageUrlField.getText());

        // 2. Nếu chuỗi hiện tại KHÁC chuỗi gốc ban đầu -> Tức là có người đã chỉnh sửa chữ
        if (!currentFormText.equals(this.originalFormText)) {
            if (!confirmExit()) {
                return; // Chọn không -> Giữ nguyên form
            }
        }

        // 3. Tiến hành đóng form nếu không thay đổi hoặc chọn đồng ý thoát
        formEditContainer.setVisible(false);
        formEditContainer.setManaged(false);

        this.currentEditingItem = null;
        this.originalFormText = "";

        checkAndCollapseRightContainer();
    }

    /**
     * Kiểm tra xem form sửa có bị thay đổi dữ liệu so với dữ liệu gốc của vật phẩm đang chọn không
     */
    private boolean isEditFormDirty() {
        if (selectedItem == null) return false;
        String currentName = readText(editItemNameField);
        return !isBlank(currentName) && !currentName.equals(selectedItem.getItemName());
    }

    /**
     * Thu gọn toàn bộ SplitPane bên phải nếu cả hai form đều đã đóng
     */
    private void checkAndCollapseRightContainer() {
        if ((formCreateContainer == null || !formCreateContainer.isVisible()) &&
                (formEditContainer == null || !formEditContainer.isVisible()) &&
                (auctionConfigContainer == null || !auctionConfigContainer.isVisible())) {
            setNodeVisible(rightSplitPaneContainer, false);
        }
    }

    /**
     * Xóa sạch các trường dữ liệu trên form Sửa
     */
    private void clearEditFormFields() {
        selectedItem = null;
        clearText(editItemNameField);
        clearText(editStartingPriceField);
        clearText(editYearCreatedField);
        clearText(editDescriptionField);
        clearText(editImageUrlField);
        if (editItemTypeComboBox != null) editItemTypeComboBox.getSelectionModel().clearSelection();

        clearText(editPainterField);
        clearText(editArtStyleField);
        clearText(editBrandField);
        clearText(editWarrantyMonthsField);
        clearText(editModelField);
        clearText(editEngineTypeField);
        clearText(editLicensePlateField);
        clearText(editKmAgeField);

        clearText(itemIdField);
        clearText(stepPriceField);
        fillDefaultTime();
    }

    private void updateEditTypeSpecificFieldsVisibility(String itemType) {
        if (isBlank(itemType)) {
            setNodeVisible(editArtFieldsBox, false);
            setNodeVisible(editElectronicsFieldsBox, false);
            setNodeVisible(editVehicleFieldsBox, false);
            return;
        }
        String normalizedType = itemType.trim().toUpperCase();
        setNodeVisible(editArtFieldsBox, "ART".equals(normalizedType));
        setNodeVisible(editElectronicsFieldsBox, "ELECTRONICS".equals(normalizedType));
        setNodeVisible(editVehicleFieldsBox, "VEHICLES".equals(normalizedType) || "VEHICLE".equals(normalizedType));
    }

    // =========================================================================

    /**
     * FXML action: tải danh sách item của seller đang đăng nhập.
     */
    @FXML
    private void handleLoadSellerItems() {
        refreshSellerItems(true);
    }

    /**
     * Chuc nang: cau hinh bang phien dau gia cua seller.
     */
    private void initializeSellerAuctionsTable() {
        if (sellerAuctionsTable == null) {
            return;
        }

        sellerAuctionsTable.setItems(sellerAuctions);

        if (auctionIdColumn != null) {
            auctionIdColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(safeText(data.getValue().getAuctionId())));
        }
        if (auctionItemNameColumn != null) {
            auctionItemNameColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(safeText(data.getValue().getItemName())));
        }
        if (auctionStatusColumn != null) {
            auctionStatusColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(safeText(data.getValue().getStatus())));
        }
        if (auctionStartTimeColumn != null) {
            auctionStartTimeColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getStartTime() == null ? "" : data.getValue().getStartTime().toString()));
        }
        if (auctionEndTimeColumn != null) {
            auctionEndTimeColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getEndTime() == null ? "" : data.getValue().getEndTime().toString()));
        }
        if (auctionStepPriceColumn != null) {
            auctionStepPriceColumn.setCellValueFactory(data ->
                    new SimpleDoubleProperty(data.getValue().getStepPrice()));
        }

        sellerAuctionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedAuction = newValue;
            fillAuctionUpdateForm(newValue);
        });
    }

    /**
     * Chuc nang: load danh sach phien dau gia cua seller tu backend.
     */
    @FXML
    private void handleLoadSellerAuctions() {
        SocketResponse response = auctionApi.getSellerAuctions();
        if (!isSuccessful(response)) {
            showError(response == null ? "Server khong tra ve phan hoi hop le." : response.getMessage());
            return;
        }

        sellerAuctions.setAll(auctionApi.parseAuctionSummaryList(response));
        showMessage("Da dong bo danh sach phien dau gia cua seller.");
    }

    /**
     * Chuc nang: do thong tin auction dang chon vao form cap nhat.
     */
    private void fillAuctionUpdateForm(AuctionSummaryDTO auction) {
        if (auction == null) {
            return;
        }

        setText(stepPriceField, String.valueOf(auction.getStepPrice()));
        setText(startTimeField, auction.getStartTime() == null ? "" : auction.getStartTime().toString());
        setText(endTimeField, auction.getEndTime() == null ? "" : auction.getEndTime().toString());
    }

    /**
     * Chuc nang: cap nhat phien dau gia chua chay cua seller.
     */
    @FXML
    private void handleUpdateAuction() {
        if (selectedAuction == null) {
            showError("Vui long chon mot phien dau gia can cap nhat.");
            return;
        }

        Double stepPrice = readStepPrice();
        if (stepPrice == null) return;

        LocalDateTime startTime = readDateTime(startTimeField, "thoi gian bat dau");
        if (startTime == null) return;

        LocalDateTime endTime = readDateTime(endTimeField, "thoi gian ket thuc");
        if (endTime == null) return;

        SocketResponse response = auctionApi.updateAuction(
                selectedAuction.getAuctionId(),
                stepPrice,
                startTime,
                endTime
        );

        if (!isSuccessful(response)) {
            showError(response == null ? "Cap nhat phien that bai." : response.getMessage());
            return;
        }

        handleLoadSellerAuctions();
        showInfo(response.getMessage());
    }
    /**
     * FXML action: tạo item mới.
     */
    @FXML
    private void handleCreateItem() {
        CreateItemRequest request = buildCreateItemRequest();
        if (request == null) return;

        SocketResponse response = itemApi.createItem(request);
        if (!isSuccessful(response)) {
            showError(response == null ? "Server không trả về phản hồi hợp lệ." : response.getMessage());
            return;
        }

        ItemDetailDTO createdItem = itemApi.parseItemDetail(response);

        refreshSellerItems(false);
        showInfo(response.getMessage());
        showMessage("Tạo sản phẩm thành công.");

        // Tự động ép ẩn form tạo đi một cách im lặng, không hiện popup hỏi han
        if (formCreateContainer != null) {
            formCreateContainer.setVisible(false);
            formCreateContainer.setManaged(false);
        }
        handleClearItemForm(); // Xóa chữ trong form tạo
        checkAndCollapseRightContainer(); // Co màn hình lại
    }

    /**
     * FXML action: cập nhật item đã chọn.
     */
    @FXML
    private void handleUpdateItem() {
        UpdateItemRequest request = buildUpdateItemRequestFromEditForm();
        if (request == null) {
            return;
        }

        SocketResponse response = itemApi.updateItem(request);
        if (!isSuccessful(response)) {
            showError(response == null ? "Server không trả về phản hồi hợp lệ." : response.getMessage());
            return;
        }

        ItemDetailDTO updatedItem = itemApi.parseItemDetail(response);

        refreshSellerItems(false);
        showInfo(response.getMessage());
        showMessage("Cập nhật sản phẩm thành công.");

        // [ĐÃ CẬP NHẬT]: Trực tiếp đóng và ẩn form sửa sau khi cập nhật thành công thành công
        if (formEditContainer != null) {
            formEditContainer.setVisible(false);
            formEditContainer.setManaged(false);
        }
        clearEditFormFields();
        this.currentEditingItem = null;
        this.originalFormText = "";

        checkAndCollapseRightContainer();
    }

    /**
     * FXML action: xóa/ẩn item hiện tại.
     */
    @FXML
    private void handleDeleteItem() {
        var selectedItemLocal = sellerItemsTable.getSelectionModel().getSelectedItem();
        if (selectedItemLocal == null) {
            showError("Vui lòng chọn một vật phẩm trong danh sách để xóa.");
            return;
        }

        // Kiểm tra điều kiện: Chỉ cho phép xóa sản phẩm đang ở trạng thái ACTIVE
        String currentStatus = selectedItemLocal.getStatus() == null ? "" : selectedItemLocal.getStatus().toUpperCase();
        if (!"ACTIVE".equals(currentStatus)) {
            showError("Chỉ có thể xóa sản phẩm đang hoạt động (ACTIVE). Sản phẩm mang trạng thái " + currentStatus + " không thể xóa!");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("Không còn nhu cầu đấu giá");
        dialog.setTitle("Lý do xóa vật phẩm");
        dialog.setHeaderText("Xác nhận xóa vật phẩm: " + safeText(selectedItemLocal.getItemName()));
        dialog.setContentText("Vui lòng nhập lý do xóa (*):");

        Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) {
            String reason = result.get().trim();

            if (isBlank(reason)) {
                showError("Bạn phải nhập lý do thì mới có thể xóa vật phẩm.");
                return;
            }

            // Gửi yêu cầu xóa lên Server (Server sẽ chuyển trạng thái của item thành INACTIVE trong DB)
            SocketResponse response = itemApi.deleteItem(selectedItemLocal.getItemId(), reason);

            if (!isSuccessful(response)) {
                showError(response == null ? "Xóa thất bại. Server không phản hồi hợp lệ." : response.getMessage());
                return;
            }

            // =========================================================================
            // GIẢI PHÁP CHO DTO BẤT BIẾN (IMMUTABLE):
            // Gọi hàm refresh để kéo dữ liệu trạng thái INACTIVE mới tinh từ Server về bảng
            // =========================================================================
            refreshSellerItems(false);

            // Đồng bộ đóng form bên phải nếu đang chọn xem chính sản phẩm vừa xóa
            if (this.selectedItem != null && selectedItemLocal.getItemId().equals(this.selectedItem.getItemId())) {
                if (formEditContainer != null) { formEditContainer.setVisible(false); formEditContainer.setManaged(false); }
                if (auctionConfigContainer != null) { auctionConfigContainer.setVisible(false); auctionConfigContainer.setManaged(false); }

                this.originalFormText = "";
                this.currentEditingItem = null;
                this.selectedItem = null;

                checkAndCollapseRightContainer();
            }

            showInfo(response.getMessage());
            showMessage("Sản phẩm đã được chuyển sang trạng thái ngừng hoạt động (INACTIVE).");

            // Chạy lại bộ lọc hiển thị dựa trên chế độ xem hiện tại (Ẩn nếu chọn "Chỉ ACTIVE", hiện cuối bảng nếu chọn "Hiện tất cả")
            applyStatusFilterAndSort();

            if (sellerItemsTable != null) {
                sellerItemsTable.getSelectionModel().clearSelection();
                if (!sellerItemsTable.getItems().isEmpty()) {
                    sellerItemsTable.getSelectionModel().select(0);
                }
                sellerItemsTable.refresh();
            }
        }
    }

    /**
     * FXML action: tạo phiên đấu giá cho item đã chọn.
     */
    @FXML
    private void handleCreateAuction() {
        String itemId = readItemId();
        if (isBlank(itemId)) {
            showError("Vui lòng nhập hoặc chọn itemId của vật phẩm.");
            return;
        }

        Double stepPrice = readStepPrice();
        if (stepPrice == null) {
            return;
        }

        LocalDateTime startTime = readDateTime(startTimeField, "thời gian bắt đầu");
        if (startTime == null) {
            return;
        }

        LocalDateTime endTime = readDateTime(endTimeField, "thời gian kết thúc");
        if (endTime == null) {
            return;
        }

        if (!endTime.isAfter(startTime)) {
            showError("Thời gian kết thúc phải sau thời gian bắt đầu.");
            return;
        }

        SocketResponse response = auctionApi.createAuction(
                itemId,
                stepPrice,
                startTime.toString(),
                endTime.toString()
        );

        if (!isSuccessful(response)) {
            showError(response == null ? "Server không trả về phản hồi hợp lệ." : response.getMessage());
            return;
        }

        showInfo(response.getMessage());
        showMessage("Tạo phiên đấu giá thành công.");

        // [ĐÃ CẬP NHẬT]: Tạo phiên đấu giá thành công thì đóng và thu gọn khối đấu giá độc lập
        if (auctionConfigContainer != null) {
            auctionConfigContainer.setVisible(false);
            auctionConfigContainer.setManaged(false);
        }

        if (stepPriceField != null) stepPriceField.clear();
        if (startTimeField != null) startTimeField.clear();
        if (endTimeField != null) endTimeField.clear();

        checkAndCollapseRightContainer();

        applyStatusFilterAndSort();
        if (sellerItemsTable != null) {
            sellerItemsTable.refresh();
        }

        refreshSellerItems(false);
    }

    /**
     * FXML action: điền nhanh thời gian gợi ý cho form auction.
     */
    @FXML
    private void handleUseDefaultTime() {
        fillDefaultTime();
        showMessage("Đã điền thời gian gợi ý.");
    }

    /**
     * FXML action cũ: giữ lại để tương thích FXML hiện tại.
     * Nếu muốn tách rõ, FXML mới nên dùng handleClearItemForm và handleClearAuctionForm.
     */
    @FXML
    private void handleClearForm() {
        handleClearItemForm();
        handleClearAuctionForm();
        showMessage("Đã làm mới form.");
    }

    /**
     * FXML action: clear riêng form tạo auction.
     */
    @FXML
    private void handleClearAuctionForm() {
        if (selectedItem == null) {
            clearText(itemIdField);
        }

        clearText(stepPriceField);
        fillDefaultTime();
        showMessage("Đã làm mới form tạo phiên đấu giá.");
    }

    /**
     * FXML action: dọn dẹp các trường nhập thô trong form tạo mới
     */
    private void handleClearItemForm() {
        clearText(itemNameField);
        clearText(startingPriceField);
        clearText(descriptionField);
        clearText(yearCreatedField);
        clearText(imageUrlField);
        if (itemTypeComboBox != null) itemTypeComboBox.getSelectionModel().clearSelection();

        clearText(painterField);
        clearText(artStyleField);
        clearText(brandField);
        clearText(warrantyMonthsField);
        clearText(modelField);
        clearText(engineTypeField);
        clearText(licensePlateField);
        clearText(kmAgeField);
    }

    /**
     * FXML action: quay về dashboard.
     */
    @FXML
    private void handleBack() {
        SceneNavigator.showDashboard();
    }

    /**
     * Cập nhật item đang chọn từ bảng hoặc sau khi create/detail/update.
     */
    public void setSelectedItem(ItemSummaryDTO item) {
        this.selectedItem = item;

        if (item == null) {
            return;
        }

        if (itemIdField != null) {
            itemIdField.setText(item.getItemId());
        }

        if (updateItemIdField != null) {
            updateItemIdField.setText(item.getItemId());
        }

        if (deleteItemIdField != null) {
            deleteItemIdField.setText(item.getItemId());
        }

        if (detailItemIdField != null) {
            detailItemIdField.setText(item.getItemId());
        }

        setLabelText(selectedItemIdLabel, "Item ID: " + safeText(item.getItemId()));
        setLabelText(itemNameLabel, "Vật phẩm: " + safeText(item.getItemName()));
        selectItemInTable(item.getItemId());
    }

    private void refreshSellerItems(boolean showResultMessage) {
        SocketResponse response = itemApi.getSellerItems();
        if (!isSuccessful(response)) {
            showError(response == null ? "Server không trả về phản hồi hợp lệ." : response.getMessage());
            return;
        }

        List<ItemSummaryDTO> allItems = itemApi.parseItemSummaryList(response);

        // CHUẨN HÓA: Đổ nguyên vẹn danh sách gốc từ Server trả về (giữ lại cả các item INACTIVE)
        sellerItems.setAll(allItems);

        // Chạy lại bộ lọc ẩn/hiện theo chế độ đang chọn (Chỉ hiện ACTIVE hoặc Hiện tất cả)
        applyStatusFilterAndSort();
        if (sellerItemsTable != null) {
            sellerItemsTable.refresh();
        }

        if (selectedItem != null) {
            selectItemInTable(selectedItem.getItemId());
        } else if (!sellerItems.isEmpty()) {
            setSelectedItem(sellerItems.get(0));
        }

        if (showResultMessage) {
            showMessage("Đã đồng bộ danh sách kho vật phẩm.");
        }
    }

    private void loadItemDetailIntoForm(String itemId, boolean showSuccessMessage) {
        SocketResponse response = itemApi.getItemDetail(itemId);
        if (!isSuccessful(response)) {
            showError(response == null ? "Server không trả về phản hồi hợp lệ." : response.getMessage());
            return;
        }

        ItemDetailDTO itemDetail = itemApi.parseItemDetail(response);
        if (itemDetail == null) {
            showError("Không đọc được chi tiết sản phẩm từ phản hồi server.");
            return;
        }

        fillItemForm(itemDetail);
        setSelectedItem(itemDetail.toSummaryDTO());

        if (showSuccessMessage) {
            showMessage("Đã tải chi tiết sản phẩm: " + safeText(itemDetail.getItemName()));
        }
    }

    /**
     * Fill toàn bộ dữ liệu từ ItemDetailDTO vào form Sửa đổi
     */
    private void fillItemForm(ItemDetailDTO item) {
        if (item == null) {
            return;
        }

        setText(itemIdField, item.getItemId());
        setText(updateItemIdField, item.getItemId());
        setText(deleteItemIdField, item.getItemId());
        setText(detailItemIdField, item.getItemId());

        // Đổ dữ liệu riêng biệt vào các trường của Form Chỉnh Sửa
        if (editItemTypeComboBox != null) {
            editItemTypeComboBox.setValue(normalizeItemType(item.getItemType()));
        }
        setText(editItemNameField, item.getItemName());
        setText(editStartingPriceField, numberToText(item.getStartingPrice()));
        setText(editDescriptionField, item.getDescription());
        setText(editYearCreatedField, numberToText(item.getYearCreated()));
        setText(editImageUrlField, item.getImageUrl());

        setText(editPainterField, item.getPainter());
        setText(editArtStyleField, item.getArtStyle());
        setText(editBrandField, item.getBrand());
        setText(editWarrantyMonthsField, numberToText(item.getWarrantyMonths()));
        setText(editModelField, item.getModel());
        setText(editEngineTypeField, item.getEngineType());
        setText(editLicensePlateField, item.getLicensePlate());
        setText(editKmAgeField, numberToText(item.getKmAge()));

        updateEditTypeSpecificFieldsVisibility(item.getItemType());
    }

    private CreateItemRequest buildCreateItemRequest() {
        String itemType = readItemType();
        String name = readText(itemNameField);

        if (isBlank(itemType)) {
            showError("Vui lòng chọn loại sản phẩm.");
            return null;
        }

        if (isBlank(name)) {
            showError("Vui lòng nhập tên sản phẩm.");
            return null;
        }

        Double startingPrice = readRequiredPositiveDouble(startingPriceField, "giá khởi điểm");
        if (startingPrice == null) {
            return null;
        }

        Integer yearCreated = readRequiredInteger(yearCreatedField, "năm tạo/sản xuất");
        if (yearCreated == null) {
            return null;
        }

        if (!validateTypeSpecificRequiredFields(itemType)) {
            return null;
        }

        Integer warrantyMonths = readOptionalInteger(warrantyMonthsField, "số tháng bảo hành");
        Double kmAge = readOptionalPositiveDouble(kmAgeField, "số km đã đi");

        if (hasInvalidOptionalNumber(warrantyMonthsField, warrantyMonths)
                || hasInvalidOptionalNumber(kmAgeField, kmAge)) {
            return null;
        }

        return new CreateItemRequest(
                itemType,
                name,
                startingPrice,
                readText(descriptionField),
                yearCreated,
                readText(imageUrlField),
                readText(painterField),
                readText(artStyleField),
                readText(brandField),
                warrantyMonths,
                readText(modelField),
                readText(engineTypeField),
                readText(licensePlateField),
                kmAge
        );
    }

    private UpdateItemRequest buildUpdateItemRequest() {
        String itemId = firstNonBlank(readText(updateItemIdField), readItemId());
        if (isBlank(itemId)) {
            showError("Vui lòng nhập hoặc chọn itemId cần cập nhật.");
            return null;
        }

        String itemType = firstNonBlank(
                readItemType(),
                selectedItem == null ? null : selectedItem.getItemType()
        );

        if (isBlank(itemType)) {
            showError("Vui lòng chọn loại sản phẩm để server xác thực.");
            return null;
        }

        Double startingPrice = readOptionalPositiveDouble(startingPriceField, "giá khởi điểm");
        Integer yearCreated = readOptionalInteger(yearCreatedField, "năm tạo/sản xuất");
        Integer warrantyMonths = readOptionalInteger(warrantyMonthsField, "số tháng bảo hành");
        Double kmAge = readOptionalPositiveDouble(kmAgeField, "số km đã đi");

        if (hasInvalidOptionalNumber(startingPriceField, startingPrice)
                || hasInvalidOptionalNumber(yearCreatedField, yearCreated)
                || hasInvalidOptionalNumber(warrantyMonthsField, warrantyMonths)
                || hasInvalidOptionalNumber(kmAgeField, kmAge)) {
            return null;
        }

        return new UpdateItemRequest(
                itemId,
                itemType,
                readText(itemNameField),
                startingPrice,
                readText(descriptionField),
                yearCreated,
                readText(imageUrlField),
                readText(painterField),
                readText(artStyleField),
                readText(brandField),
                warrantyMonths,
                readText(modelField),
                readText(engineTypeField),
                readText(licensePlateField),
                kmAge
        );
    }

    private UpdateItemRequest buildUpdateItemRequestFromEditForm() {
        String itemId = firstNonBlank(readText(updateItemIdField), readItemId());
        if (isBlank(itemId)) {
            showError("Vui lòng chọn sản phẩm cần cập nhật từ danh sách.");
            return null;
        }

        String itemType = editItemTypeComboBox != null ? editItemTypeComboBox.getValue() : null;
        if (isBlank(itemType) && selectedItem != null) {
            itemType = selectedItem.getItemType();
        }

        if (isBlank(itemType)) {
            showError("Vui lòng chọn loại sản phẩm.");
            return null;
        }

        Double startingPrice = readOptionalPositiveDouble(editStartingPriceField, "giá khởi điểm");
        Integer yearCreated = readOptionalInteger(editYearCreatedField, "năm tạo/sản xuất");
        Integer warrantyMonths = readOptionalInteger(editWarrantyMonthsField, "số tháng bảo hành");
        Double kmAge = readOptionalPositiveDouble(editKmAgeField, "số km đã đi");

        if (hasInvalidOptionalNumber(editStartingPriceField, startingPrice)
                || hasInvalidOptionalNumber(editYearCreatedField, yearCreated)
                || hasInvalidOptionalNumber(editWarrantyMonthsField, warrantyMonths)
                || hasInvalidOptionalNumber(editKmAgeField, kmAge)) {
            return null;
        }

        return new UpdateItemRequest(
                itemId,
                itemType,
                readText(editItemNameField),
                startingPrice,
                readText(editDescriptionField),
                yearCreated,
                readText(editImageUrlField),
                readText(editPainterField),
                readText(editArtStyleField),
                readText(editBrandField),
                warrantyMonths,
                readText(editModelField),
                readText(editEngineTypeField),
                readText(editLicensePlateField),
                kmAge
        );
    }

    private boolean validateTypeSpecificRequiredFields(String itemType) {
        String normalizedType = normalizeItemType(itemType);

        if ("ART".equals(normalizedType)) {
            if (isBlank(readText(painterField))) {
                showError("Vui lòng nhập họa sĩ/tác giả.");
                return false;
            }
            if (isBlank(readText(artStyleField))) {
                showError("Vui lòng nhập phong cách nghệ thuật.");
                return false;
            }
        }

        if ("ELECTRONICS".equals(normalizedType)) {
            if (isBlank(readText(brandField))) {
                showError("Vui lòng nhập thương hiệu.");
                return false;
            }
            if (readRequiredInteger(warrantyMonthsField, "số tháng bảo hành") == null) {
                return false;
            }
        }

        if ("VEHICLES".equals(normalizedType)) {
            if (isBlank(readText(modelField))) {
                showError("Vui lòng nhập dòng xe/model.");
                return false;
            }
            if (isBlank(readText(engineTypeField))) {
                showError("Vui lòng nhập loại động cơ.");
                return false;
            }
            if (isBlank(readText(licensePlateField))) {
                showError("Vui lòng nhập biển số.");
                return false;
            }
            if (readRequiredPositiveDouble(kmAgeField, "số km đã đi") == null) {
                return false;
            }
        }

        return true;
    }

    private void updateTypeSpecificFieldsVisibility(String itemType) {
        String normalizedType = normalizeItemType(itemType);

        setNodeVisible(artFieldsBox, "ART".equals(normalizedType));
        setNodeVisible(electronicsFieldsBox, "ELECTRONICS".equals(normalizedType));
        setNodeVisible(vehicleFieldsBox, "VEHICLES".equals(normalizedType));
    }

    private void selectItemInTable(String itemId) {
        if (sellerItemsTable == null || isBlank(itemId)) {
            return;
        }

        for (ItemSummaryDTO item : sellerItems) {
            if (itemId.equals(item.getItemId())) {
                sellerItemsTable.getSelectionModel().select(item);
                return;
            }
        }
    }

    private String readItemId() {
        if (selectedItem != null && !isBlank(selectedItem.getItemId())) {
            return selectedItem.getItemId();
        }

        return readText(itemIdField);
    }

    private String readItemType() {
        if (itemTypeComboBox != null && !isBlank(itemTypeComboBox.getValue())) {
            return itemTypeComboBox.getValue();
        }

        return readText(itemTypeField);
    }

    private void setItemTypeValue(String itemType) {
        String normalizedType = normalizeItemType(itemType);

        if (itemTypeComboBox != null) {
            itemTypeComboBox.setValue(normalizedType);
        }

        setText(itemTypeField, normalizedType);
    }

    private String normalizeItemType(String itemType) {
        if (isBlank(itemType)) {
            return null;
        }

        String normalizedType = itemType.trim().toUpperCase();
        return "VEHICLE".equals(normalizedType) ? "VEHICLES" : normalizedType;
    }

    private Double readStepPrice() {
        if (stepPriceField == null || isBlank(stepPriceField.getText())) {
            showError("Vui lòng nhập bước giá.");
            return null;
        }

        try {
            String rawStepPrice = stepPriceField.getText().trim().replace(",", ".");
            double stepPrice = Double.parseDouble(rawStepPrice);

            if (stepPrice <= 0) {
                showError("Bước giá phải lớn hơn 0.");
                return null;
            }

            return stepPrice;
        } catch (NumberFormatException e) {
            showError("Bước giá không hợp lệ.");
            return null;
        }
    }

    private LocalDateTime readDateTime(TextField field, String fieldName) {
        if (field == null || isBlank(field.getText())) {
            showError("Vui lòng nhập " + fieldName + ".");
            return null;
        }

        try {
            return LocalDateTime.parse(field.getText().trim());
        } catch (DateTimeParseException e) {
            showError(fieldName + " không hợp lệ. Ví dụ đúng: 2026-05-20T10:30:00.");
            return null;
        }
    }

    private void fillDefaultTimeIfEmpty() {
        if ((startTimeField == null || isBlank(startTimeField.getText()))
                && (endTimeField == null || isBlank(endTimeField.getText()))) {
            fillDefaultTime();
        }
    }

    private void fillDefaultTime() {
        LocalDateTime startTime = LocalDateTime.now()
                .plusMinutes(5)
                .withSecond(0)
                .withNano(0);

        LocalDateTime endTime = startTime.plusHours(1);

        setText(startTimeField, startTime.toString());
        setText(endTimeField, endTime.toString());
    }

    private String readText(TextField field) {
        return field == null ? null : field.getText();
    }

    private void setText(TextField field, String value) {
        if (field != null) {
            field.setText(safeText(value));
        }
    }

    private void clearText(TextField field) {
        if (field != null) {
            field.clear();
        }
    }

    private String firstNonBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private Double readRequiredPositiveDouble(TextField field, String fieldName) {
        String value = readText(field);
        if (isBlank(value)) {
            showError("Vui lòng nhập " + fieldName + ".");
            return null;
        }
        return parsePositiveDouble(value, fieldName);
    }

    private Double readOptionalPositiveDouble(TextField field, String fieldName) {
        String value = readText(field);
        if (isBlank(value)) {
            return null;
        }
        return parsePositiveDouble(value, fieldName);
    }

    private Double parsePositiveDouble(String value, String fieldName) {
        try {
            double number = Double.parseDouble(value.trim().replace(",", "."));
            if (number < 0) {
                showError(fieldName + " phải lớn hơn hoặc bằng 0.");
                return null;
            }
            return number;
        } catch (NumberFormatException e) {
            showError(fieldName + " không hợp lệ.");
            return null;
        }
    }

    private Integer readRequiredInteger(TextField field, String fieldName) {
        String value = readText(field);
        if (isBlank(value)) {
            showError("Vui lòng nhập " + fieldName + ".");
            return null;
        }
        return parseInteger(value, fieldName);
    }

    private Integer readOptionalInteger(TextField field, String fieldName) {
        String value = readText(field);
        if (isBlank(value)) {
            return null;
        }
        return parseInteger(value, fieldName);
    }

    private Integer parseInteger(String value, String fieldName) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            showError(fieldName + " không hợp lệ.");
            return null;
        }
    }

    private boolean hasInvalidOptionalNumber(TextField field, Number parsedValue) {
        return field != null && !isBlank(field.getText()) && parsedValue == null;
    }

    private boolean isSuccessful(SocketResponse response) {
        return response != null && response.isSuccess();
    }

    private void setNodeVisible(Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    private void setLabelText(Label label, String text) {
        if (label != null) {
            label.setText(safeText(text));
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String numberToText(Number value) {
        return value == null ? "" : String.valueOf(value);
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
        alert.setTitle("Lỗi");
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

    // Hàm tiện ích kiểm tra xem người dùng đã gõ bất kỳ ký tự nào vào một danh sách các ô TextField chưa
    private boolean isAnyFieldFilled(TextField... fields) {
        for (TextField field : fields) {
            if (field != null && field.getText() != null && !field.getText().trim().isEmpty()) {
                return true; // Chỉ cần có ít nhất 1 ô có dữ liệu
            }
        }
        return false;
    }

    // Hàm hiển thị hộp thoại xác nhận hủy thao tác
    private boolean confirmExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận hủy bỏ");
        alert.setHeaderText("Bạn đã nhập dữ liệu dở dang.");
        alert.setContentText("Bạn có chắc chắn muốn thoát và xóa sạch các dữ liệu đã nhập không?");

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    @FXML
    private void handleOpenFormForAuction() {
        var selectedItem = sellerItemsTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setContentText("Vui lòng chọn một vật phẩm từ bảng danh sách trước khi tạo phiên đấu giá!");
            alert.showAndWait();
            return;
        }
        rightSplitPaneContainer.setVisible(true); rightSplitPaneContainer.setManaged(true);
        auctionConfigContainer.setVisible(true); auctionConfigContainer.setManaged(true);

        formCreateContainer.setVisible(false); formCreateContainer.setManaged(false);
        formEditContainer.setVisible(false); formEditContainer.setManaged(false);

        // Hiển thị thông tin Item lên tiêu đề form đấu giá để người dùng không bị nhầm lẫn
        if(auctionItemIdLabel != null) auctionItemIdLabel.setText("Item ID: " + selectedItem.getItemId());
        if(auctionItemNameLabel != null) auctionItemNameLabel.setText("Vật phẩm: " + selectedItem.getItemName());
        fillDefaultTimeIfEmpty();
    }

    @FXML
    private void handleCloseAuctionForm() {
        // Chỉ hỏi khi người dùng đã gõ bất kỳ chữ nào vào ô nhập của form đấu giá
        if (isAnyFieldFilled(stepPriceField, startTimeField, endTimeField)) {
            if (!confirmExit()) {
                return; // Chọn không -> Giữ nguyên form
            }
        }

        // Tiến hành ẩn form đấu giá
        if (auctionConfigContainer != null) {
            auctionConfigContainer.setVisible(false);
            auctionConfigContainer.setManaged(false);
        }

        // Clear dữ liệu nhập dở
        if (stepPriceField != null) stepPriceField.clear();

        // Thu gọn container bên phải
        checkAndCollapseRightContainer();
    }
}