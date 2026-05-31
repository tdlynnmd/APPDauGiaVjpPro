package com.auction.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuctionEventBusTest {

    private AuctionEventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = AuctionEventBus.getInstance();

        // Vì AuctionEventBus là singleton nên phải clear trước mỗi test
        eventBus.clearAllObservers();
    }

    // Tạo event mẫu để publish
    private AuctionEvent sampleEvent() {
        return new AuctionEvent(
                "auction-1",
                AuctionEventType.TIMER_TICK,
                30
        );
    }

    // Observer giả: lưu lại event đã nhận
    private static class FakeObserver implements AuctionObserver {
        List<AuctionEvent> receivedEvents = new ArrayList<>();

        @Override
        public void update(AuctionEvent event) {
            receivedEvents.add(event);
        }
    }

    // Observer giả: cố tình ném lỗi khi nhận event
    private static class BrokenObserver implements AuctionObserver {
        @Override
        public void update(AuctionEvent event) {
            throw new RuntimeException("fake observer error");
        }
    }

    // =========================================================
    // getInstance()
    // =========================================================

    // getInstance nhiều lần phải trả về cùng singleton
    @Test
    void getInstanceShouldReturnSameObject() {
        AuctionEventBus first = AuctionEventBus.getInstance();
        AuctionEventBus second = AuctionEventBus.getInstance();

        assertSame(first, second);
    }

    // =========================================================
    // attach()
    // =========================================================

    // attach observer hợp lệ thì observerCount tăng
    @Test
    void attachShouldAddObserver() {
        FakeObserver observer = new FakeObserver();

        eventBus.attach(observer);

        assertEquals(1, eventBus.observerCount());
    }

    // attach null thì không crash và không tăng observerCount
    @Test
    void attachShouldIgnoreNullObserver() {
        assertDoesNotThrow(() -> {
            eventBus.attach(null);
        });

        assertEquals(0, eventBus.observerCount());
    }

    // attach nhiều observer thì observerCount tăng đúng
    @Test
    void attachShouldAddMultipleObservers() {
        FakeObserver observer1 = new FakeObserver();
        FakeObserver observer2 = new FakeObserver();

        eventBus.attach(observer1);
        eventBus.attach(observer2);

        assertEquals(2, eventBus.observerCount());
    }

    // attach cùng observer 2 lần hiện tại sẽ bị duplicate
    @Test
    void attachSameObserverTwiceShouldRegisterTwiceInCurrentCode() {
        FakeObserver observer = new FakeObserver();

        eventBus.attach(observer);
        eventBus.attach(observer);

        assertEquals(2, eventBus.observerCount());
    }

    // =========================================================
    // detach()
    // =========================================================

    // detach observer đã attach thì observerCount giảm
    @Test
    void detachShouldRemoveObserver() {
        FakeObserver observer = new FakeObserver();

        eventBus.attach(observer);
        eventBus.detach(observer);

        assertEquals(0, eventBus.observerCount());
    }

    // detach null thì không crash
    @Test
    void detachShouldIgnoreNullObserver() {
        assertDoesNotThrow(() -> {
            eventBus.detach(null);
        });

        assertEquals(0, eventBus.observerCount());
    }

    // detach observer chưa attach thì không crash và count không đổi
    @Test
    void detachShouldNotThrowWhenObserverDoesNotExist() {
        FakeObserver observer1 = new FakeObserver();
        FakeObserver observer2 = new FakeObserver();

        eventBus.attach(observer1);
        eventBus.detach(observer2);

        assertEquals(1, eventBus.observerCount());
    }

    // detach một observer không được xóa observer khác
    @Test
    void detachShouldOnlyRemoveTargetObserver() {
        FakeObserver observer1 = new FakeObserver();
        FakeObserver observer2 = new FakeObserver();

        eventBus.attach(observer1);
        eventBus.attach(observer2);

        eventBus.detach(observer1);

        assertEquals(1, eventBus.observerCount());

        AuctionEvent event = sampleEvent();
        eventBus.publish(event);

        assertTrue(observer1.receivedEvents.isEmpty());
        assertEquals(1, observer2.receivedEvents.size());
        assertSame(event, observer2.receivedEvents.get(0));
    }

    // detach khi observer bị attach trùng chỉ xóa một lần
    @Test
    void detachShouldRemoveOnlyOneDuplicateObserverInCurrentCode() {
        FakeObserver observer = new FakeObserver();

        eventBus.attach(observer);
        eventBus.attach(observer);

        eventBus.detach(observer);

        assertEquals(1, eventBus.observerCount());
    }

    // =========================================================
    // publish()
    // =========================================================

    // publish event hợp lệ thì observer nhận được event
    @Test
    void publishShouldSendEventToObserver() {
        FakeObserver observer = new FakeObserver();
        AuctionEvent event = sampleEvent();

        eventBus.attach(observer);
        eventBus.publish(event);

        assertEquals(1, observer.receivedEvents.size());
        assertSame(event, observer.receivedEvents.get(0));
    }

    // publish phải gửi event tới tất cả observer
    @Test
    void publishShouldSendEventToAllObservers() {
        FakeObserver observer1 = new FakeObserver();
        FakeObserver observer2 = new FakeObserver();
        AuctionEvent event = sampleEvent();

        eventBus.attach(observer1);
        eventBus.attach(observer2);

        eventBus.publish(event);

        assertEquals(1, observer1.receivedEvents.size());
        assertEquals(1, observer2.receivedEvents.size());

        assertSame(event, observer1.receivedEvents.get(0));
        assertSame(event, observer2.receivedEvents.get(0));
    }

    // publish null thì không crash và không gửi gì
    @Test
    void publishShouldIgnoreNullEvent() {
        FakeObserver observer = new FakeObserver();

        eventBus.attach(observer);

        assertDoesNotThrow(() -> {
            eventBus.publish(null);
        });

        assertTrue(observer.receivedEvents.isEmpty());
    }

    // publish khi không có observer thì không crash
    @Test
    void publishShouldNotThrowWhenNoObserverExists() {
        AuctionEvent event = sampleEvent();

        assertDoesNotThrow(() -> {
            eventBus.publish(event);
        });

        assertEquals(0, eventBus.observerCount());
    }

    // observer bị detach rồi thì không nhận event nữa
    @Test
    void publishShouldNotSendEventToDetachedObserver() {
        FakeObserver observer = new FakeObserver();
        AuctionEvent event = sampleEvent();

        eventBus.attach(observer);
        eventBus.detach(observer);

        eventBus.publish(event);

        assertTrue(observer.receivedEvents.isEmpty());
    }

    // nếu một observer lỗi thì observer khác vẫn phải nhận event
    @Test
    void publishShouldContinueWhenOneObserverThrowsException() {
        BrokenObserver brokenObserver = new BrokenObserver();
        FakeObserver normalObserver = new FakeObserver();
        AuctionEvent event = sampleEvent();

        eventBus.attach(brokenObserver);
        eventBus.attach(normalObserver);

        assertDoesNotThrow(() -> {
            eventBus.publish(event);
        });

        assertEquals(1, normalObserver.receivedEvents.size());
        assertSame(event, normalObserver.receivedEvents.get(0));
    }

    // attach trùng observer thì hiện tại publish sẽ gọi update 2 lần
    @Test
    void publishShouldCallDuplicateObserverTwiceInCurrentCode() {
        FakeObserver observer = new FakeObserver();
        AuctionEvent event = sampleEvent();

        eventBus.attach(observer);
        eventBus.attach(observer);

        eventBus.publish(event);

        assertEquals(2, observer.receivedEvents.size());
    }

    // =========================================================
    // clearAllObservers()
    // =========================================================

    // clearAllObservers phải xóa sạch observer
    @Test
    void clearAllObserversShouldRemoveAllObservers() {
        eventBus.attach(new FakeObserver());
        eventBus.attach(new FakeObserver());

        eventBus.clearAllObservers();

        assertEquals(0, eventBus.observerCount());
    }

    // clearAllObservers khi rỗng không crash
    @Test
    void clearAllObserversShouldNotThrowWhenEmpty() {
        assertDoesNotThrow(() -> {
            eventBus.clearAllObservers();
        });

        assertEquals(0, eventBus.observerCount());
    }

    // sau clearAllObservers, observer cũ không nhận event nữa
    @Test
    void clearAllObserversShouldStopOldObserversFromReceivingEvents() {
        FakeObserver observer = new FakeObserver();
        AuctionEvent event = sampleEvent();

        eventBus.attach(observer);
        eventBus.clearAllObservers();

        eventBus.publish(event);

        assertTrue(observer.receivedEvents.isEmpty());
    }
}