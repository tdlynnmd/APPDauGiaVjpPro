package com.auction.controller;

import com.auction.dto.SocketResponse;
import com.auction.dto.UserDTO;
import com.auction.network.ClientUserApi;
import com.auction.util.ClientSession;
import com.auction.util.SceneNavigator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SettingsController {
    private final ClientUserApi userApi = new ClientUserApi();

    @FXML private VBox rootContainer;
    @FXML private Label messageLabel;
    @FXML private Label currentThemeLabel;

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField oldPasswordField;
    @FXML private PasswordField newPasswordField;

    @FXML private Button updateProfileButton;
    @FXML private Button updatePasswordButton;
    @FXML private Button themeToggleButton;
    @FXML private Button backButton;

    // --- CÁC THÀNH PHẦN THÊM MỚI ĐỂ PHỤC VỤ CHỨC NĂNG TỰ CUỘN BẢNG ---
    @FXML private ScrollPane settingsScrollPane;    // ScrollPane chứa nội dung bên phải
    @FXML private VBox mainContentContainer;         // Container tổng chứa cả 3 thẻ card bên trong ScrollPane
    @FXML private VBox cardThongTinHoSo;             // Khối card Thông Tin Hồ Sơ
    @FXML private VBox cardDoiMatKhau;               // Khối card Đổi Mật Khẩu
    @FXML private VBox cardGiaoDien;                 // Khối card Giao Diện Ứng Dụng

    // Vai trò: khởi tạo màn settings.
    @FXML
    public void initialize() {
        applyTheme();
        updateThemeLabel();

        if (!ClientSession.isLoggedIn()) {
            SceneNavigator.showLogin();
            return;
        }

        fillProfileForm(ClientSession.getCurrentUser());
        handleLoadProfile();
    }

    // Chức năng: tải profile mới nhất từ backend.
    @FXML
    private void handleLoadProfile() {
        runRequest(
                "Đang tải thông tin tài khoản...",
                userApi::getUserProfile,
                this::handleProfileResponse
        );
    }

    // Chức năng: cập nhật username và email.
    @FXML
    private void handleUpdateProfile() {
        String username = usernameField == null ? "" : usernameField.getText().trim();
        String email = emailField == null ? "" : emailField.getText().trim();

        if (username.isEmpty()) {
            showError("Vui lòng nhập username.");
            return;
        }

        if (email.isEmpty()) {
            showError("Vui lòng nhập email.");
            return;
        }

        runRequest(
                "Đang cập nhật thông tin tài khoản...",
                () -> userApi.updateProfile(username, email),
                this::handleUpdateProfileResponse
        );
    }

    // Chức năng: đổi mật khẩu tài khoản hiện tại.
    @FXML
    private void handleUpdatePassword() {
        String oldPassword = oldPasswordField == null ? "" : oldPasswordField.getText();
        String newPassword = newPasswordField == null ? "" : newPasswordField.getText();

        if (oldPassword.isEmpty()) {
            showError("Vui lòng nhập mật khẩu cũ.");
            return;
        }

        if (newPassword.isEmpty()) {
            showError("Vui lòng nhập mật khẩu mới.");
            return;
        }

        if (newPassword.length() < 8) {
            showError("Mật khẩu mới phải có ít nhất 8 ký tự.");
            return;
        }

        runRequest(
                "Đang đổi mật khẩu...",
                () -> userApi.updatePassword(oldPassword, newPassword),
                this::handleUpdatePasswordResponse
        );
    }

    // Chức năng: đổi light mode / dark mode.
    @FXML
    private void handleToggleTheme() {
        SceneNavigator.isAppDarkMode = !SceneNavigator.isAppDarkMode;
        applyTheme();
        updateThemeLabel();
        showMessage(SceneNavigator.isAppDarkMode ? "Đã bật Dark mode." : "Đã bật Light mode.");
    }

    // Vai trò: quay lại Dashboard.
    @FXML
    private void handleBack() {
        SceneNavigator.showDashboard();
    }

    // CHỨC NĂNG BỔ SUNG: Điều hướng cuộn mượt mà (Smooth Scroll) tới các phân khu card nội dung
    private void scrollToNode(javafx.scene.Node targetNode) {
        if (settingsScrollPane == null || targetNode == null || mainContentContainer == null) return;

        // Tính toán tổng chiều cao có thể cuộn thực tế của nội dung
        double totalScrollableHeight = mainContentContainer.getBoundsInLocal().getHeight() - settingsScrollPane.getViewportBounds().getHeight();
        if (totalScrollableHeight <= 0) return;

        // Lấy tọa độ Y của card đích so với container chứa nó
        double targetY = targetNode.getBoundsInParent().getMinY();

        // Tính toán tỷ lệ vvalue tương ứng (trong khoảng từ 0.0 đến 1.0)
        double targetVvalue = targetY / totalScrollableHeight;
        targetVvalue = Math.max(0.0, Math.min(1.0, targetVvalue));

        // Thực hiện hiệu ứng dịch chuyển vvalue mượt mà trong 300ms
        Timeline timeline = new Timeline();
        KeyValue keyValue = new KeyValue(settingsScrollPane.vvalueProperty(), targetVvalue);
        KeyFrame keyFrame = new KeyFrame(Duration.millis(300), keyValue);
        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
    }

    // Chức năng: Xử lý sự kiện click menu "Thông Tin Hồ Sơ"
    @FXML
    private void handleMenuThongTinHoSo() {
        scrollToNode(cardThongTinHoSo);
    }

    // Chức năng: Xử lý sự kiện click menu "Đổi Mật Khẩu"
    @FXML
    private void handleMenuDoiMatKhau() {
        scrollToNode(cardDoiMatKhau);
    }

    // Chức năng: Xử lý sự kiện click menu "Giao Diện Ứng Dụng"
    @FXML
    private void handleMenuGiaoDien() {
        scrollToNode(cardGiaoDien);
    }

    // Vai trò: xử lý response tải profile.
    private void handleProfileResponse(SocketResponse response) {
        if (!isSuccessful(response)) {
            showError(response == null ? "Server không phản hồi." : response.getMessage());
            return;
        }

        UserDTO user = userApi.parseUser(response);
        if (user == null) {
            showError("Không đọc được thông tin tài khoản.");
            return;
        }

        ClientSession.saveLoginSession(ClientSession.getToken(), user);
        fillProfileForm(user);
        showMessage("Đã tải thông tin tài khoản.");
    }

    // Vai trò: xử lý response cập nhật profile.
    private void handleUpdateProfileResponse(SocketResponse response) {
        if (!isSuccessful(response)) {
            showError(response == null ? "Cập nhật thất bại." : response.getMessage());
            return;
        }

        UserDTO user = userApi.parseUser(response);
        if (user != null) {
            ClientSession.saveLoginSession(ClientSession.getToken(), user);
            fillProfileForm(user);
        }

        showInfo(response.getMessage() == null ? "Cập nhật tài khoản thành công." : response.getMessage());
    }

    // Vai trò: xử lý response đổi mật khẩu.
    private void handleUpdatePasswordResponse(SocketResponse response) {
        if (!isSuccessful(response)) {
            showError(response == null ? "Đổi mật khẩu thất bại." : response.getMessage());
            return;
        }

        if (oldPasswordField != null) oldPasswordField.clear();
        if (newPasswordField != null) newPasswordField.clear();

        showInfo(response.getMessage() == null ? "Đổi mật khẩu thành công." : response.getMessage());
    }

    // Vai trò: đổ dữ liệu user vào form.
    private void fillProfileForm(UserDTO user) {
        if (user == null) return;

        if (usernameField != null) usernameField.setText(safeText(user.getUsername()));
        if (emailField != null) emailField.setText(safeText(user.getEmail()));
    }

    // Vai trò: chạy request backend ngoài UI thread.
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

        Thread worker = new Thread(task, "settings-request-thread");
        worker.setDaemon(true);
        worker.start();
    }

    // Vai trò: áp dụng theme hiện tại cho màn settings.
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
            System.out.println("Không thể nạp theme cho Settings: " + cssPath);
        }
    }

    // Vai trò: cập nhật nhãn trạng thái theme.
    private void updateThemeLabel() {
        if (currentThemeLabel != null) {
            currentThemeLabel.setText(SceneNavigator.isAppDarkMode ? "Dark mode" : "Light mode");
        }

        if (themeToggleButton != null) {
            themeToggleButton.setText(SceneNavigator.isAppDarkMode ? "Chuyển sang Light mode" : "Chuyển sang Dark mode");
        }
    }

    private void setBusy(boolean busy) {
        setDisabled(updateProfileButton, busy);
        setDisabled(updatePasswordButton, busy);
        setDisabled(backButton, busy);
        setDisabled(usernameField, busy);
        setDisabled(emailField, busy);
        setDisabled(oldPasswordField, busy);
        setDisabled(newPasswordField, busy);
    }

    private void setDisabled(Control control, boolean disabled) {
        if (control != null) control.setDisable(disabled);
    }

    private boolean isSuccessful(SocketResponse response) {
        return response != null && response.isSuccess();
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
        alert.setTitle("Lỗi");
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