package com.auction.dto;

import com.auction.enums.BidStatus;
import com.auction.utils.GsonProvider;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BidTransactionDTOTest {

    private final com.google.gson.Gson gson = GsonProvider.getGson();

    // Constructor cũ phải lưu đúng bidderName, amount, time, status
    @Test
    void oldConstructorShouldStoreBasicFields() {
        LocalDateTime bidTime = LocalDateTime.now();

        BidTransactionDTO dto = new BidTransactionDTO(
                "bidder01",
                1500.0,
                bidTime,
                BidStatus.ACCEPTED.name()
        );

        assertEquals("bidder01", dto.getBidderName());
        assertEquals(1500.0, dto.getAmount());
        assertSame(bidTime, dto.getTime());
        assertEquals("ACCEPTED", dto.getStatus());
    }

    // Constructor cũ chưa có anti-sniping field nên newEndTime null, liveStepPrice mặc định 0
    @Test
    void oldConstructorShouldLeaveAntiSnipingFieldsDefault() {
        LocalDateTime bidTime = LocalDateTime.now();

        BidTransactionDTO dto = new BidTransactionDTO(
                "bidder01",
                1500.0,
                bidTime,
                BidStatus.ACCEPTED.name()
        );

        assertNull(dto.getNewEndTime());
        assertEquals(0.0, dto.getLiveStepPrice());
    }

    // Constructor mới phải lưu đủ thông tin anti-sniping
    @Test
    void newConstructorShouldStoreAntiSnipingFields() {
        LocalDateTime bidTime = LocalDateTime.now();
        LocalDateTime newEndTime = bidTime.plusMinutes(2);

        BidTransactionDTO dto = new BidTransactionDTO(
                "bidder01",
                1500.0,
                bidTime,
                BidStatus.ACCEPTED.name(),
                newEndTime,
                200.0
        );

        assertEquals("bidder01", dto.getBidderName());
        assertEquals(1500.0, dto.getAmount());
        assertSame(bidTime, dto.getTime());
        assertEquals("ACCEPTED", dto.getStatus());
        assertSame(newEndTime, dto.getNewEndTime());
        assertEquals(200.0, dto.getLiveStepPrice());
    }

    // bidderName null vẫn được giữ nguyên, validate nằm ở service/mapper
    @Test
    void constructorShouldAllowNullBidderName() {
        LocalDateTime bidTime = LocalDateTime.now();

        BidTransactionDTO dto = new BidTransactionDTO(
                null,
                1500.0,
                bidTime,
                BidStatus.ACCEPTED.name()
        );

        assertNull(dto.getBidderName());
        assertEquals(1500.0, dto.getAmount());
    }

    // time null vẫn được giữ nguyên, validate nằm ở service/mapper
    @Test
    void constructorShouldAllowNullTime() {
        BidTransactionDTO dto = new BidTransactionDTO(
                "bidder01",
                1500.0,
                null,
                BidStatus.ACCEPTED.name()
        );

        assertEquals("bidder01", dto.getBidderName());
        assertNull(dto.getTime());
    }

    // status null vẫn được giữ nguyên, validate nằm ở service/mapper
    @Test
    void constructorShouldAllowNullStatus() {
        LocalDateTime bidTime = LocalDateTime.now();

        BidTransactionDTO dto = new BidTransactionDTO(
                "bidder01",
                1500.0,
                bidTime,
                null
        );

        assertEquals("bidder01", dto.getBidderName());
        assertNull(dto.getStatus());
    }

    // amount âm hiện tại vẫn được lưu, validate nằm ở service
    @Test
    void constructorShouldAllowNegativeAmount() {
        LocalDateTime bidTime = LocalDateTime.now();

        BidTransactionDTO dto = new BidTransactionDTO(
                "bidder01",
                -100.0,
                bidTime,
                BidStatus.REJECTED.name()
        );

        assertEquals(-100.0, dto.getAmount());
    }

    // liveStepPrice âm hiện tại vẫn được lưu, validate nằm ở service
    @Test
    void newConstructorShouldAllowNegativeLiveStepPrice() {
        LocalDateTime bidTime = LocalDateTime.now();
        LocalDateTime newEndTime = bidTime.plusMinutes(2);

        BidTransactionDTO dto = new BidTransactionDTO(
                "bidder01",
                1500.0,
                bidTime,
                BidStatus.ACCEPTED.name(),
                newEndTime,
                -50.0
        );

        assertEquals(-50.0, dto.getLiveStepPrice());
    }

    // DTO constructor cũ phải serialize ra JSON có field cơ bản
    @Test
    void oldDtoShouldSerializeToJson() {
        LocalDateTime bidTime = LocalDateTime.of(2026, 1, 1, 10, 30);

        BidTransactionDTO dto = new BidTransactionDTO(
                "bidder01",
                1500.0,
                bidTime,
                BidStatus.ACCEPTED.name()
        );

        String json = gson.toJson(dto);

        assertTrue(json.contains("\"bidderName\":\"bidder01\""));
        assertTrue(json.contains("\"amount\":1500"));
        assertTrue(json.contains("\"status\":\"ACCEPTED\""));
        assertTrue(json.contains("\"time\""));
    }

    // DTO constructor mới phải serialize ra JSON có newEndTime và liveStepPrice
    @Test
    void newDtoShouldSerializeAntiSnipingFieldsToJson() {
        LocalDateTime bidTime = LocalDateTime.of(2026, 1, 1, 10, 30);
        LocalDateTime newEndTime = LocalDateTime.of(2026, 1, 1, 10, 35);

        BidTransactionDTO dto = new BidTransactionDTO(
                "bidder01",
                1500.0,
                bidTime,
                BidStatus.ACCEPTED.name(),
                newEndTime,
                200.0
        );

        String json = gson.toJson(dto);

        assertTrue(json.contains("\"bidderName\":\"bidder01\""));
        assertTrue(json.contains("\"amount\":1500"));
        assertTrue(json.contains("\"status\":\"ACCEPTED\""));
        assertTrue(json.contains("\"newEndTime\""));
        assertTrue(json.contains("\"liveStepPrice\":200"));
    }

    // DTO phải deserialize lại được từ JSON constructor cũ
    @Test
    void oldDtoShouldDeserializeFromJson() {
        String json = """
                {
                  "bidderName": "bidder01",
                  "amount": 1500,
                  "time": "2026-01-01T10:30:00",
                  "status": "ACCEPTED"
                }
                """;

        BidTransactionDTO dto = gson.fromJson(json, BidTransactionDTO.class);

        assertEquals("bidder01", dto.getBidderName());
        assertEquals(1500.0, dto.getAmount());
        assertEquals(LocalDateTime.of(2026, 1, 1, 10, 30), dto.getTime());
        assertEquals("ACCEPTED", dto.getStatus());
        assertNull(dto.getNewEndTime());
        assertEquals(0.0, dto.getLiveStepPrice());
    }

    // DTO phải deserialize lại được từ JSON có anti-sniping field
    @Test
    void newDtoShouldDeserializeAntiSnipingFieldsFromJson() {
        String json = """
                {
                  "bidderName": "bidder01",
                  "amount": 1500,
                  "time": "2026-01-01T10:30:00",
                  "status": "ACCEPTED",
                  "newEndTime": "2026-01-01T10:35:00",
                  "liveStepPrice": 200
                }
                """;

        BidTransactionDTO dto = gson.fromJson(json, BidTransactionDTO.class);

        assertEquals("bidder01", dto.getBidderName());
        assertEquals(1500.0, dto.getAmount());
        assertEquals(LocalDateTime.of(2026, 1, 1, 10, 30), dto.getTime());
        assertEquals("ACCEPTED", dto.getStatus());
        assertEquals(LocalDateTime.of(2026, 1, 1, 10, 35), dto.getNewEndTime());
        assertEquals(200.0, dto.getLiveStepPrice());
    }

    // BidTransactionDTO phải nằm được trong SocketResponse body
    @Test
    void bidTransactionDtoShouldWorkInsideSocketResponseBody() {
        LocalDateTime bidTime = LocalDateTime.of(2026, 1, 1, 10, 30);
        LocalDateTime newEndTime = LocalDateTime.of(2026, 1, 1, 10, 35);

        BidTransactionDTO dto = new BidTransactionDTO(
                "bidder01",
                1500.0,
                bidTime,
                BidStatus.ACCEPTED.name(),
                newEndTime,
                200.0
        );

        SocketResponse response = SocketResponse.event(
                com.auction.enums.ActionType.BID_UPDATE,
                "Có lượt đặt giá mới.",
                dto
        );

        String json = gson.toJson(response);

        assertTrue(json.contains("\"type\":\"EVENT\""));
        assertTrue(json.contains("\"action\":\"BID_UPDATE\""));
        assertTrue(json.contains("\"bidderName\":\"bidder01\""));
        assertTrue(json.contains("\"newEndTime\""));
        assertTrue(json.contains("\"liveStepPrice\":200"));
    }
}