package com.auction.controller;

import com.auction.dto.RegisterResponse;
import com.auction.enums.UserRole;
import com.auction.network.ClientAuthApi;
import com.auction.util.SceneNavigator;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

import java.util.Objects;

/**
  RegisterController là Controller phía Client cho màn hình đăng ký.

  Vai trò:
  - Nhận username, email, password, confirm password và role từ giao diện.
  - Kiểm tra sơ bộ dữ liệu trước khi gửi lên Server.
  - Gọi clientAuthApi.register() để gửi request REGISTER qua socket.
  - Nhận RegisterResponse và hiển thị kết quả cho người dùng.

 * Lưu ý quan trọng:
  - Controller chỉ xử lý giao diện.
  - Controller không tạo User.
  - Controller không kiểm tra username/email trùng.
  - Controller không hash password.
  - Những nghiệp vụ đó thuộc về AuthService phía Server.
 */
public class RegisterController {

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private ComboBox<UserRole> roleComboBox;

    @FXML
    private Label errorLabel;

    @FXML
    private Pane rootContainer;

    // Biến theo dõi theme hiện tại.
    // false nghĩa là đang ở light mode, true nghĩa là đang ở dark mode.
    private boolean isDarkMode = false;

    /**
     * initialize() được JavaFX tự động gọi sau khi load register.fxml.
     * Nhiệm vụ:
      - Đưa danh sách role vào ComboBox.
      - Mặc định chọn BIDDER.
      - Ẩn label lỗi ban đầu.

      Không đưa ADMIN vào đây vì tài khoản admin không nên cho đăng ký tự do.
     */
    @FXML
    public void initialize() {
        roleComboBox.getItems().setAll(UserRole.BIDDER, UserRole.SELLER);
        roleComboBox.setValue(UserRole.BIDDER);

        errorLabel.setVisible(false);
    }

    /**
     handleRegister() được gọi khi người dùng bấm nút Register.
     * Luồng xử lý:
     * 1. Lấy dữ liệu từ form.
     * 2. Kiểm tra các lỗi nhập liệu cơ bản.
     * 3. Gửi request REGISTER sang Server.
     * 4. Nhận RegisterResponse.
     * 5. Nếu thành công thì chuyển về Login.
     * 6. Nếu thất bại thì hiển thị lỗi.
     */
    @FXML
    public void handleRegister(ActionEvent event) {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        UserRole role = roleComboBox.getValue();

        errorLabel.setVisible(true);

        // Kiểm tra rỗng ở Client để tránh gửi request thiếu dữ liệu lên Server.
        // Server vẫn phải validate lại, vì dữ liệu từ Client không bao giờ được tin tuyệt đối.
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please fill in all registration information!");
            return;
        }

        // Confirm password là lỗi nhập liệu thuộc về giao diện,
        // nên kiểm tra ở Client là hợp lý.
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match!");
            return;
        }

        // Role bắt buộc phải có để Server biết tạo Bidder hay Seller.
        if (role == null) {
            showError("Please select an account role!");
            return;
        }

        // Gọi lớp API phía Client để gửi request REGISTER qua socket.
        // Controller không tự làm việc với Socket trực tiếp.
        ClientAuthApi authApi = new ClientAuthApi();
        RegisterResponse response = authApi.register(username, password, email, role);

        if (response.isSuccess()) {
            showSuccess("Registration successful! Returning to the login screen.");

            // Thông báo rõ cho người dùng biết tài khoản đã được tạo.
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Registration Successful");
            alert.setHeaderText(null);
            alert.setContentText("Your account has been created. You can log in now.");
            alert.showAndWait();

            // Đăng ký xong thì quay về Login để người dùng đăng nhập.
            SceneNavigator.showLogin();
        } else {
            // Nếu Server báo lỗi, hiển thị message do Server trả về.
            // Ví dụ: username đã tồn tại, email sai format, password yếu.
            showError(response.getMessage());
        }
    }

    /**
     * Chuyển từ màn hình Register về Login.
     * Hàm này được gọi khi người dùng bấm hyperlink "Login?".
     */
    @FXML
    public void goToLogin(ActionEvent event) {
        SceneNavigator.showLogin();
    }

    /**
     * Đổi theme sáng/tối cho màn hình Register.
     * Đây chỉ là xử lý giao diện, không liên quan đến nghiệp vụ đăng ký.
     */
    @FXML
    public void toggleTheme(ActionEvent event) {
        rootContainer.getStylesheets().clear();

        String path = isDarkMode
                ? "/com/auction/client/view/light.css"
                : "/com/auction/client/view/dark.css";

        try {
            String css = Objects.requireNonNull(getClass().getResource(path)).toExternalForm();
            rootContainer.getStylesheets().add(css);
            // Đảo trạng thái để lần bấm sau đổi sang theme còn lại.
            isDarkMode = !isDarkMode;
        } catch (Exception e) {
            e.printStackTrace();
            showError("CSS file not found: " + path);
        }
    }

    /**
     * Hiển thị lỗi lên label.
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: red;");
    }

    /**
     * Hiển thị thông báo thành công lên label.
     */
    private void showSuccess(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: green;");
    }
}