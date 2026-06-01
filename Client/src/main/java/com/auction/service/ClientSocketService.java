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
 * ClientSocketService la tang doc/ghi socket trung tam cua Client.
 *
 * Nhiem vu chinh:
 * - Gui SocketRequest sang Server.
 * - Chay mot thread rieng de doc moi message Server gui ve.
 * - Phan biet SocketResponse type = RESPONSE va type = EVENT.
 * - RESPONSE: tra ve dung request dang cho thong qua requestId.
 * - EVENT: gui cho cac RealtimeUpdateListener dang dang ky.
 *
 * Ly do can class nay:
 * - Neu ClientAuthApi/ClientAuctionApi tu reader.readLine(), realtime EVENT co the bi doc nham.
 * - Khi gom doc socket ve mot noi, Client co the xu ly request-response va realtime cung luc.
 */
public class ClientSocketService {
    private static final int REQUEST_TIMEOUT_SECONDS = 15;

    private static ClientSocketService instance;

    private final com.google.gson.Gson gson = GsonProvider.getGson();
    private final PrintWriter writer;
    private final BufferedReader reader;

    /*
     * Luu cac request da gui nhung chua nhan response.
     *
     * Key:
     * - requestId cua SocketRequest.
     *
     * Value:
     * - PendingRequest chua action va CompletableFuture dang cho response.
     */
    private final Map<String, PendingRequest> pendingResponses = new ConcurrentHashMap<>();

    /*
     * Danh sach cac man hinh/class muon nhan realtime event.
     *
     * CopyOnWriteArrayList an toan khi:
     * - Thread doc socket dang duyet danh sach listener.
     * - JavaFX Controller cung luc add/remove listener.
     */
    private final CopyOnWriteArrayList<RealtimeUpdateListener> realtimeListeners = new CopyOnWriteArrayList<>();

    private volatile boolean running = true;
    private volatile boolean forceLogoutHandled = false;

    private ClientSocketService() {
        ClientNetworkManager network = ClientNetworkManager.getInstance();

        this.writer = network.getWriter();
        this.reader = network.getReader();

        startReaderThread();
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
        // === CẬP NHẬT MỚI ===
        // Neu service cu da tung bi dung (do bam nút logout), ta phai tao moi lai hoan toan
        // de kich hoat lai luong Thread doc socket moi cho phien dang nhap moi.
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
            /*
             * Dua request sang JSON roi gui qua socket.
             *
             * synchronized(writer) giup tranh truong hop nhieu thread cung gui request
             * lam noi dung ghi ra socket bi xen ke.
             */
            synchronized (writer) {
                writer.println(gson.toJson(socketRequest));
                writer.flush();
            }

            /*
             * Response khong duoc doc truc tiep o day.
             * No se duoc listenToServer() doc, sau do complete future nay.
             */
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
                    handleConnectionClosed();
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
                /*
                 * Mot so broadcast cu co the van la chuoi thuong, chua phai JSON SocketResponse.
                 * Khi parse loi, bo qua message do de thread doc socket khong bi chet.
                 */
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

        if (SocketResponse.TYPE_EVENT.equals(response.getType())) {
            if (isForceLogoutEvent(response)) {
                handleForceLogout(response);
                return;
            }

            notifyRealtimeListeners(response);
            return;
        }

        if (SocketResponse.TYPE_RESPONSE.equals(response.getType())) {
            completePendingResponse(response);
            return;
        }

        /*
         * Du phong:
         * Neu response cu chua co type nhung co requestId, van co tra ve cho request dang cho.
         */
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

        completeAllPendingResponses(
                "ClientSocketService da dung.",
                "SOCKET_SERVICE_STOPPED"
        );
    }

    // === CẬP NHẬT MỚI ===
    // Ham don dep triet de, dung luong doc ngam va giai phong thuc the Singleton tren RAM
    public static synchronized void reset() {
        if (instance != null) {
            instance.stop(); // Goi ham stop co san cua ban de tat flag running va lam sach pending request
            instance = null; // Ep thuc the cu ve null de xoa khoi bo nho
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
