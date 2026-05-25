package com.auction.models.Item;

import com.auction.enums.ItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ItemFactoryTest {

    /**
     * Hàm chạy thiết lập trước mỗi Test Case.
     * 🔥 ĐỒNG BỘ MỚI: Khóa đăng ký (Registry Key) đã được chuyển đổi từ String sang ItemType Enum chuẩn hóa.
     */
    @BeforeEach
    void setUp() {
        ItemFactory.register(ItemType.ART, new ArtFactory());
        ItemFactory.register(ItemType.VEHICLES, new VehicleFactory()); // Giả sử Enum của bạn là VEHICLES hoặc VEHICLE
        ItemFactory.register(ItemType.ELECTRONICS, new ElectronicsFactory());
    }

    private Map<String, Object> electronicsData() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Laptop Dell");
        data.put("startingPrice", 12000000.0);
        data.put("description", "Laptop văn phòng");
        data.put("yearCreated", 2022);
        data.put("sellerId", "seller-1");
        data.put("imageUrl", "dell.png");
        data.put("brand", "Dell");
        data.put("warrantyMonths", 24);
        return data;
    }

    private Map<String, Object> artData() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Mona Lisa");
        data.put("startingPrice", 50000000.0);
        data.put("description", "Tranh nổi tiếng");
        data.put("yearCreated", 1503);
        data.put("sellerId", "seller-2");
        data.put("imageUrl", "mona-lisa.png");
        data.put("painter", "Leonardo da Vinci");
        data.put("artStyle", "Renaissance");
        return data;
    }

    private Map<String, Object> vehicleData() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Toyota Camry");
        data.put("startingPrice", 600000000.0);
        data.put("description", "Xe còn mới");
        data.put("yearCreated", 2020);
        data.put("sellerId", "seller-3");
        data.put("imageUrl", "camry.png");
        data.put("model", "Camry 2.5Q");
        data.put("engineType", "Gasoline");
        data.put("licensePlate", "30A-12345");
        data.put("kmAge", 25000.5);
        return data;
    }

    @Test
    @DisplayName("Test tạo thành công thực thể đồ điện tử từ Factory")
    void createItemShouldCreateElectronicsWhenTypeIsElectronics() {
        // 🔥 ĐỒNG BỘ MỚI: Gọi hàm bằng ItemType.ELECTRONICS Enum thay vì chuỗi "ELECTRONICS"
        Item item = ItemFactory.createItem(ItemType.ELECTRONICS, electronicsData());

        assertInstanceOf(Electronics.class, item);
        Electronics electronics = (Electronics) item;

        assertEquals("Laptop Dell", electronics.getName());
        assertEquals(12000000.0, electronics.getStartingPrice());
        assertEquals("Laptop văn phòng", electronics.getDescription());
        assertEquals(2022, electronics.getYearCreated());
        assertEquals("seller-1", electronics.getSellerId());
        assertEquals("dell.png", electronics.getImageUrl());
        assertEquals(ItemType.ELECTRONICS, electronics.getItemType());

        assertEquals("Dell", electronics.getBrand());
        assertEquals(24, electronics.getWarrantyMonths());
    }

    @Test
    @DisplayName("Test tạo thành công thực thể tác phẩm nghệ thuật từ Factory")
    void createItemShouldCreateArtWhenTypeIsArt() {
        // 🔥 ĐỒNG BỘ MỚI: Gọi hàm bằng ItemType.ART Enum thay vì chuỗi "ART"
        Item item = ItemFactory.createItem(ItemType.ART, artData());

        assertInstanceOf(Art.class, item);
        Art art = (Art) item;

        assertEquals("Mona Lisa", art.getName());
        assertEquals(50000000.0, art.getStartingPrice());
        assertEquals("Tranh nổi tiếng", art.getDescription());
        assertEquals(1503, art.getYearCreated());
        assertEquals("seller-2", art.getSellerId());
        assertEquals("mona-lisa.png", art.getImageUrl());
        assertEquals(ItemType.ART, art.getItemType());

        assertEquals("Leonardo da Vinci", art.getPainter());
        assertEquals("Renaissance", art.getArtStyle());
    }

    @Test
    @DisplayName("Test tạo thành công thực thể phương tiện giao thông từ Factory")
    void createItemShouldCreateVehicleWhenTypeIsVehicle() {
        // 🔥 ĐỒNG BỘ MỚI: Gọi hàm bằng ItemType.VEHICLES Enum thay vì chuỗi "VEHICLE"
        Item item = ItemFactory.createItem(ItemType.VEHICLES, vehicleData());

        assertInstanceOf(Vehicle.class, item);
        Vehicle vehicle = (Vehicle) item;

        assertEquals("Toyota Camry", vehicle.getName());
        assertEquals(600000000.0, vehicle.getStartingPrice());
        assertEquals("Xe còn mới", vehicle.getDescription());
        assertEquals(2020, vehicle.getYearCreated());
        assertEquals("seller-3", vehicle.getSellerId());
        assertEquals("camry.png", vehicle.getImageUrl());
        assertEquals(ItemType.VEHICLES, vehicle.getItemType());

        assertEquals("Camry 2.5Q", vehicle.getModel());
        assertEquals("Gasoline", vehicle.getEngineType());
        assertEquals("30A-12345", vehicle.getLicensePlate());
        assertEquals(25000.5, vehicle.getKmage());
    }

    /**
     * 🔥 CHÚ Ý REFACTOR: Test Case "createItemShouldIgnoreCaseOfType" CŨ ĐÃ BỊ LOẠI BỎ.
     * Vì hệ thống hiện tại ép kiểu tham số đầu vào bắt buộc phải là Enum Type-safe,
     * việc truyền chuỗi thường "electronics" không thể xảy ra ở tầng Compile nữa.
     * Thay vào đó, ta viết ca kiểm thử đảm bảo lỗi quăng ra chuẩn khi truyền null hoặc lỗi nạp thiếu loại hỗ trợ.
     */
    @Test
    @DisplayName("Test quăng lỗi quăng Exception khi truyền loại Item rỗng (null)")
    void createItemShouldThrowExceptionWhenTypeIsNull() {
        Map<String, Object> data = electronicsData();

        assertThrows(IllegalArgumentException.class, () -> ItemFactory.createItem(null, data));
    }

    @Test
    @DisplayName("Test thiếu trường dữ liệu chữ (String) bắt buộc")
    void createElectronicsShouldThrowExceptionWhenRequiredStringIsMissing() {
        Map<String, Object> data = electronicsData();
        data.remove("name");

        assertThrows(IllegalArgumentException.class, () -> ItemFactory.createItem(ItemType.ELECTRONICS, data));
    }

    @Test
    @DisplayName("Test trường dữ liệu bắt buộc bị để trống hoặc chứa toàn ký tự trắng")
    void createElectronicsShouldThrowExceptionWhenRequiredStringIsBlank() {
        Map<String, Object> data = electronicsData();
        data.put("name", "   ");

        assertThrows(IllegalArgumentException.class, () -> ItemFactory.createItem(ItemType.ELECTRONICS, data));
    }

    @Test
    @DisplayName("Test trường kiểu số thực (Double) nhận dữ liệu sai định dạng")
    void createElectronicsShouldThrowExceptionWhenRequiredDoubleIsInvalid() {
        Map<String, Object> data = electronicsData();
        data.put("startingPrice", "abc");

        assertThrows(IllegalArgumentException.class, () -> ItemFactory.createItem(ItemType.ELECTRONICS, data));
    }

    @Test
    @DisplayName("Test trường kiểu số nguyên (Int) nhận dữ liệu sai định dạng")
    void createElectronicsShouldThrowExceptionWhenRequiredIntIsInvalid() {
        Map<String, Object> data = electronicsData();
        data.put("yearCreated", "abc");

        assertThrows(IllegalArgumentException.class, () -> ItemFactory.createItem(ItemType.ELECTRONICS, data));
    }

    @Test
    @DisplayName("Test sử dụng chuỗi mô tả mặc định khi trường optional bị khuyết thiếu")
    void createElectronicsShouldUseDefaultDescriptionWhenDescriptionIsMissing() {
        Map<String, Object> data = electronicsData();
        data.remove("description");

        Electronics item = (Electronics) ItemFactory.createItem(ItemType.ELECTRONICS, data);
        assertEquals("Không có mô tả", item.getDescription());
    }

    @Test
    @DisplayName("Test sử dụng đường dẫn ảnh mặc định khi trường ảnh optional bị khuyết thiếu")
    void createElectronicsShouldUseDefaultImageUrlWhenImageUrlIsMissing() {
        Map<String, Object> data = electronicsData();
        data.remove("imageUrl");

        Electronics item = (Electronics) ItemFactory.createItem(ItemType.ELECTRONICS, data);
        assertEquals("default_electronics.png", item.getImageUrl());
    }

    @Test
    @DisplayName("Test khả năng tự động bốc ép kiểu dữ liệu từ chuỗi String sang Double hợp lệ")
    void createElectronicsShouldParseDoubleFromString() {
        Map<String, Object> data = electronicsData();
        data.put("startingPrice", "12000000");

        Electronics item = (Electronics) ItemFactory.createItem(ItemType.ELECTRONICS, data);
        assertEquals(12000000.0, item.getStartingPrice());
    }

    @Test
    @DisplayName("Test khả năng tự động bốc ép kiểu dữ liệu từ chuỗi String sang Integer hợp lệ")
    void createElectronicsShouldParseIntFromString() {
        Map<String, Object> data = electronicsData();
        data.put("yearCreated", "2022");

        Electronics item = (Electronics) ItemFactory.createItem(ItemType.ELECTRONICS, data);
        assertEquals(2022, item.getYearCreated());
    }
}