package com.auction.controller;

import com.auction.dto.BidTransactionDTO;
import com.auction.dto.PageDTO;
import com.auction.dto.SocketResponse;
import com.auction.network.ClientBidHistoryApi;
import com.auction.service.ClientSocketService;
import com.auction.service.RealtimeUpdateListener;
import com.auction.utils.GsonProvider;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Vai tro:
 * - Controller rieng cho bieu do tang gia cua mot phien dau gia.
 *
 * Chuc nang chinh:
 * - Nhan auctionId tu LiveBiddingController.
 * - Goi backend lay lich su bid cua dung phien.
 * - Ve duong gia tang theo tung luot dat gia.
 * - Lang nghe BID_UPDATE realtime de them diem moi vao chart.
 *
 * Luu y:
 * - Controller nay chi xu ly UI chart va dieu phoi API client.
 * - Backend van la noi quyet dinh bid hop le, gia hien tai va trang thai phien.
 */
public class BidPriceChartController implements RealtimeUpdateListener {
    private static final int CHART_PAGE = 1;
    private static final int CHART_PAGE_SIZE = 200;
    private static final String ACTION_BID_UPDATE = "BID_UPDATE";

    private final ClientBidHistoryApi bidHistoryApi = new ClientBidHistoryApi();
    private final ClientSocketService socketService = ClientSocketService.getInstance();

    /*
     * Format ngan gon cho nhan truc X.
     * Chart khong can ngay-thang-day-du vi man live chi quan tam dien bien theo thoi gian gan.
     */
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    /*
     * Series la tap hop cac diem du lieu tren LineChart.
     * Moi diem tuong ung voi mot luot bid hop le cua phien.
     */
    private final XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();

    /*
     * Dung de tranh ve trung mot bid khi:
     * - Bid da co trong lich su ban dau.
     * - Sau do server lai day BID_UPDATE realtime cung bid.
     */
    private final Set<String> plottedBidKeys = new HashSet<>();

    private String auctionId;
    private boolean listenerRegistered;

    @FXML
    private LineChart<String, Number> bidPriceChart;

    @FXML
    private CategoryAxis bidTimeAxis;

    @FXML
    private NumberAxis bidAmountAxis;

    /**
     * Vai tro:
     * - Khoi tao cau hinh tinh cua chart sau khi FXML load xong.
     *
     * Chuc nang chinh:
     * - Gan series vao LineChart.
     * - Tat animation de realtime update khong gay giat UI.
     * - Cau hinh label truc X/Y.
     *
     * Luu y:
     * - Tai thoi diem initialize(), auctionId thuong chua duoc truyen vao.
     * - Du lieu that se duoc load trong setAuctionId().
     */
    @FXML
    public void initialize() {
        priceSeries.setName("Gia dat");

        if (bidPriceChart != null) {
            bidPriceChart.setAnimated(false);
            bidPriceChart.setLegendVisible(false);
            bidPriceChart.setCreateSymbols(true);
            bidPriceChart.getData().setAll(priceSeries);
        }

        if (bidTimeAxis != null) {
            bidTimeAxis.setLabel("Luot dat gia");
        }

        if (bidAmountAxis != null) {
            bidAmountAxis.setLabel("Gia dat (VND)");
            bidAmountAxis.setForceZeroInRange(false);
        }
    }

    /**
     * Vai tro:
     * - Nhan auctionId tu controller cha.
     *
     * Chuc nang chinh:
     * - Luu auctionId hien tai.
     * - Xoa chart cu neu dang hien phien khac.
     * - Dang ky realtime listener.
     * - Load lich su bid ban dau cua phien de ve chart.
     */
    public void setAuctionId(String auctionId) {
        if (isBlank(auctionId)) {
            return;
        }

        this.auctionId = auctionId.trim();
        clearChart();
        registerRealtimeListener();
        loadInitialHistory(this.auctionId);
    }

