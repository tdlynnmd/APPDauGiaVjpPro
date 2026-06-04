package com.auction.controller;

import com.auction.dto.AuctionSummaryDTO;
import com.auction.dto.SocketResponse;
import com.auction.network.ClientAuctionApi;
import com.auction.util.SceneNavigator;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Bộ điều khiển (Controller) hoặc lớp tiện ích AuctionListController xử lý giao diện Client JavaFX.
 */
public class AuctionListController {
    private final ClientAuctionApi auctionApi = new ClientAuctionApi();
    private Timeline autoRefreshTimeline;

    private final ObservableList<AuctionSummaryDTO> allServerAuctions = FXCollections.observableArrayList();
    private final ObservableList<AuctionSummaryDTO> paginatedAuctions = FXCollections.observableArrayList();
    private FilteredList<AuctionSummaryDTO> filteredAuctions;

    private int currentAuctionPage = 1;
    private int auctionPageSize = 10;

    @FXML private javafx.scene.layout.StackPane rootPane;
    @FXML private TableView<AuctionSummaryDTO> auctionTable;
    @FXML private TableColumn<AuctionSummaryDTO, String> itemNameColumn;
    @FXML private TableColumn<AuctionSummaryDTO, Number> currentPriceColumn;
    @FXML private TableColumn<AuctionSummaryDTO, String> statusColumn;
    @FXML private TableColumn<AuctionSummaryDTO, String> endTimeColumn;
    @FXML private TableColumn<AuctionSummaryDTO, String> startTimeColumn;
    @FXML private Label messageLabel;

    @FXML private Label headerAvailableBalance;
    @FXML private Label headerFrozenBalance;
    @FXML private Label headerTotalBalance;

    @FXML private TextField searchAuctionField;
    @FXML private TextField pageSizeField;
    @FXML private Button prevPageButton;
    @FXML private Button nextPageButton;
    @FXML private Label pageInfoLabel;

