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
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import com.auction.dto.AuctionSummaryDTO;
import javafx.scene.control.TableColumn;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Bộ điều khiển (Controller) hoặc lớp tiện ích SellerItemController xử lý giao diện Client JavaFX.
 */
public class SellerItemController {
    private final ClientAuctionApi auctionApi = new ClientAuctionApi();
    private final ClientItemApi itemApi = new ClientItemApi();

    private final ObservableList<ItemSummaryDTO> allServerItems = FXCollections.observableArrayList();
    private final ObservableList<ItemSummaryDTO> sellerItems = FXCollections.observableArrayList();
    private ItemSummaryDTO selectedItem;
    private javafx.collections.transformation.FilteredList<ItemSummaryDTO> filteredItems;
    private boolean showAllStatusesMode = true;
    private boolean isRefreshing = false;
    private boolean isNameAscending = true;
    private boolean isPriceAscending = true;
    private boolean isTypeAscending = true;

    private int currentSellerPage = 1;
    private int totalSellerPages = 0;
    private static final int DEFAULT_SELLER_PAGE_SIZE = 10;

    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private TextField pageSizeField;
    @FXML private Label pageLabel;
    @FXML private Button previousPageButton;
    @FXML private Button nextPageButton;
    @FXML private Button refreshButton;

    @FXML private ScrollPane rightSplitPaneContainer;
    @FXML private VBox formCreateContainer;
    @FXML private VBox formEditContainer;

    @FXML private StackPane rootContainer;
    @FXML private Label dynamicTitleLabel;
    @FXML private Label formTitleLabel;
    @FXML private Label selectedItemIdLabel;
    @FXML private Label itemNameLabel;
    @FXML private Label messageLabel;
    @FXML private Button btnSubmit;

    @FXML private Label headerAvailableBalance;
    @FXML private Label headerFrozenBalance;
    @FXML private Label headerTotalBalance;

    @FXML private TableView<ItemSummaryDTO> sellerItemsTable;
    @FXML private TableColumn<ItemSummaryDTO, String> itemIdColumn;
    @FXML private TableColumn<ItemSummaryDTO, String> itemNameColumn;
    @FXML private TableColumn<ItemSummaryDTO, String> itemTypeColumn;
    @FXML private TableColumn<ItemSummaryDTO, Number> startingPriceColumn;
    @FXML private TableColumn<ItemSummaryDTO, String> statusColumn;

    @FXML private ComboBox<String> itemTypeComboBox;
    @FXML private TextField itemTypeField;
    @FXML private TextField itemNameField;
    @FXML private TextField startingPriceField;
    @FXML private TextField descriptionField;
    @FXML private TextField yearCreatedField;
    @FXML private TextField imageUrlField;

    @FXML private VBox artFieldsBox;
    @FXML private TextField painterField;
    @FXML private TextField artStyleField;

    @FXML private VBox electronicsFieldsBox;
    @FXML private TextField brandField;
    @FXML private TextField warrantyMonthsField;

    @FXML private VBox vehicleFieldsBox;
    @FXML private TextField modelField;
    @FXML private TextField engineTypeField;
    @FXML private TextField licensePlateField;
    @FXML private TextField kmAgeField;

    @FXML private TextField updateItemIdField;
    @FXML private TextField deleteItemIdField;
    @FXML private TextField detailItemIdField;

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

    @FXML private TextField itemIdField;
    @FXML private TextField stepPriceField;
    @FXML private TextField startTimeField;
    @FXML private TextField endTimeField;

    @FXML private VBox auctionConfigContainer;
    @FXML private Label auctionItemIdLabel;
    @FXML private Label auctionItemNameLabel;

    private com.auction.dto.ItemSummaryDTO currentEditingItem = null;

