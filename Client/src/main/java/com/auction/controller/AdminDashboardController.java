package com.auction.controller;

import com.auction.dto.ActionLogDTO;
import com.auction.dto.AuctionSummaryDTO;
import com.auction.dto.ItemSummaryDTO;
import com.auction.dto.PageDTO;
import com.auction.dto.SocketResponse;
import com.auction.dto.UserDTO;
import com.auction.enums.UserRole;
import com.auction.network.ClientAdminApi;
import com.auction.util.ClientSession;
import com.auction.util.SceneNavigator;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Bộ điều khiển (Controller) hoặc lớp tiện ích AdminDashboardController xử lý giao diện Client JavaFX.
 */
public class AdminDashboardController {
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final ClientAdminApi adminApi = new ClientAdminApi();

    private final ObservableList<UserDTO> users = FXCollections.observableArrayList();
    private final ObservableList<ActionLogDTO> logs = FXCollections.observableArrayList();
    private final ObservableList<ItemSummaryDTO> items = FXCollections.observableArrayList();
    private final ObservableList<AuctionSummaryDTO> auctions = FXCollections.observableArrayList();

    private int currentUserPage = 1;
    private int totalUserPages = 0;

    private int currentLogPage = 1;
    private int totalLogPages = 0;

    private int currentItemPage = 1;
    private int totalItemPages = 0;

    private int currentAuctionPage = 1;
    private int totalAuctionPages = 0;

    @FXML private Parent rootContainer;
    @FXML private Label messageLabel;

    @FXML private TableView<UserDTO> usersTable;
    @FXML private TableColumn<UserDTO, String> userIdColumn;
    @FXML private TableColumn<UserDTO, String> usernameColumn;
    @FXML private TableColumn<UserDTO, String> emailColumn;
    @FXML private TableColumn<UserDTO, String> roleColumn;
    @FXML private TableColumn<UserDTO, String> statusColumn;
    @FXML private TableColumn<UserDTO, Number> availableBalanceColumn;
    @FXML private TableColumn<UserDTO, Number> frozenBalanceColumn;

    @FXML private Label userPageLabel;
    @FXML private TextField userPageSizeField;
    @FXML private TextField lockUserIdField;
    @FXML private TextField lockReasonField;

    @FXML private Button refreshUsersButton;
    @FXML private Button previousUserPageButton;
    @FXML private Button nextUserPageButton;
    @FXML private Button lockUserButton;

    @FXML private TableView<ActionLogDTO> logsTable;
    @FXML private TableColumn<ActionLogDTO, String> logIdColumn;
    @FXML private TableColumn<ActionLogDTO, String> logAdminIdColumn;
    @FXML private TableColumn<ActionLogDTO, String> logActionDetailColumn;
    @FXML private TableColumn<ActionLogDTO, String> logTargetTypeColumn;
    @FXML private TableColumn<ActionLogDTO, String> logTargetIdColumn;
    @FXML private TableColumn<ActionLogDTO, String> logTimestampColumn;

    @FXML private Label logPageLabel;
    @FXML private TextField logPageSizeField;

    @FXML private Button refreshLogsButton;
    @FXML private Button previousLogPageButton;
    @FXML private Button nextLogPageButton;

    @FXML private TextField cancelAuctionIdField;
    @FXML private TextField cancelAuctionReasonField;
    @FXML private TextField deleteItemIdField;
    @FXML private TextField deleteItemReasonField;

    @FXML private Button cancelAuctionButton;
    @FXML private Button deleteItemButton;

    @FXML private TableView<ItemSummaryDTO> itemsTable;
    @FXML private TableColumn<ItemSummaryDTO, String> itemIdColumn;
    @FXML private TableColumn<ItemSummaryDTO, String> itemNameColumn;
    @FXML private TableColumn<ItemSummaryDTO, String> itemTypeColumn;
    @FXML private TableColumn<ItemSummaryDTO, Number> itemPriceColumn;
    @FXML private TableColumn<ItemSummaryDTO, String> itemStatusColumn;

    @FXML private Label itemPageLabel;
    @FXML private TextField itemPageSizeField;

    @FXML private Button refreshItemsButton;
    @FXML private Button previousItemPageButton;
    @FXML private Button nextItemPageButton;