    @FXML
    public void initialize() {
        com.auction.util.HeaderBalanceHelper.setupHeaderBalance(headerAvailableBalance, headerFrozenBalance, headerTotalBalance);

        if (rootPane != null) {
            rootPane.getStylesheets().clear();
            String initialPath = SceneNavigator.isAppDarkMode
                    ? "/com/auction/client/view/dark.css"
                    : "/com/auction/client/view/light.css";
            try {
                String css = Objects.requireNonNull(getClass().getResource(initialPath)).toExternalForm();
                rootPane.getStylesheets().add(css);
            } catch (Exception e) {
                System.out.println("Không tìm thấy file CSS khởi tạo!");
            }
        }

        filteredAuctions = new FilteredList<>(allServerAuctions, p -> true);
        setupTableColumns();
        auctionTable.setItems(paginatedAuctions);

        if (pageSizeField != null) {
            pageSizeField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null || newVal.trim().isEmpty()) {
                    return;
                }
                try {
                    int parsedSize = Integer.parseInt(newVal.trim());
                    if (parsedSize > 0) {
                        this.auctionPageSize = parsedSize;
                        this.currentAuctionPage = 1;
                        applySearchFilterAndPagination();
                    }
                } catch (NumberFormatException e) {
                }
            });
        }

        if (searchAuctionField != null) {
            searchAuctionField.textProperty().addListener((obs, oldVal, newVal) -> {
                currentAuctionPage = 1;
                applySearchFilterAndPagination();
            });
        }

        loadActiveAuctions();
        updateTablePlaceholder(SceneNavigator.isAppDarkMode);

        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(4), event -> {
            if (auctionTable.getScene() == null || auctionTable.getScene().getWindow() == null) {
                autoRefreshTimeline.stop();
            } else {
                loadActiveAuctionsSilently();
            }
        }));
        autoRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        autoRefreshTimeline.play();
    }

    /**
     * Thuật toán bóc tách dữ liệu mảng độc lập
     */
    private void applySearchFilterAndPagination() {
        if (filteredAuctions == null) return;

        if (auctionTable != null) {
            auctionTable.getSelectionModel().clearSelection();
        }

        String keyword = (searchAuctionField != null) ? searchAuctionField.getText().trim().toLowerCase() : "";

        filteredAuctions.setPredicate(auction -> {
            if (keyword.isEmpty()) return true;
            boolean matchesName = auction.getItemName() != null && auction.getItemName().toLowerCase().contains(keyword);
            boolean matchesId = String.valueOf(auction.getAuctionId()).contains(keyword);
            return matchesName || matchesId;
        });

        SortedList<AuctionSummaryDTO> sortedAuctions = new SortedList<>(filteredAuctions);
        sortedAuctions.comparatorProperty().bind(auctionTable.comparatorProperty());

        java.util.List<AuctionSummaryDTO> activeList = new java.util.ArrayList<>();
        java.util.List<AuctionSummaryDTO> finishedList = new java.util.ArrayList<>();
        for (AuctionSummaryDTO a : sortedAuctions) {
            String status = a.getStatus();
            if ("RUNNING".equalsIgnoreCase(status) || "OPEN".equalsIgnoreCase(status)) {
                activeList.add(a);
            } else {
                finishedList.add(a);
            }
        }

        int activeCount = activeList.size();
        int activePages = (int) Math.ceil((double) activeCount / auctionPageSize);
        if (activePages == 0) activePages = 1;
        int totalCapacity = activePages * auctionPageSize;
        int finishedToShow = totalCapacity - activeCount;

        java.util.List<AuctionSummaryDTO> combinedList = new java.util.ArrayList<>(activeList);
        for (int i = 0; i < Math.min(finishedToShow, finishedList.size()); i++) {
            combinedList.add(finishedList.get(i));
        }

        int totalItems = combinedList.size();
        int totalPages = (int) Math.ceil((double) totalItems / auctionPageSize);
        if (totalPages == 0) totalPages = 1;

        if (currentAuctionPage > totalPages) currentAuctionPage = totalPages;
        if (currentAuctionPage < 1) currentAuctionPage = 1;

        int startIndex = (currentAuctionPage - 1) * auctionPageSize;
        int endIndex = Math.min(startIndex + auctionPageSize, totalItems);

        paginatedAuctions.clear();
        if (startIndex < totalItems && startIndex >= 0) {
            for (int i = startIndex; i < endIndex; i++) {
                paginatedAuctions.add(combinedList.get(i));
            }
        }

        if (pageInfoLabel != null) {
            pageInfoLabel.setText(String.format("Trang %d / %d", currentAuctionPage, totalPages));
        }
        if (prevPageButton != null) {
            prevPageButton.setDisable(currentAuctionPage == 1);
        }
        if (nextPageButton != null) {
            nextPageButton.setDisable(currentAuctionPage >= totalPages);
        }
    }

    @FXML
    private void handlePrevPage() {
        if (currentAuctionPage > 1) {
            currentAuctionPage--;
            applySearchFilterAndPagination();
        }
    }

    @FXML
    private void handleNextPage() {
        if (filteredAuctions == null) return;
        int totalItems = filteredAuctions.size();
        int totalPages = (int) Math.ceil((double) totalItems / auctionPageSize);
        if (currentAuctionPage < totalPages) {
            currentAuctionPage++;
            applySearchFilterAndPagination();
        }
    }

    @FXML
    private void handleSearchAction() {
        currentAuctionPage = 1;
        applySearchFilterAndPagination();
        showMessage("Đã lọc danh sách theo từ khóa tìm kiếm.");
    }

    @FXML
    private void handleRefresh() {
        if (pageSizeField != null && !pageSizeField.getText().trim().isEmpty()) {
            try {
                int parsedSize = Integer.parseInt(pageSizeField.getText().trim());
                if (parsedSize > 0) {
                    this.auctionPageSize = parsedSize;
                }
            } catch (NumberFormatException e) {
            }
        }

        loadActiveAuctions();
    }

    private void loadActiveAuctions() {
        showMessage("Đang làm mới danh sách...");
        SocketResponse response = auctionApi.getActiveAuctions();

        if (response != null && !response.isSuccess()) {
            showError(response.getMessage());
            return;
        }

        List<AuctionSummaryDTO> auctions = auctionApi.parseAuctionSummaryList(response);
        allServerAuctions.setAll(auctions);

        applySearchFilterAndPagination();

        if (allServerAuctions.isEmpty()) {
            showMessage("Hiện chưa có phiên đấu giá nào đang hoạt động.");
        } else {
            showMessage("Đã cập nhật. Tìm thấy tổng cộng " + allServerAuctions.size() + " phiên đấu giá.");
        }
    }

    private void loadActiveAuctionsSilently() {
        SocketResponse response = auctionApi.getActiveAuctions();
        if (response != null && response.isSuccess()) {
            List<AuctionSummaryDTO> auctions = auctionApi.parseAuctionSummaryList(response);
            allServerAuctions.setAll(auctions);
            applySearchFilterAndPagination();
        }
    }

    private void updateTablePlaceholder(boolean isDarkMode) {
        VBox emptyBox = new VBox(12);
        emptyBox.setStyle("-fx-alignment: center; -fx-padding: 30;");
        Label iconLabel = new Label();
        Label msgLabel = new Label();
        msgLabel.getStyleClass().add("status-message-label");

        if (isDarkMode) {
            iconLabel.setText("🏴‍☠️🔨");
            iconLabel.setStyle("-fx-font-size: 42px;");
            msgLabel.setText("Sàn đấu ngầm đang trống... Hãy ẩn mình chờ thời cuộc.");
            msgLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-font-style: italic; -fx-text-fill: #E5B869;");
        } else {
            iconLabel.setText("📦");
            iconLabel.setStyle("-fx-font-size: 38px; -fx-opacity: 0.75;");
            msgLabel.setText("Hiện tại không có phiên đấu giá nào khả dụng. Vui lòng quay lại sau.");
            msgLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-font-style: italic; -fx-text-fill: #1E3A8A;");
        }

        emptyBox.getChildren().addAll(iconLabel, msgLabel);
        auctionTable.setPlaceholder(emptyBox);
    }

    private void setupTableColumns() {
        itemNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItemName()));
        itemNameColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else {
                    setText(item);
                    setStyle("-fx-font-weight: bold;");
                }
            }
        });

        currentPriceColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getCurrentPrice()));
        currentPriceColumn.setCellFactory(column -> new TableCell<>() {
            private final java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,###");
            @Override
            protected void updateItem(Number price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) setText(null);
                else {
                    setText(formatter.format(price.doubleValue()) + " VNĐ");
                    setStyle("-fx-font-weight: bold; -fx-alignment: center-right;");
                }
            }
        });

        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) setText(null);
                else {
                    setText(status);
                    setStyle("-fx-alignment: center; -fx-font-weight: bold;");
                    getStyleClass().removeAll("status-running", "status-open", "status-finished", "status-paid");
                    if ("RUNNING".equalsIgnoreCase(status)) getStyleClass().add("status-running");
                    else if ("OPEN".equalsIgnoreCase(status)) getStyleClass().add("status-open");
                    else if ("FINISHED".equalsIgnoreCase(status)) getStyleClass().add("status-finished");
                    else if ("PAID".equalsIgnoreCase(status)) getStyleClass().add("status-paid");
                }
            }
        });

        java.time.format.DateTimeFormatter vnFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
        endTimeColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getEndTime() == null) return new SimpleStringProperty("");
            return new SimpleStringProperty(cellData.getValue().getEndTime().format(vnFormatter));
        });
        endTimeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else {
                    setText(item);
                    setStyle("-fx-alignment: center;");
                }
            }
        });

        if (startTimeColumn != null) {
            startTimeColumn.setCellValueFactory(cellData -> {
                if (cellData.getValue().getStartTime() == null) return new SimpleStringProperty("");
                return new SimpleStringProperty(cellData.getValue().getStartTime().format(vnFormatter));
            });
            startTimeColumn.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) setText(null);
                    else {
                        setText(item);
                        setStyle("-fx-alignment: center;");
                    }
                }
            });
        }
    }

    @FXML
    private void handleViewDetail() {
        AuctionSummaryDTO selectedAuction = auctionTable.getSelectionModel().getSelectedItem();
        if (selectedAuction == null) {
            showError("Vui lòng chọn một phiên đấu giá.");
            return;
        }
        SceneNavigator.showAuctionDetail(selectedAuction.getAuctionId());
    }

    @FXML
    private void handleBack() {
        SceneNavigator.showDashboard();
    }

    private void showMessage(String message) {
        if (messageLabel != null) messageLabel.setText(message);
    }

    private void showError(String message) {
        showMessage(message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}