    /**
     * Thực hiện tính toán phân trang dựa trên danh sách dữ liệu đã qua bộ lọc (Search + Status)
     */
    private void applyStatusFilterAndSort() {
        if (filteredItems == null) return;

        String keyword = (searchField != null) ? searchField.getText().trim().toLowerCase() : "";

        filteredItems.setPredicate(item -> {
            if (item == null) return false;

            boolean matchesStatus = true;
            if (item.getStatus() != null) {
                String status = item.getStatus().toUpperCase();
                if (!showAllStatusesMode) {
                    matchesStatus = "ACTIVE".equals(status);
                }
            } else {
                matchesStatus = false;
            }

            boolean matchesKeyword = true;
            if (!keyword.isEmpty()) {
                String itemName = item.getItemName() == null ? "" : item.getItemName().toLowerCase();
                matchesKeyword = itemName.contains(keyword);
            }

            return matchesStatus && matchesKeyword;
        });

        int pageSize = DEFAULT_SELLER_PAGE_SIZE;
        if (pageSizeField != null && !pageSizeField.getText().trim().isEmpty()) {
            try {
                pageSize = Integer.parseInt(pageSizeField.getText().trim());
                if (pageSize <= 0) pageSize = DEFAULT_SELLER_PAGE_SIZE;
            } catch (NumberFormatException ignored) {}
        }

        int totalFilteredCount = filteredItems.size();
        if (totalFilteredCount > 0) {
            totalSellerPages = (int) Math.ceil((double) totalFilteredCount / pageSize);
            if (currentSellerPage > totalSellerPages) {
                currentSellerPage = totalSellerPages;
            }
            if (currentSellerPage <= 0) currentSellerPage = 1;
        } else {
            totalSellerPages = 1;
            currentSellerPage = 1;
        }

        int fromIndex = (currentSellerPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalFilteredCount);

        List<ItemSummaryDTO> pageItems = new ArrayList<>();
        if (fromIndex < totalFilteredCount) {
            pageItems = filteredItems.subList(fromIndex, toIndex);
        }
        sellerItems.setAll(pageItems);

        SortedList<ItemSummaryDTO> sortedItems = new SortedList<>(sellerItems);

        if (sellerItemsTable.getSortOrder().isEmpty()) {
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
            TableColumn<ItemSummaryDTO, ?> currentSortColumn = sellerItemsTable.getSortOrder().get(0);

            if (currentSortColumn == itemNameColumn) {
                sortedItems.setComparator((item1, item2) -> {
                    if (item1 == null || item2 == null) return 0;
                    String name1 = item1.getItemName() == null ? "" : item1.getItemName();
                    String name2 = item2.getItemName() == null ? "" : item2.getItemName();
                    int comp = name1.compareToIgnoreCase(name2);
                    return isNameAscending ? comp : -comp;
                });
            }
            else if (currentSortColumn == startingPriceColumn) {
                sortedItems.setComparator((item1, item2) -> {
                    if (item1 == null || item2 == null) return 0;
                    int comp = Double.compare(item1.getStartingPrice(), item2.getStartingPrice());
                    return isPriceAscending ? comp : -comp;
                });
            }
            else if (currentSortColumn == itemTypeColumn) {
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
        updatePageUI();
    }

    private AuctionSummaryDTO selectedAuction;

    @FXML
    public void initialize() {
        com.auction.util.HeaderBalanceHelper.setupHeaderBalance(headerAvailableBalance, headerFrozenBalance, headerTotalBalance);
        initializeItemTypeControl();
        initializeSellerItemsTable();
        fillDefaultTimeIfEmpty();

        applyTheme();

        if (rightSplitPaneContainer != null) { rightSplitPaneContainer.setVisible(false); rightSplitPaneContainer.setManaged(false); }
        if (formCreateContainer != null) { formCreateContainer.setVisible(false); formCreateContainer.setManaged(false); }
        if (formEditContainer != null) { formEditContainer.setVisible(false); formEditContainer.setManaged(false); }
        if (auctionConfigContainer != null) { auctionConfigContainer.setVisible(false); auctionConfigContainer.setManaged(false); }

        updateTypeSpecificFieldsVisibility(readItemType());

        loadSellerItems(1, true);

        startSellerAutoRefresh();
    }
    private javafx.animation.Timeline sellerAutoRefreshTimeline;
    private void startSellerAutoRefresh() {
        if (sellerAutoRefreshTimeline != null) {
            return;
        }

        sellerAutoRefreshTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(5), event -> loadSellerItems(currentSellerPage, false))
        );
        sellerAutoRefreshTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        sellerAutoRefreshTimeline.play();
    }

