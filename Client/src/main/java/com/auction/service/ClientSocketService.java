package com.auction.service;

import com.auction.dto.SocketRequest;
import com.auction.dto.SocketResponse;
import com.auction.enums.ActionType;
import com.auction.network.ClientNetworkManager;
import com.auction.util.ClientSession;
import com.auction.util.SceneNavigator;
import com.auction.utils.GsonProvider;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Dịch vụ xử lý đọc/ghi Socket TCP trung tâm, định tuyến gói tin phản hồi (Response) và sự kiện thời gian thực (Event) phía Client.
 */
public class ClientSocketService {
    private static final int REQUEST_TIMEOUT_SECONDS = 15;

    private static ClientSocketService instance;

    private final com.google.gson.Gson gson = GsonProvider.getGson();
    private final PrintWriter writer;
    private final BufferedReader reader;

    private final Map<String, PendingRequest> pendingResponses = new ConcurrentHashMap<>();

    private final CopyOnWriteArrayList<RealtimeUpdateListener> realtimeListeners = new CopyOnWriteArrayList<>();

    private volatile boolean running = true;
    private volatile boolean forceLogoutHandled = false;

    private final java.util.concurrent.ScheduledExecutorService pingScheduler = 
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ping-scheduler");
            t.setDaemon(true);
            return t;
        });

    private ClientSocketService() {
        ClientNetworkManager network = ClientNetworkManager.getInstance();

        this.writer = network.getWriter();
        this.reader = network.getReader();

        startReaderThread();
        startPingScheduler();
    }

    private void startPingScheduler() {
        pingScheduler.scheduleAtFixedRate(this::sendPing, 20, 20, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void sendPing() {
        if (!running || writer == null) {
            return;
        }
        try {
            SocketRequest pingRequest = new SocketRequest(ActionType.PING, (String) null);
            synchronized (writer) {
                writer.println(gson.toJson(pingRequest));
                writer.flush();
            }
        } catch (Exception e) {
            System.err.println("[ClientSocketService] Loi gui PING keep-alive: " + e.getMessage());
        }
    }

    /**
     * Singleton de toan bo Client dung chung dung mot ClientSocketService.
     *
     * Dieu nay dam bao chi co mot thread duy nhat doc socket.
     */
    public static synchronized ClientSocketService getInstance() {
        if (instance == null) {
            instance = new ClientSocketService();
        }
        else if (!instance.running) {
            System.out.println("[Service] ClientSocketService cu da dung. Dang tai tao phien dich vu moi...");
            instance = new ClientSocketService();
        }

        return instance;
    }

    /**
     * Gui request sang Server va cho SocketResponse tuong ung.
     *
     * Luong xu ly:
     * 1. Luu requestId vao pendingResponses.
     * 2. Gui JSON request sang Server.
     * 3. Thread doc socket nhan RESPONSE.
     * 4. RESPONSE duoc ghep lai voi request dang cho bang requestId.
     *
     * @param socketRequest request can gui sang Server
     * @return SocketResponse Server tra ve, hoac failure neu timeout/loi ket noi
     */
    public SocketResponse sendRequest(SocketRequest socketRequest) {
        String requestId = socketRequest.getRequestId();
        ActionType action = socketRequest.getAction();

        if (writer == null || reader == null) {
            return SocketResponse.failure(
                    requestId,
                    action,
                    "Client chua ket noi duoc toi Server.",
                    "CONNECTION_ERROR"
            );
        }

        PendingRequest pendingRequest = new PendingRequest(action);
        pendingResponses.put(requestId, pendingRequest);

        try {
            
            synchronized (writer) {
                writer.println(gson.toJson(socketRequest));
                writer.flush();
            }

            return pendingRequest.future.get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        } catch (TimeoutException e) {
            pendingResponses.remove(requestId);

            return SocketResponse.failure(
                    requestId,
                    action,
                    "Server phan hoi qua lau.",
                    "REQUEST_TIMEOUT"
            );

        } catch (InterruptedException e) {
            pendingResponses.remove(requestId);
            Thread.currentThread().interrupt();

            return SocketResponse.failure(
                    requestId,
                    action,
                    "Request bi gian doan trong luc cho Server phan hoi.",
                    "REQUEST_INTERRUPTED"
            );

        } catch (ExecutionException e) {
            pendingResponses.remove(requestId);

            return SocketResponse.failure(
                    requestId,
                    action,
                    "Co loi khi nhan phan hoi tu Server.",
                    "RESPONSE_ERROR"
            );

        } catch (Exception e) {
            pendingResponses.remove(requestId);
            e.printStackTrace();

            return SocketResponse.failure(
                    requestId,
                    action,
                    "Khong the gui request toi Server.",
                    "SEND_REQUEST_ERROR"
            );
        }
    }

    /**
     * Dang ky listener nhan realtime event.
     */
    public void addRealtimeListener(RealtimeUpdateListener listener) {
        if (listener == null) {
            return;
        }

        if (!realtimeListeners.contains(listener)) {
            realtimeListeners.add(listener);
        }
    }

    /**
     * Huy dang ky listener.
     *
     * Can goi khi Controller roi man hinh de tranh nhan event thua.
     */
    public void removeRealtimeListener(RealtimeUpdateListener listener) {
        if (listener == null) {
            return;
        }

        realtimeListeners.remove(listener);
    }

    /**
     * Tao thread nen chuyen doc tat ca message Server gui ve.
     *
     * Day la noi duy nhat trong Client duoc phep reader.readLine().
     */
    private void startReaderThread() {
        if (reader == null) {
            System.err.println("[ClientSocketService] Khong the bat dau reader thread vi reader null.");
            return;
        }

        Thread readerThread = new Thread(this::listenToServer, "client-socket-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    /**
     * Vong lap doc message tu Server.
     *
     * Moi dong Server gui ve nen la JSON cua SocketResponse.
     */
    private void listenToServer() {
        while (running) {
            try {
                String rawMessage = reader.readLine();

                if (rawMessage == null) {
                    if (running) {
                        handleConnectionClosed();
                    }
                    break;
                }

                if (rawMessage.trim().isEmpty()) {
                    continue;
                }

                SocketResponse response = gson.fromJson(rawMessage, SocketResponse.class);
                handleServerMessage(response);

            } catch (IOException e) {
                if (running) {
                    handleConnectionClosed();
                }
                break;

            } catch (Exception e) {
                
                System.err.println("[ClientSocketService] Khong parse duoc message tu Server: " + e.getMessage());
            }
        }
    }

    /**
     * Phan loai message Server gui ve.
     *
     * - EVENT: gui cho realtime listener.
     * - RESPONSE: ghep voi request dang cho.
     */
    private void handleServerMessage(SocketResponse response) {
        if (response == null) {
            return;
        }

        if (ActionType.PING.name().equals(response.getAction())) {
            return;
        }

        if (SocketResponse.TYPE_EVENT.equals(response.getType())) {
            if (isForceLogoutEvent(response)) {
                handleForceLogout(response);
                return;
            }
            if ("WALLET_UPDATE".equals(response.getAction())) {
                handleWalletUpdate(response);
            }

            notifyRealtimeListeners(response);
            return;
        }

        if (SocketResponse.TYPE_RESPONSE.equals(response.getType())) {
            completePendingResponse(response);
            return;
        }

        if (response.getRequestId() != null && !response.getRequestId().trim().isEmpty()) {
            completePendingResponse(response);
        }
    }

    /**
     * FORCE_LOGOUT la event cap he thong.
     *
     * Khi Admin khoa user online, Server gui event nay ve client cua user do.
     * Client phai dung moi request dang cho, xoa session, dong socket va quay ve Login.
     */
    private void handleForceLogout(SocketResponse event) {
        if (forceLogoutHandled) {
            return;
        }

        forceLogoutHandled = true;
        running = false;

        completeAllPendingResponses(
                "Tai khoan da bi khoa boi quan tri vien.",
                "FORCE_LOGOUT"
        );

        String displayMessage = buildForceLogoutDisplayMessage(event);

        Platform.runLater(() -> {
            showForceLogoutAlert(displayMessage);

            ClientSession.clear();
            ClientNetworkManager.resetConnection();
            ClientSocketService.reset();
            SceneNavigator.showLogin();
        });
    }

    private void handleWalletUpdate(SocketResponse event) {
        if (event == null || event.getBody() == null || !event.getBody().isJsonObject()) {
            return;
        }
        try {
            JsonObject body = event.getBody().getAsJsonObject();
            double available = body.get("availableBalance").getAsDouble();
            double frozen = body.get("frozenBalance").getAsDouble();
            
            Platform.runLater(() -> {
                ClientSession.triggerBalanceUpdate(available, frozen);
            });
        } catch (Exception e) {
            System.err.println("[ClientSocketService] Loi khi xu ly WALLET_UPDATE: " + e.getMessage());
        }
    }

    private boolean isForceLogoutEvent(SocketResponse response) {
        return ActionType.FORCE_LOGOUT.name().equals(getActionName(response));
    }

    private String getActionName(SocketResponse response) {
        try {
            return response.getAction();
        } catch (Exception e) {
            return null;
        }
    }

    private String buildForceLogoutDisplayMessage(SocketResponse event) {
        String message = event.getMessage();

        String reason = extractForceLogoutReason(event);
        if (reason != null && !reason.trim().isEmpty()) {
            return safeText(message, "Tai khoan cua ban da bi khoa boi quan tri vien.")
                    + "\nLy do: " + reason.trim();
        }

        return safeText(message, "Tai khoan cua ban da bi khoa boi quan tri vien.");
    }

    private String extractForceLogoutReason(SocketResponse event) {
        if (event.getBody() == null || !event.getBody().isJsonObject()) {
            return null;
        }

        JsonObject body = event.getBody().getAsJsonObject();
        if (!body.has("reason") || body.get("reason").isJsonNull()) {
            return null;
        }

        return body.get("reason").getAsString();
    }

    private void showForceLogoutAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Tai khoan bi khoa");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    /**
     * Tra SocketResponse ve dung request dang cho bang requestId.
     */
    private void completePendingResponse(SocketResponse response) {
        String requestId = response.getRequestId();

        if (requestId == null || requestId.trim().isEmpty()) {
            System.err.println("[ClientSocketService] RESPONSE thieu requestId nen khong the ghep request.");
            return;
        }

        PendingRequest pendingRequest = pendingResponses.remove(requestId);

        if (pendingRequest == null) {
            System.err.println("[ClientSocketService] Khong tim thay request dang cho voi requestId = " + requestId);
            return;
        }

        pendingRequest.future.complete(response);
    }

    /**
     * Gui realtime event cho tat ca listener da dang ky.
     *
     * Luu y:
     * - Ham listener chay tren thread doc socket.
     * - Controller JavaFX phai dung Platform.runLater(...) neu cap nhat UI.
     */
    private void notifyRealtimeListeners(SocketResponse event) {
        for (RealtimeUpdateListener listener : realtimeListeners) {
            try {
                listener.onRealtimeUpdate(event);
            } catch (Exception e) {
                System.err.println("[ClientSocketService] Listener xu ly realtime event bi loi: " + e.getMessage());
            }
        }
    }

    /**
     * Xu ly khi Server dong ket noi hoac socket bi loi.
     */
    private void handleConnectionClosed() {
        running = false;

        completeAllPendingResponses(
                "Server da dong ket noi.",
                "CONNECTION_CLOSED"
        );

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Mat ket noi");
            alert.setHeaderText(null);
            alert.setContentText("Mat ket noi toi Server. Vui long dang nhap lai!");
            alert.showAndWait();

            ClientSession.clear();
            ClientNetworkManager.resetConnection();
            ClientSocketService.reset();
            SceneNavigator.showLogin();
        });
    }

    /**
     * Neu socket chet, tat ca request dang cho phai duoc tra failure.
     */
    private void completeAllPendingResponses(String message, String errorCode) {
        for (Map.Entry<String, PendingRequest> entry : pendingResponses.entrySet()) {
            String requestId = entry.getKey();
            PendingRequest pendingRequest = pendingResponses.remove(requestId);

            if (pendingRequest != null) {
                pendingRequest.future.complete(
                        SocketResponse.failure(
                                requestId,
                                pendingRequest.action,
                                message,
                                errorCode
                        )
                );
            }
        }
    }

    /**
     * Dung service doc socket.
     *
     * Hien tai chua bat buoc dung, nhung de san cho logout/cleanup sau nay.
     */
    public void stop() {
        running = false;
        try {
            pingScheduler.shutdownNow();
        } catch (Exception e) {
        }

        completeAllPendingResponses(
                "ClientSocketService da dung.",
                "SOCKET_SERVICE_STOPPED"
        );
    }

    public static synchronized void reset() {
        if (instance != null) {
            instance.stop();
            instance = null;
            System.out.println("[Service] Da don dep sach thuc the ClientSocketService.");
        }
    }

    /**
     * Object noi bo dung de nho request nao dang cho response.
     */
    private static class PendingRequest {
        private final ActionType action;
        private final CompletableFuture<SocketResponse> future = new CompletableFuture<>();

        private PendingRequest(ActionType action) {
            this.action = action;
        }
    }
}
