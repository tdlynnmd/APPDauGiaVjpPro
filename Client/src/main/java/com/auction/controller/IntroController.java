package com.auction.controller;

import com.auction.service.ClientSocketService;
import com.auction.util.SceneNavigator;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

/**
 * Bộ điều khiển (Controller) hoặc lớp tiện ích IntroController xử lý giao diện Client JavaFX.
 */
public class IntroController {

    @FXML private AnchorPane introRoot;
    @FXML private Pane leftPersonNode, rightPersonNode, dealNode;
    @FXML private SVGPath leftLegA, leftLegB, rightLegA, rightLegB;
    @FXML private Label statusLabel;

    private Timeline walkingTimeline;

    @FXML
    public void initialize() {
        walkingTimeline = new Timeline(new KeyFrame(Duration.millis(150), e -> {
            boolean toggle = leftLegA.isVisible();
            leftLegA.setVisible(!toggle);
            leftLegB.setVisible(toggle);
            rightLegA.setVisible(!toggle);
            rightLegB.setVisible(toggle);
        }));
        walkingTimeline.setCycleCount(Animation.INDEFINITE);
        walkingTimeline.play();

        Thread initThread = new Thread(() -> {
            try {
                animate(0, 100, 0, -100, 1400, "Đang mời các nhà đầu tư vào phòng đấu giá...");
                Thread.sleep(1500);

                Platform.runLater(() -> statusLabel.setText("Đang kiểm tra chứng thực kết nối..."));
                long start = System.currentTimeMillis();
                ClientSocketService.getInstance();
                long elapsed = System.currentTimeMillis() - start;
                if (elapsed < 900) Thread.sleep(900 - elapsed);

                animate(100, 245, -100, -245, 700, "Khớp lệnh thành công! Đang đóng dấu hợp đồng...");
                Thread.sleep(750);

                Platform.runLater(() -> {
                    walkingTimeline.stop();
                    leftPersonNode.setVisible(false);
                    rightPersonNode.setVisible(false);
                    dealNode.setVisible(true);
                });
                Thread.sleep(1200);

                Platform.runLater(this::fadeOutAndSwitch);

            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Lỗi kết nối máy chủ! Vui lòng thử lại.");
                    statusLabel.setTextFill(javafx.scene.paint.Color.web("#ef4444"));
                    walkingTimeline.stop();
                });
            }
        });
        initThread.setDaemon(true);
        initThread.start();
    }

    private void animate(double lFrom, double lTo, double rFrom, double rTo, double ms, String txt) {
        Platform.runLater(() -> {
            statusLabel.setText(txt);
            Duration d = Duration.millis(ms);

            TranslateTransition mLeft = new TranslateTransition(d, leftPersonNode);
            mLeft.setFromX(lFrom); mLeft.setToX(lTo);
            mLeft.setInterpolator(Interpolator.LINEAR);
            mLeft.play();

            TranslateTransition mRight = new TranslateTransition(d, rightPersonNode);
            mRight.setFromX(rFrom); mRight.setToX(rTo);
            mRight.setInterpolator(Interpolator.LINEAR);
            mRight.play();
        });
    }

    private void fadeOutAndSwitch() {
        FadeTransition fade = new FadeTransition(Duration.millis(500), introRoot);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> SceneNavigator.showLogin());
        fade.play();
    }

    public void playOutroAndExit() {
        Platform.runLater(() -> {
            dealNode.setVisible(false);
            leftPersonNode.setVisible(true);
            rightPersonNode.setVisible(true);

            leftPersonNode.setScaleX(-1.4);
            rightPersonNode.setScaleX(1.4);

            statusLabel.setText("Cảm ơn quý nhà đầu tư. Hệ thống đang an toàn đóng sảnh...");
            statusLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #e5c158;");

            walkingTimeline.play();

            Duration duration = Duration.millis(1000);

            TranslateTransition moveLeft = new TranslateTransition(duration, leftPersonNode);
            moveLeft.setFromX(245); moveLeft.setToX(0);
            moveLeft.play();

            TranslateTransition moveRight = new TranslateTransition(duration, rightPersonNode);
            moveRight.setFromX(-245); moveRight.setToX(0);
            moveRight.play();

            FadeTransition fadeOut = new FadeTransition(duration, introRoot);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(event -> {
                walkingTimeline.stop();
                Platform.exit();
                System.exit(0);
            });
            fadeOut.play();
        });
    }
}