    @FXML private TableView<AuctionSummaryDTO> auctionsTable;
    @FXML private TableColumn<AuctionSummaryDTO, String> auctionIdColumn;
    @FXML private TableColumn<AuctionSummaryDTO, String> auctionItemIdColumn;
    @FXML private TableColumn<AuctionSummaryDTO, Number> auctionPriceColumn;
    @FXML private TableColumn<AuctionSummaryDTO, String> auctionStatusColumn;
    @FXML private TableColumn<AuctionSummaryDTO, String> auctionEndTimeColumn;

    @FXML private Label auctionPageLabel;
    @FXML private TextField auctionPageSizeField;

    @FXML private Button refreshAuctionsButton;
    @FXML private Button previousAuctionPageButton;
    @FXML private Button nextAuctionPageButton;

    @FXML
    public void initialize() {
        applyTheme();

        if (!isAdminSession()) {
            showError("Ban khong co quyen truy cap man hinh Admin.");
            SceneNavigator.showDashboard();
            return;
        }

        initializeDefaults();
        initializeUsersTable();
        initializeLogsTable();
        initializeItemsTable();
        initializeAuctionsTable();

        loadUsers(1);
        loadLogs(1);
        loadItems(1);
        loadAuctions(1);
    }

    /**
     * Ap dung theme hien tai cua app.
     * Neu FXML chua gan rootContainer thi bo qua, khong lam crash controller.
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
            System.out.println("Khong the nap theme cho AdminDashboard: " + cssPath);
        }
    }

    /**
     * Check nhanh phia Client de tranh vao nham man hinh.
     * Server van se check quyen that su bang AuthorizationService.
     */
    private boolean isAdminSession() {
        return ClientSession.isLoggedIn()
                && ClientSession.getCurrentUser() != null
                && ClientSession.getCurrentUser().getRole() == UserRole.ADMIN;
    }

    private void initializeDefaults() {
        if (userPageSizeField != null && isBlank(userPageSizeField.getText())) {
            userPageSizeField.setText(String.valueOf(DEFAULT_PAGE_SIZE));
        }

        if (logPageSizeField != null && isBlank(logPageSizeField.getText())) {
            logPageSizeField.setText(String.valueOf(DEFAULT_PAGE_SIZE));
        }

        if (itemPageSizeField != null && isBlank(itemPageSizeField.getText())) {
            itemPageSizeField.setText(String.valueOf(DEFAULT_PAGE_SIZE));
        }

        if (auctionPageSizeField != null && isBlank(auctionPageSizeField.getText())) {
            auctionPageSizeField.setText(String.valueOf(DEFAULT_PAGE_SIZE));
        }

        if (userPageSizeField != null) {
            userPageSizeField.setOnAction(event -> loadUsers(1));
        }
        if (logPageSizeField != null) {
            logPageSizeField.setOnAction(event -> loadLogs(1));
        }
        if (itemPageSizeField != null) {
            itemPageSizeField.setOnAction(event -> loadItems(1));
        }
        if (auctionPageSizeField != null) {
            auctionPageSizeField.setOnAction(event -> loadAuctions(1));
        }

        showMessage("Dang tai du lieu quan tri...");
    }

    /**
     * Cau hinh bang user.
     * TableView chi hien thi ObservableList users; khi users.setAll(...) thi UI tu cap nhat.
     */
    private void initializeUsersTable() {
        if (usersTable == null) {
            return;
        }

        usersTable.setItems(users);

        if (userIdColumn != null) {
            userIdColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(safeText(data.getValue().getId()))
            );
        }

