package com.auction.util;

import com.auction.dto.UserDTO;
import javafx.application.Platform;
import javafx.scene.control.Label;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Lớp tiện ích phụ trợ cập nhật số dư ví hiển thị trên thanh tiêu đề giao diện.
 */
public class HeaderBalanceHelper {
    private static final NumberFormat moneyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public static void setupHeaderBalance(Label availableLabel, Label frozenLabel, Label totalLabel) {
        if (availableLabel == null && frozenLabel == null && totalLabel == null) {
            return;
        }

        updateLabels(availableLabel, frozenLabel, totalLabel);

        ClientSession.addBalanceListener((available, frozen) -> {
            Platform.runLater(() -> updateLabels(availableLabel, frozenLabel, totalLabel));
        });
    }

    private static void updateLabels(Label availableLabel, Label frozenLabel, Label totalLabel) {
        UserDTO user = ClientSession.getCurrentUser();
        if (user == null) {
            return;
        }
        double available = user.getAvailableBalance();
        double frozen = user.getFrozenBalance();
        double total = available + frozen;

        if (availableLabel != null) {
            availableLabel.setText(moneyFormat.format(available));
        }
        if (frozenLabel != null) {
            frozenLabel.setText(moneyFormat.format(frozen));
        }
        if (totalLabel != null) {
            totalLabel.setText(moneyFormat.format(total));
        }
    }
}