    /**
     * Vai tro:
     * - Lay du lieu lich su bid ban dau cho chart.
     *
     * Chuc nang chinh:
     * - Goi GET_AUCTION_BID_HISTORY tren thread nen.
     * - Parse ket qua thanh PageDTO<BidTransactionDTO>.
     * - Sau khi thanh cong, ve chart tren JavaFX Application Thread.
     *
     * Luu y:
     * - Khong goi API socket tren UI thread de tranh treo giao dien.
     */
    private void loadInitialHistory(String targetAuctionId) {
        Task<PageDTO<BidTransactionDTO>> task = new Task<>() {
            @Override
            protected PageDTO<BidTransactionDTO> call() {
                SocketResponse response = bidHistoryApi.getAuctionBidHistory(
                        targetAuctionId,
                        CHART_PAGE,
                        CHART_PAGE_SIZE
                );
                return bidHistoryApi.parseAuctionBidHistoryPage(response);
            }
        };

        task.setOnSucceeded(event -> {
            /*
             * Neu nguoi dung da chuyen sang phien khac trong luc request dang chay,
             * khong ve du lieu cu len chart moi.
             */
            if (targetAuctionId.equals(this.auctionId)) {
                renderChart(task.getValue());
            }
        });

        task.setOnFailed(event -> clearChart());

        Thread worker = new Thread(task, "bid-price-chart-loader");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Vai tro:
     * - Chuyen PageDTO tu backend thanh cac diem tren chart.
     *
     * Chuc nang chinh:
     * - Lay danh sach bid trong page.
     * - Dao thu tu vi backend dang tra bid moi nhat truoc.
     * - Them tung bid vao series theo thu tu cu -> moi.
     */
    private void renderChart(PageDTO<BidTransactionDTO> page) {
        List<BidTransactionDTO> bids = page == null || page.getData() == null
                ? new ArrayList<>()
                : new ArrayList<>(page.getData());

        Collections.reverse(bids);

        clearChart();

        for (BidTransactionDTO bid : bids) {
            appendBidToChart(bid);
        }
    }

    /**
     * Vai tro:
     * - Them mot bid thanh mot diem tren bieu do.
     *
     * Chuc nang chinh:
     * - Bo qua bid null, bid khong co thoi gian, hoac bid REJECTED.
     * - Chong trung bid bang bidId.
     * - Tao nhan truc X theo so thu tu luot bid va gio bid.
     * - Them amount vao truc Y.
     */
    private void appendBidToChart(BidTransactionDTO bid) {
        if (bid == null || bid.getTime() == null) {
            return;
        }

        if ("REJECTED".equalsIgnoreCase(safeText(bid.getStatus()))) {
            return;
        }

        String bidKey = buildBidKey(bid);
        if (!plottedBidKeys.add(bidKey)) {
            return;
        }

        int nextIndex = priceSeries.getData().size() + 1;
        String xLabel = nextIndex + " - " + bid.getTime().format(timeFormatter);

        priceSeries.getData().add(new XYChart.Data<>(xLabel, bid.getAmount()));

        if (priceSeries.getData().size() > CHART_PAGE_SIZE) {
            priceSeries.getData().remove(0);
        }
    }

    /**
     * Vai tro:
     * - Nhan event realtime tu ClientSocketService.
     *
     * Chuc nang chinh:
     * - Chi xu ly BID_UPDATE.
     * - Chi xu ly event thuoc auctionId hien tai.
     * - Lay bidTransaction trong body va append vao chart.
     *
     * Luu y:
     * - Ham nay duoc goi tu thread doc socket.
     * - Moi thao tac cap nhat JavaFX UI phai boc trong Platform.runLater().
     */
    @Override
    public void onRealtimeUpdate(SocketResponse event) {
        if (event == null || event.getAction() == null) {
            return;
        }

        if (!ACTION_BID_UPDATE.equals(event.getAction())) {
            return;
        }

        JsonObject body = event.getBody() != null && event.getBody().isJsonObject()
                ? event.getBody().getAsJsonObject()
                : null;

        if (body == null || !isEventForCurrentAuction(body)) {
            return;
        }

        if (body.has("bidTransaction") && !body.get("bidTransaction").isJsonNull()) {
            BidTransactionDTO newBid = GsonProvider.getGson()
                    .fromJson(body.get("bidTransaction"), BidTransactionDTO.class);

            Platform.runLater(() -> appendBidToChart(newBid));
        }
    }

    /**
     * Vai tro:
     * - Don dep controller khi man hinh cha roi khoi live room.
     *
     * Chuc nang chinh:
     * - Go realtime listener khoi ClientSocketService.
     *
     * Luu y:
     * - Neu khong go listener, controller cu co the van nhan BID_UPDATE sau khi da roi man.
     */
    public void dispose() {
        unregisterRealtimeListener();
    }

    /**
     * Vai tro:
     * - Kiem tra event realtime co thuoc phien dau gia dang xem hay khong.
     */
    private boolean isEventForCurrentAuction(JsonObject body) {
        String roomId = readString(body, "roomId");
        return !isBlank(auctionId) && auctionId.equals(roomId);
    }

    /**
     * Vai tro:
     * - Doc an toan mot field String tu JsonObject.
     */
    private String readString(JsonObject body, String fieldName) {
        if (body == null || !body.has(fieldName) || body.get(fieldName).isJsonNull()) {
            return null;
        }
        return body.get(fieldName).getAsString();
    }

    /**
     * Vai tro:
     * - Tao khoa duy nhat cho moi bid de tranh ve trung diem.
     *
     * Chuc nang chinh:
     * - Uu tien bidId neu backend co tra ve.
     * - Neu bidId null, fallback bang bidderName + amount + time.
     */
    private String buildBidKey(BidTransactionDTO bid) {
        if (!isBlank(bid.getBidId())) {
            return bid.getBidId();
        }
        return safeText(bid.getBidderName()) + "|" + bid.getAmount() + "|" + bid.getTime();
    }

    /**
     * Vai tro:
     * - Xoa toan bo du lieu chart hien tai.
     */
    private void clearChart() {
        plottedBidKeys.clear();
        priceSeries.getData().clear();
    }

    /**
     * Vai tro:
     * - Dang ky controller chart vao he thong realtime.
     *
     * Chuc nang chinh:
     * - Dam bao moi controller chi duoc add listener mot lan.
     */
    private void registerRealtimeListener() {
        if (listenerRegistered) {
            return;
        }

        socketService.addRealtimeListener(this);
        listenerRegistered = true;
    }

    /**
     * Vai tro:
     * - Huy dang ky realtime listener cua controller chart.
     */
    private void unregisterRealtimeListener() {
        if (!listenerRegistered) {
            return;
        }

        socketService.removeRealtimeListener(this);
        listenerRegistered = false;
    }

    /**
     * Vai tro:
     * - Kiem tra chuoi null/rong de tranh NullPointerException.
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Vai tro:
     * - Chuan hoa chuoi hien thi/so sanh.
     */
    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}