        if (usernameColumn != null) {
            usernameColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(safeText(data.getValue().getUsername()))
            );
        }

        if (emailColumn != null) {
            emailColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(safeText(data.getValue().getEmail()))
            );
        }

        if (roleColumn != null) {
            roleColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getRole() == null ? "" : data.getValue().getRole().name())
            );
        }

        if (statusColumn != null) {
            statusColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getStatus() == null ? "" : data.getValue().getStatus().name())
            );
            statusColumn.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        if ("ACTIVE".equalsIgnoreCase(item)) {
                            setTextFill(Color.valueOf("#2ecc71"));
                        } else if ("LOCKED".equalsIgnoreCase(item) || "BANNED".equalsIgnoreCase(item)) {
                            setTextFill(Color.valueOf("#e74c3c"));
                        } else {
                            setTextFill(Color.valueOf("#ff9f43"));
                        }
                    }
                }
            });
        }

        if (availableBalanceColumn != null) {
            availableBalanceColumn.setCellValueFactory(data ->
                    new ReadOnlyDoubleWrapper(data.getValue().getAvailableBalance())
            );
            availableBalanceColumn.setCellFactory(col -> new TableCell<>() {
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
        }

        if (frozenBalanceColumn != null) {
            frozenBalanceColumn.setCellValueFactory(data ->
                    new ReadOnlyDoubleWrapper(data.getValue().getFrozenBalance())
            );
            frozenBalanceColumn.setCellFactory(col -> new TableCell<>() {
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
        }

        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldUser, newUser) -> {
            if (newUser != null && lockUserIdField != null) {
                lockUserIdField.setText(newUser.getId());
            }
        });
    }

    /**
     * Cau hinh bang audit log.
     */
    private void initializeLogsTable() {
        if (logsTable == null) {
            return;
        }

        logsTable.setItems(logs);

        if (logIdColumn != null) {
            logIdColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(safeText(data.getValue().getLogId()))
            );
        }

        if (logAdminIdColumn != null) {
            logAdminIdColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(safeText(data.getValue().getAdminId()))
            );
        }

        if (logActionDetailColumn != null) {
            logActionDetailColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(safeText(data.getValue().getActionDetail()))
            );
        }

        if (logTargetTypeColumn != null) {
            logTargetTypeColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(safeText(data.getValue().getTargetType()))
            );
        }

        if (logTargetIdColumn != null) {
            logTargetIdColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(safeText(data.getValue().getTargetId()))
            );
        }

        if (logTimestampColumn != null) {
            logTimestampColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(formatDateTime(data.getValue().getTimestamp()))
            );
        }
    }

    @FXML
    private void handleRefreshUsers() {
        loadUsers(currentUserPage <= 0 ? 1 : currentUserPage);
    }

    @FXML
    private void handlePreviousUserPage() {
        if (currentUserPage <= 1) {
            return;
        }

        loadUsers(currentUserPage - 1);
    }

    @FXML
    private void handleNextUserPage() {
        if (totalUserPages > 0 && currentUserPage >= totalUserPages) {
            return;
        }

        loadUsers(currentUserPage + 1);
    }

    /**
     * Goi server de khoa user.
     *
     * Luong:
     * - Lay userId tu TextField hoac row dang chon.
     * - Lay reason.
     * - Goi CMD_ADMIN_LOCK_USER qua ClientAdminApi.
     * - Neu thanh cong thi reload user + audit log.
     */
    @FXML
    private void handleLockSelectedUser() {
        String userId = firstNonBlank(readText(lockUserIdField), getSelectedUserId());
        String reason = readText(lockReasonField);

        if (isBlank(userId)) {
            showError("Vui long chon hoac nhap userId can khoa.");
            return;
        }

        if (isBlank(reason)) {
            showError("Vui long nhap ly do khoa user.");
            return;
        }

        if (ClientSession.getCurrentUser() != null
                && userId.equals(ClientSession.getCurrentUser().getId())) {
            showError("Khong nen tu khoa tai khoan Admin dang dang nhap.");
            return;
        }

        if (!confirm("Khoa user nay?", "User ID: " + userId + "\nLy do: " + reason)) {
            return;
        }

        runServerCall(
                "Dang khoa user...",
                () -> adminApi.lockUser(userId, reason),
                response -> {
                    showInfo(response.getMessage());
                    clearText(lockReasonField);
                    loadUsers(currentUserPage);
                    loadLogs(1);
                }
        );
    }

    private void loadUsers(int page) {
        Integer pageSize = readPositiveInt(userPageSizeField, DEFAULT_PAGE_SIZE, "kich thuoc trang user");
        if (pageSize == null) {
            return;
        }

        runServerCall(
                "Dang tai danh sach user...",
                () -> adminApi.getUsers(page, pageSize),
                response -> {
                    PageDTO<UserDTO> userPage = adminApi.parseUserPage(response);
                    applyUserPage(userPage, page);
                }
        );
    }

    private void applyUserPage(PageDTO<UserDTO> userPage, int requestedPage) {
        List<UserDTO> data = userPage == null || userPage.getData() == null
                ? List.of()
                : userPage.getData();

        users.setAll(data);

        currentUserPage = userPage == null ? requestedPage : userPage.getCurrentPage();
        totalUserPages = userPage == null ? 0 : userPage.getTotalPages();

        updateUserPageLabel();
        updateUserPageButtons();

        showMessage("Da tai " + data.size() + " user.");
    }

    @FXML
    private void handleRefreshLogs() {
        loadLogs(currentLogPage <= 0 ? 1 : currentLogPage);
    }

    @FXML
    private void handlePreviousLogPage() {
        if (currentLogPage <= 1) {
            return;
        }

        loadLogs(currentLogPage - 1);
    }

    @FXML
    private void handleNextLogPage() {
        if (totalLogPages > 0 && currentLogPage >= totalLogPages) {
            return;
        }

        loadLogs(currentLogPage + 1);
    }

    private void loadLogs(int page) {
        Integer pageSize = readPositiveInt(logPageSizeField, DEFAULT_PAGE_SIZE, "kich thuoc trang log");
        if (pageSize == null) {
            return;
        }

        runServerCall(
                "Dang tai audit log...",
                () -> adminApi.getLogs(page, pageSize),
                response -> {
                    PageDTO<ActionLogDTO> logPage = adminApi.parseLogPage(response);
                    applyLogPage(logPage, page);
                }
        );
    }

    private void applyLogPage(PageDTO<ActionLogDTO> logPage, int requestedPage) {
        List<ActionLogDTO> data = logPage == null || logPage.getData() == null
                ? List.of()
                : logPage.getData();

        logs.setAll(data);

        currentLogPage = logPage == null ? requestedPage : logPage.getCurrentPage();
        totalLogPages = logPage == null ? 0 : logPage.getTotalPages();

        updateLogPageLabel();
        updateLogPageButtons();

        showMessage("Da tai " + data.size() + " audit log.");
    }

    /**
     * Goi server de Admin huy phien dau gia vi pham.
     */
    @FXML
    private void handleCancelAuction() {
        String auctionId = readText(cancelAuctionIdField);
        String reason = readText(cancelAuctionReasonField);

        if (isBlank(auctionId)) {
            showError("Vui long nhap auctionId can huy.");
            return;
        }

        if (isBlank(reason)) {
            showError("Vui long nhap ly do huy phien dau gia.");
            return;
        }

        if (!confirm("Huy phien dau gia?", "Auction ID: " + auctionId + "\nLy do: " + reason)) {
            return;
        }

        runServerCall(
                "Dang huy phien dau gia...",
                () -> adminApi.cancelAuction(auctionId, reason),
                response -> {
                    showInfo(response.getMessage());
                    clearText(cancelAuctionIdField);
                    clearText(cancelAuctionReasonField);
                    loadLogs(1);
                }
        );
    }

    /**
     * Goi server de Admin go/ban item vi pham.
     */
    @FXML
    private void handleDeleteItem() {
        String itemId = readText(deleteItemIdField);
        String reason = readText(deleteItemReasonField);

        if (isBlank(itemId)) {
            showError("Vui long nhap itemId can go bo.");
            return;
        }

        if (isBlank(reason)) {
            showError("Vui long nhap ly do go bo item.");
            return;
        }

        if (!confirm("Go bo item?", "Item ID: " + itemId + "\nLy do: " + reason)) {
            return;
        }

        runServerCall(
                "Dang go bo item...",
                () -> adminApi.deleteItem(itemId, reason),
                response -> {
                    showInfo(response.getMessage());
                    clearText(deleteItemIdField);
                    clearText(deleteItemReasonField);
                    loadLogs(1);
                    loadItems(1);
                }
        );
    }

    /**
     * Cau hinh bang items cho Admin Dashboard.
     * Bam sat y het initializeUsersTable – ObservableList, CellValueFactory, selection listener.
     */
    private void initializeItemsTable() {
        if (itemsTable == null) {
            return;
        }

        itemsTable.setItems(items);

        if (itemIdColumn != null) {
            itemIdColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(safeText(data.getValue().getItemId()))
            );
        }

        if (itemNameColumn != null) {
            itemNameColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(safeText(data.getValue().getItemName()))
            );
        }

        if (itemTypeColumn != null) {
            itemTypeColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(safeText(data.getValue().getItemType()))
            );
        }

        if (itemPriceColumn != null) {
            itemPriceColumn.setCellValueFactory(data ->
                    new ReadOnlyDoubleWrapper(data.getValue().getStartingPrice())
            );
            itemPriceColumn.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(Number item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : String.format("%,.0f VND", item.doubleValue()));
                }
            });
        }

        if (itemStatusColumn != null) {
            itemStatusColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(safeText(data.getValue().getStatus()))
            );
            itemStatusColumn.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null); setStyle("");
                    } else {
                        setText(item);
                        if ("ACTIVE".equalsIgnoreCase(item)) {
                            setTextFill(Color.valueOf("#2ecc71"));
                        } else if ("BANNED".equalsIgnoreCase(item) || "REMOVED".equalsIgnoreCase(item)) {
                            setTextFill(Color.valueOf("#e74c3c"));
                        } else {
                            setTextFill(Color.valueOf("#ff9f43"));
                        }
                    }
                }
            });
        }

        itemsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem != null && deleteItemIdField != null) {
                deleteItemIdField.setText(newItem.getItemId());
            }
        });
    }

    @FXML
    private void handleRefreshItems() {
        loadItems(currentItemPage <= 0 ? 1 : currentItemPage);
    }

    @FXML
    private void handlePreviousItemPage() {
        if (currentItemPage <= 1) return;
        loadItems(currentItemPage - 1);
    }

    @FXML
    private void handleNextItemPage() {
        if (totalItemPages > 0 && currentItemPage >= totalItemPages) return;
        loadItems(currentItemPage + 1);
    }

    private void loadItems(int page) {
        Integer pageSize = readPositiveInt(itemPageSizeField, DEFAULT_PAGE_SIZE, "kich thuoc trang item");
        if (pageSize == null) return;

        runServerCall(
                "Dang tai danh sach vat pham...",
                () -> adminApi.getItems(page, pageSize),
                response -> {
                    PageDTO<ItemSummaryDTO> itemPage = adminApi.parseItemPage(response);
                    applyItemPage(itemPage, page);
                }
        );
    }

    private void applyItemPage(PageDTO<ItemSummaryDTO> itemPage, int requestedPage) {
        List<ItemSummaryDTO> data = itemPage == null || itemPage.getData() == null
                ? List.of() : itemPage.getData();

        items.setAll(data);

        currentItemPage = itemPage == null ? requestedPage : itemPage.getCurrentPage();
        totalItemPages = itemPage == null ? 0 : itemPage.getTotalPages();

        updateItemPageLabel();
        updateItemPageButtons();

        showMessage("Da tai " + data.size() + " vat pham.");
    }

    /**
     * Cau hinh bang auctions cho Admin Dashboard.
     * Bam sat y het initializeLogsTable.
     */
    private void initializeAuctionsTable() {
        if (auctionsTable == null) {
            return;
        }

        auctionsTable.setItems(auctions);

        if (auctionIdColumn != null) {
            auctionIdColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(safeText(data.getValue().getAuctionId()))
            );
        }

        if (auctionItemIdColumn != null) {
            auctionItemIdColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(safeText(data.getValue().getItemName()))
            );
        }

        if (auctionPriceColumn != null) {
            auctionPriceColumn.setCellValueFactory(data ->
                    new ReadOnlyDoubleWrapper(data.getValue().getCurrentPrice())
            );
            auctionPriceColumn.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(Number item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : String.format("%,.0f VND", item.doubleValue()));
                }
            });
        }

        if (auctionStatusColumn != null) {
            auctionStatusColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(safeText(data.getValue().getStatus()))
            );
            auctionStatusColumn.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null); setStyle("");
                    } else {
                        setText(item);
                        if ("RUNNING".equalsIgnoreCase(item)) {
                            setTextFill(Color.valueOf("#2ecc71"));
                        } else if ("CANCELED".equalsIgnoreCase(item)) {
                            setTextFill(Color.valueOf("#e74c3c"));
                        } else if ("OPEN".equalsIgnoreCase(item)) {
                            setTextFill(Color.valueOf("#3498db"));
                        } else {
                            setTextFill(Color.valueOf("#95a5a6"));
                        }
                    }
                }
            });
        }

        if (auctionEndTimeColumn != null) {
            auctionEndTimeColumn.setCellValueFactory(data ->
                    new SimpleStringProperty(formatDateTime(data.getValue().getEndTime()))
            );
        }

        auctionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldAuction, newAuction) -> {
            if (newAuction != null && cancelAuctionIdField != null) {
                cancelAuctionIdField.setText(newAuction.getAuctionId());
            }
        });
    }

    @FXML
    private void handleRefreshAuctions() {
        loadAuctions(currentAuctionPage <= 0 ? 1 : currentAuctionPage);
    }

    @FXML
    private void handlePreviousAuctionPage() {
        if (currentAuctionPage <= 1) return;
        loadAuctions(currentAuctionPage - 1);
    }

    @FXML
    private void handleNextAuctionPage() {
        if (totalAuctionPages > 0 && currentAuctionPage >= totalAuctionPages) return;
        loadAuctions(currentAuctionPage + 1);
    }

    private void loadAuctions(int page) {
        Integer pageSize = readPositiveInt(auctionPageSizeField, DEFAULT_PAGE_SIZE, "kich thuoc trang auction");
        if (pageSize == null) return;

        runServerCall(
                "Dang tai danh sach phien dau gia...",
                () -> adminApi.getAuctions(page, pageSize),
                response -> {
                    PageDTO<AuctionSummaryDTO> auctionPage = adminApi.parseAuctionPage(response);
                    applyAuctionPage(auctionPage, page);
                }
        );
    }

    private void applyAuctionPage(PageDTO<AuctionSummaryDTO> auctionPage, int requestedPage) {
        List<AuctionSummaryDTO> data = auctionPage == null || auctionPage.getData() == null
                ? List.of() : auctionPage.getData();

        auctions.setAll(data);

        currentAuctionPage = auctionPage == null ? requestedPage : auctionPage.getCurrentPage();
        totalAuctionPages = auctionPage == null ? 0 : auctionPage.getTotalPages();

        updateAuctionPageLabel();
        updateAuctionPageButtons();

        showMessage("Da tai " + data.size() + " phien dau gia.");
    }

    @FXML
    private void handleBack() {
        SceneNavigator.showDashboard();
    }

    @FXML
    private void handleClearForms() {
        clearText(lockUserIdField);
        clearText(lockReasonField);
        clearText(cancelAuctionIdField);
        clearText(cancelAuctionReasonField);
        clearText(deleteItemIdField);
        clearText(deleteItemReasonField);

        if (usersTable != null) {
            usersTable.getSelectionModel().clearSelection();
        }

        showMessage("Da lam moi form Admin.");
    }

    /**
     * Chay request server tren background thread de UI JavaFX khong bi dung.
     *
     * Diem quan trong:
     * - supplier.call server thong qua ClientAdminApi.
     * - onSuccess chay tren JavaFX Application Thread nhờ Task.setOnSucceeded.
     */
    private void runServerCall(String loadingMessage,
                               Supplier<SocketResponse> supplier,
                               Consumer<SocketResponse> onSuccess) {
        setBusy(true);
        showMessage(loadingMessage);

        Task<SocketResponse> task = new Task<>() {
            @Override
            protected SocketResponse call() {
                return supplier.get();
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);

            SocketResponse response = task.getValue();
            if (!isSuccessful(response)) {
                showError(response == null
                        ? "Server khong tra ve phan hoi hop le."
                        : response.getMessage());
                return;
            }

            onSuccess.accept(response);
        });

        task.setOnFailed(event -> {
            setBusy(false);

            Throwable error = task.getException();
            showError("Loi khi lien he server: " + (error == null ? "unknown" : error.getMessage()));
        });

        Thread thread = new Thread(task, "admin-dashboard-server-call");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Disable cac nut thao tac trong luc dang goi server de tranh bam lap request.
     */
    private void setBusy(boolean busy) {
        setDisable(refreshUsersButton, busy);
        setDisable(previousUserPageButton, busy);
        setDisable(nextUserPageButton, busy);
        setDisable(lockUserButton, busy);

        setDisable(refreshLogsButton, busy);
        setDisable(previousLogPageButton, busy);
        setDisable(nextLogPageButton, busy);

        setDisable(cancelAuctionButton, busy);
        setDisable(deleteItemButton, busy);

        setDisable(refreshItemsButton, busy);
        setDisable(previousItemPageButton, busy);
        setDisable(nextItemPageButton, busy);

        setDisable(refreshAuctionsButton, busy);
        setDisable(previousAuctionPageButton, busy);
        setDisable(nextAuctionPageButton, busy);
    }

    private void updateUserPageLabel() {
        if (userPageLabel != null) {
            userPageLabel.setText("Users page " + currentUserPage + " / " + Math.max(totalUserPages, 0));
        }
    }

    private void updateLogPageLabel() {
        if (logPageLabel != null) {
            logPageLabel.setText("Logs page " + currentLogPage + " / " + Math.max(totalLogPages, 0));
        }
    }

    private void updateItemPageLabel() {
        if (itemPageLabel != null) {
            itemPageLabel.setText("Items page " + currentItemPage + " / " + Math.max(totalItemPages, 0));
        }
    }

    private void updateAuctionPageLabel() {
        if (auctionPageLabel != null) {
            auctionPageLabel.setText("Auctions page " + currentAuctionPage + " / " + Math.max(totalAuctionPages, 0));
        }
    }

    private void updateUserPageButtons() {
        if (previousUserPageButton != null) {
            previousUserPageButton.setDisable(currentUserPage <= 1);
        }

        if (nextUserPageButton != null) {
            nextUserPageButton.setDisable(totalUserPages <= 0 || currentUserPage >= totalUserPages);
        }
    }

    private void updateLogPageButtons() {
        if (previousLogPageButton != null) {
            previousLogPageButton.setDisable(currentLogPage <= 1);
        }

        if (nextLogPageButton != null) {
            nextLogPageButton.setDisable(totalLogPages <= 0 || currentLogPage >= totalLogPages);
        }
    }

    private void updateItemPageButtons() {
        if (previousItemPageButton != null) {
            previousItemPageButton.setDisable(currentItemPage <= 1);
        }
        if (nextItemPageButton != null) {
            nextItemPageButton.setDisable(totalItemPages <= 0 || currentItemPage >= totalItemPages);
        }
    }

    private void updateAuctionPageButtons() {
        if (previousAuctionPageButton != null) {
            previousAuctionPageButton.setDisable(currentAuctionPage <= 1);
        }
        if (nextAuctionPageButton != null) {
            nextAuctionPageButton.setDisable(totalAuctionPages <= 0 || currentAuctionPage >= totalAuctionPages);
        }
    }

    private void showMessage(String message) {
        if (messageLabel != null) {
            messageLabel.setText(safeText(message));
        }
    }

    private void showError(String message) {
        showMessage(message);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Loi");
        alert.setHeaderText(null);
        alert.setContentText(safeText(message));
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thong bao");
        alert.setHeaderText(null);
        alert.setContentText(safeText(message));
        alert.showAndWait();
    }

    private boolean confirm(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xac nhan");
        alert.setHeaderText(title);
        alert.setContentText(content);

        return alert.showAndWait()
                .filter(buttonType -> buttonType == javafx.scene.control.ButtonType.OK)
                .isPresent();
    }

    private boolean isSuccessful(SocketResponse response) {
        return response != null && response.isSuccess();
    }

    private String getSelectedUserId() {
        if (usersTable == null || usersTable.getSelectionModel().getSelectedItem() == null) {
            return null;
        }

        return usersTable.getSelectionModel().getSelectedItem().getId();
    }

    private Integer readPositiveInt(TextField field, int defaultValue, String fieldName) {
        String value = readText(field);

        if (isBlank(value)) {
            return defaultValue;
        }

        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0) {
                showError(fieldName + " phai lon hon 0.");
                return null;
            }

            return parsed;
        } catch (NumberFormatException e) {
            showError(fieldName + " khong hop le.");
            return null;
        }
    }

    private String readText(TextField field) {
        return field == null ? null : field.getText();
    }

    private void clearText(TextField field) {
        if (field != null) {
            field.clear();
        }
    }

    private String firstNonBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    /**
     * Dinh dang thoi gian hien thi hieu nang cao cho Audit Log (Vi du: dd/MM/yyyy HH:mm:ss)
     */
    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            return value.format(formatter);
        } catch (Exception e) {
            return value.toString();
        }
    }

    private void setDisable(Button button, boolean disabled) {
        if (button != null) {
            button.setDisable(disabled);
        }
    }
}