    private void stopSellerAutoRefresh() {
        if (sellerAutoRefreshTimeline != null) {
            sellerAutoRefreshTimeline.stop();
            sellerAutoRefreshTimeline = null;
        }
    }
    /**
     * Áp dụng theme hiện tại của ứng dụng dựa trên cấu hình hệ thống toàn cục.
     */
    private void applyTheme() {
        if (rootContainer == null) {
            return;
        }

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

        if (SceneNavigator.isAppDarkMode) {
            setLabelText(dynamicTitleLabel, "QUẢN LÝ VẬT PHẨM");
            showMessage("● SYSTEM DARK MODE SIGNED ⚡");
        } else {
            setLabelText(dynamicTitleLabel, "QUẢN LÝ VẬT PHẨM");
            showMessage("✓ Hệ thống đã sẵn sàng.");
        }

        if (sellerItemsTable != null) {
            javafx.scene.layout.VBox placeholderBox = new javafx.scene.layout.VBox();
            placeholderBox.setAlignment(javafx.geometry.Pos.CENTER);
            placeholderBox.setSpacing(10);

            javafx.scene.control.Label iconLabel = new javafx.scene.control.Label();
            iconLabel.getStyleClass().add("placeholder-icon");

            javafx.scene.control.Label textLabel = new javafx.scene.control.Label();
            textLabel.getStyleClass().add("placeholder-text");

            if (SceneNavigator.isAppDarkMode) {
                iconLabel.setText("🏴‍☠️🔨");
                textLabel.setText("Hàng tồn hiện không còn trong kho");
            } else {
                iconLabel.setText("📦");
                textLabel.setText("Không có sản phẩm nào trong danh sách");
            }

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

        filteredItems = new javafx.collections.transformation.FilteredList<>(allServerItems, p -> true);
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
                event.consume();

                if (!sellerItemsTable.getSortOrder().isEmpty()) {
                    TableColumn<ItemSummaryDTO, ?> col = sellerItemsTable.getSortOrder().get(0);

                    if (col == itemNameColumn) isNameAscending = !isNameAscending;
                    if (col == startingPriceColumn) isPriceAscending = !isPriceAscending;
                    if (col == itemTypeColumn) isTypeAscending = !isTypeAscending;

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

            statusColumn.setGraphic(new Label("Trạng Thái 🔄"));
            statusColumn.getGraphic().setStyle("-fx-cursor: hand; -fx-text-fill: -fx-primary-color;");
            statusColumn.setText("");
            statusColumn.setSortable(false);

            statusColumn.getGraphic().setOnMouseClicked(event -> {
                showAllStatusesMode = !showAllStatusesMode;

                currentSellerPage = 1;
                applyStatusFilterAndSort();

                if (!showAllStatusesMode) {
                    showMessage("Chế độ: Chỉ hiển thị các sản phẩm ACTIVE");

                    if (this.selectedItem != null && !"ACTIVE".equalsIgnoreCase(this.selectedItem.getStatus())) {
                        if (formEditContainer != null) { formEditContainer.setVisible(false); formEditContainer.setManaged(false); }
                        if (auctionConfigContainer != null) { auctionConfigContainer.setVisible(false); auctionConfigContainer.setManaged(false); }
                        checkAndCollapseRightContainer();
                        this.originalFormText = "";
                        this.currentEditingItem = null;
                    }
                } else {
                    showMessage("Chế độ: Hiện tất cả trạng thái");
                }

                if (sellerItemsTable != null && !sellerItemsTable.getItems().isEmpty()) {
                    var firstVisibleItem = sellerItemsTable.getItems().get(0);
                    sellerItemsTable.getSelectionModel().select(firstVisibleItem);
                } else {
                    this.selectedItem = null;
                }

                sellerItemsTable.refresh();
            });
        }

        sellerItemsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (isRefreshing) {
                return;
            }

            if (newItem == null) return;

            if (auctionConfigContainer != null && auctionConfigContainer.isVisible()) {
                if (stepPriceField != null && !stepPriceField.getText().trim().isEmpty()) {
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

        formEditContainer.setVisible(false);
        formEditContainer.setManaged(false);
        if (auctionConfigContainer != null) {
            auctionConfigContainer.setVisible(false);
            auctionConfigContainer.setManaged(false);
        }
    }

    private String originalFormText = "";

    @FXML
    private void handleOpenFormForEdit() {
        var selectedItem = sellerItemsTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Thông báo");
            alert.setHeaderText(null);
            alert.setContentText("Vui lòng chọn một vật phẩm từ bảng danh sách trước khi thực hiện sửa!");
            alert.showAndWait();
            return;
        }

        String status = selectedItem.getStatus();
        if (!"ACTIVE".equalsIgnoreCase(status)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Không thể chỉnh sửa");
            alert.setHeaderText(null);
            alert.setContentText("Chỉ có thể chỉnh sửa sản phẩm đang hoạt động (ACTIVE).\nVật phẩm này đang ở trạng thái: " + status);
            alert.showAndWait();
            return;
        }

        this.currentEditingItem = selectedItem;

        activateEditFormWorkflow(selectedItem);
    }

    /**
     * Hàm dùng chung xử lý đóng form Tạo và trượt form Sửa ra kèm load dữ liệu
     */
    private void activateEditFormWorkflow(ItemSummaryDTO item) {
        setNodeVisible(formCreateContainer, false);
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
        if (isAnyFieldFilled(itemNameField, startingPriceField, yearCreatedField, descriptionField, imageUrlField, painterField, artStyleField, brandField, warrantyMonthsField, modelField, engineTypeField, licensePlateField, kmAgeField)) {
            if (!confirmExit()) {
                return;
            }
        }

        rightSplitPaneContainer.setVisible(false);
        rightSplitPaneContainer.setManaged(false);
        formCreateContainer.setVisible(false);
        formCreateContainer.setManaged(false);

        handleClearItemForm();
        handleLoadSellerItems();
    }

    /**
     * Nút (X) trên Form Sửa: Có cảnh báo nếu phát hiện người dùng đã chỉnh sửa dữ liệu dở dang chưa lưu.
     */
    @FXML
    private void handleCloseEditForm() {
        if (formEditContainer == null || !formEditContainer.isVisible()) {
            return;
        }

        String currentFormText = (editItemNameField.getText() == null ? "" : editItemNameField.getText()) + "|"
                + (editStartingPriceField.getText() == null ? "" : editStartingPriceField.getText()) + "|"
                + (editYearCreatedField.getText() == null ? "" : editYearCreatedField.getText()) + "|"
                + (editDescriptionField.getText() == null ? "" : editDescriptionField.getText()) + "|"
                + (editImageUrlField.getText() == null ? "" : editImageUrlField.getText());

        if (!currentFormText.equals(this.originalFormText)) {
            if (!confirmExit()) {
                return;
            }
        }

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

    /**
     * FXML action: tải danh sách item của seller đang đăng nhập.
     */
    @FXML
    private void handleLoadSellerItems() {
        loadSellerItems(currentSellerPage, true);
    }

    @FXML
    private void handleSearch() {
        currentSellerPage = 1;
        applyStatusFilterAndSort();
        showMessage("Kết quả tìm kiếm cho từ khóa: " + searchField.getText().trim());
    }

    @FXML
    private void handleClearSearch() {
        if (searchField != null) {
            searchField.clear();
        }
        currentSellerPage = 1;
        applyStatusFilterAndSort();
        showMessage("Đã xóa bộ lọc tìm kiếm.");
    }

    @FXML
    private void handlePreviousPage() {
        if (currentSellerPage <= 1) return;
        currentSellerPage--;
        applyStatusFilterAndSort();
    }

    @FXML
    private void handleNextPage() {
        if (totalSellerPages > 0 && currentSellerPage >= totalSellerPages) return;
        currentSellerPage++;
        applyStatusFilterAndSort();
    }

    /**
     * Tải danh sách vật phẩm từ database Server và đưa vào danh sách tổng.
     */
    private void loadSellerItems(int page, boolean showResultMessage) {
        if (showResultMessage && messageLabel != null) {
            messageLabel.setText("Đang kết nối đồng bộ kho dữ liệu phân trang...");
        }

        Task<SocketResponse> task = new Task<>() {
            @Override
            protected SocketResponse call() throws Exception {
                return itemApi.getSellerItems();
            }
        };

        task.setOnSucceeded(event -> {
            SocketResponse response = task.getValue();
            if (!isSuccessful(response)) {
                showError(response == null ? "Server không trả về phản hồi hợp lệ." : response.getMessage());
                return;
            }

            List<ItemSummaryDTO> allItems = itemApi.parseItemSummaryList(response);

            isRefreshing = true;

            allServerItems.setAll(allItems);

            currentSellerPage = page;

            applyStatusFilterAndSort();

            if (selectedItem != null) {
                selectItemInTable(selectedItem.getItemId());
            } else if (!sellerItems.isEmpty()) {
                setSelectedItem(sellerItems.get(0));
            }

            isRefreshing = false;

            if (showResultMessage) {
                showMessage("Đã đồng bộ danh sách kho vật phẩm (Trang " + currentSellerPage + ").");
            }
        });

        task.setOnFailed(event -> {
            showError("Lỗi luồng xử lý dữ liệu ngầm phân trang.");
        });

        Thread thread = new Thread(task, "seller-item-paging-worker");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Cập nhật trạng thái hiển thị của các nút phân trang và nhãn chỉ số trang.
     */
    private void updatePageUI() {
        if (pageLabel != null) {
            pageLabel.setText("Trang " + currentSellerPage + " / " + Math.max(totalSellerPages, 1));
        }
        if (previousPageButton != null) {
            previousPageButton.setDisable(currentSellerPage <= 1);
        }
        if (nextPageButton != null) {
            nextPageButton.setDisable(totalSellerPages <= 0 || currentSellerPage >= totalSellerPages);
        }
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

        loadSellerItems(currentSellerPage, false);
        showInfo(response.getMessage());
        showMessage("Tạo sản phẩm thành công.");

        if (formCreateContainer != null) {
            formCreateContainer.setVisible(false);
            formCreateContainer.setManaged(false);
        }
        handleClearItemForm();
        checkAndCollapseRightContainer();
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

        loadSellerItems(currentSellerPage, false);
        showInfo(response.getMessage());
        showMessage("Cập nhật sản phẩm thành công.");

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

        String currentStatus = selectedItemLocal.getStatus() == null ? "" : selectedItemLocal.getStatus().toUpperCase();
        if (!"ACTIVE".equals(currentStatus) && !"SOLD".equals(currentStatus)) {
            showError("Chỉ có thể xóa sản phẩm đang hoạt động (ACTIVE) hoặc đã bán (SOLD). Sản phẩm mang trạng thái " + currentStatus + " không thể xóa!");
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

            SocketResponse response = itemApi.deleteItem(selectedItemLocal.getItemId(), reason);

            if (!isSuccessful(response)) {
                showError(response == null ? "Xóa thất bại. Server không phản hồi hợp lệ." : response.getMessage());
                return;
            }

            loadSellerItems(currentSellerPage, false);

            if (this.selectedItem != null && selectedItemLocal.getItemId().equals(this.selectedItem.getItemId())) {
                if (formEditContainer != null) { formEditContainer.setVisible(false); formEditContainer.setManaged(false); }
                if (auctionConfigContainer != null) { auctionConfigContainer.setVisible(false); auctionConfigContainer.setManaged(false); }

                this.originalFormText = "";
                this.currentEditingItem = null;
                this.selectedItem = null;

                checkAndCollapseRightContainer();
            }

            showInfo(response.getMessage());
            showMessage("Sản phẩm đã được xóa thành công.");

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

        // ĐỊNH NGHĨA: Bộ định dạng ngày/tháng/năm giờ:phút thân thiện
        java.time.format.DateTimeFormatter friendlyFormatter =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // CẬP NHẬT: Truyền thêm friendlyFormatter vào hàm đọc thời gian bắt đầu
        LocalDateTime startTime = readDateTimeWithFormatter(startTimeField, "thời gian bắt đầu", friendlyFormatter);
        if (startTime == null) {
            return;
        }

        // CẬP NHẬT: Truyền thêm friendlyFormatter vào hàm đọc thời gian kết thúc
        LocalDateTime endTime = readDateTimeWithFormatter(endTimeField, "thời gian kết thúc", friendlyFormatter);
        if (endTime == null) {
            return;
        }

        if (!endTime.isAfter(startTime)) {
            showError("Thời gian kết thúc phải sau thời gian bắt đầu.");
            return;
        }

        // Lệnh .toString() ở đây vẫn tự động biến đổi thành chuỗi ISO chuẩn để gửi lên API hoạt động thông suốt
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

        loadSellerItems(currentSellerPage, false);
    }

    private LocalDateTime readDateTimeWithFormatter(TextField field, String fieldName, java.time.format.DateTimeFormatter formatter) {
        if (field == null) return null;
        String text = field.getText().trim();
        if (text.isEmpty()) {
            showError("Vui lòng nhập " + fieldName + ".");
            return null;
        }
        try {
            // Dịch chuỗi nhập tay dd/MM/yyyy HH:mm thành đối tượng LocalDateTime
            return LocalDateTime.parse(text, formatter);
        } catch (java.time.format.DateTimeParseException e) {
            showError("Định dạng " + fieldName + " không hợp lệ! Vui lòng nhập đúng dạng dd/MM/yyyy HH:mm (Ví dụ: 04/06/2026 10:45)");
            return null;
        }
    }

    /**
     * FXML action: điền nhanh thời gian gợi ý cho form auction.
     */
    @FXML
    private void handleUseDefaultTime() {
        fillDefaultTime();
        showMessage("Đã điền thời gian gợi ý.");
    }

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
        stopSellerAutoRefresh();
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
        java.time.format.DateTimeFormatter friendlyFormatter =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        if (startTimeField != null && startTimeField.getText().trim().isEmpty()) {
            startTimeField.setText(LocalDateTime.now().plusMinutes(2).format(friendlyFormatter));
        }
        if (endTimeField != null && endTimeField.getText().trim().isEmpty()) {
            endTimeField.setText(LocalDateTime.now().plusDays(1).format(friendlyFormatter));
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

    private boolean isAnyFieldFilled(TextField... fields) {
        for (TextField field : fields) {
            if (field != null && field.getText() != null && !field.getText().trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

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
        var selectedItemLocal = sellerItemsTable.getSelectionModel().getSelectedItem();
        if (selectedItemLocal == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setContentText("Vui lòng chọn một vật phẩm từ bảng danh sách trước khi tạo phiên đấu giá!");
            alert.showAndWait();
            return;
        }

        String status = selectedItemLocal.getStatus() == null ? "" : selectedItemLocal.getStatus().toUpperCase();
        if (!"ACTIVE".equals(status)) {
            showError("Chỉ có thể tạo phiên đấu giá cho sản phẩm ACTIVE. Sản phẩm hiện tại đang là " + status + ".");
            return;
        }

        rightSplitPaneContainer.setVisible(true); rightSplitPaneContainer.setManaged(true);
        auctionConfigContainer.setVisible(true); auctionConfigContainer.setManaged(true);

        formCreateContainer.setVisible(false); formCreateContainer.setManaged(false);
        formEditContainer.setVisible(false); formEditContainer.setManaged(false);

        if(auctionItemIdLabel != null) auctionItemIdLabel.setText("Item ID: " + selectedItemLocal.getItemId());
        if(auctionItemNameLabel != null) auctionItemNameLabel.setText("Vật phẩm: " + selectedItemLocal.getItemName());
        fillDefaultTimeIfEmpty();
    }

    @FXML
    private void handleCloseAuctionForm() {
        if (isAnyFieldFilled(stepPriceField, startTimeField, endTimeField)) {
            if (!confirmExit()) {
                return;
            }
        }

        if (auctionConfigContainer != null) {
            auctionConfigContainer.setVisible(false);
            auctionConfigContainer.setManaged(false);
        }

        if (stepPriceField != null) stepPriceField.clear();

        checkAndCollapseRightContainer();
    }
}