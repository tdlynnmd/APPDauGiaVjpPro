package com.auction.util;

import com.auction.dto.UserDTO;

/**
 * Lớp lưu trữ thông tin trạng thái đăng nhập và các bộ lắng nghe số dư ví phía Client.
 */
public class ClientSession {
    private static String token;
    private static UserDTO currentUser;

    private ClientSession() {
    }

    public static void saveLoginSession(String newToken, UserDTO user) {
        token = newToken;
        currentUser = user;
    }

    public static String getToken() {
        return token;
    }

    public static UserDTO getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return token != null && currentUser != null;
    }

    public static void clear() {
        token = null;
        currentUser = null;
    }

    public interface BalanceListener {
        void onBalanceUpdated(double availableBalance, double frozenBalance);
    }

    private static final java.util.List<BalanceListener> balanceListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void addBalanceListener(BalanceListener listener) {
        if (listener != null) {
            balanceListeners.add(listener);
        }
    }

    public static void removeBalanceListener(BalanceListener listener) {
        if (listener != null) {
            balanceListeners.remove(listener);
        }
    }

    public static void clearBalanceListeners() {
        balanceListeners.clear();
    }

    public static synchronized void setBalanceListener(BalanceListener listener) {
        balanceListeners.clear();
        if (listener != null) {
            balanceListeners.add(listener);
        }
    }

    public static void triggerBalanceUpdate(double available, double frozen) {
        if (currentUser != null) {
            currentUser.setAvailableBalance(available);
            currentUser.setFrozenBalance(frozen);
        }
        for (BalanceListener listener : balanceListeners) {
            try {
                listener.onBalanceUpdated(available, frozen);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
