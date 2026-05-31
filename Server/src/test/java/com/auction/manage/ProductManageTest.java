package com.auction.manage;

import com.auction.models.Item.Electronics;
import com.auction.models.Item.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProductManageTest {

    private ProductManage productManage;

    @BeforeEach
    void setUp() throws Exception {
        productManage = ProductManage.getInstance();
        clearProductManage();
    }

    // Clear dữ liệu RAM trong singleton trước mỗi test
    private void clearProductManage() throws Exception {
        clearMapField("items");
        clearMapField("lastAccessedTime");
    }

    // Clear một Map private bằng reflection
    private void clearMapField(String fieldName) throws Exception {
        Field field = ProductManage.class.getDeclaredField(fieldName);
        field.setAccessible(true);

        Map<?, ?> map = (Map<?, ?>) field.get(productManage);
        map.clear();
    }

    // Lấy map lastAccessedTime để kiểm tra thời gian truy cập
    @SuppressWarnings("unchecked")
    private Map<String, LocalDateTime> getLastAccessedTimeMap() throws Exception {
        Field field = ProductManage.class.getDeclaredField("lastAccessedTime");
        field.setAccessible(true);

        return (Map<String, LocalDateTime>) field.get(productManage);
    }

    // Lấy map items để kiểm tra dữ liệu RAM bên trong
    @SuppressWarnings("unchecked")
    private Map<String, Item> getItemsMap() throws Exception {
        Field field = ProductManage.class.getDeclaredField("items");
        field.setAccessible(true);

        return (Map<String, Item>) field.get(productManage);
    }

    // Tạo item mẫu có id cố định
    private Electronics sampleItem(String itemId) {
        Electronics item = new Electronics(
                "Laptop Dell",
                12000000,
                "Laptop văn phòng",
                2022,
                "seller-1",
                "dell.png",
                "Dell",
                24
        );

        item.setId(itemId);
        return item;
    }

    // =========================================================
    // getInstance()
    // =========================================================

    // getInstance nhiều lần phải trả cùng một singleton
    @Test
    void getInstanceShouldReturnSameObject() {
        ProductManage first = ProductManage.getInstance();
        ProductManage second = ProductManage.getInstance();

        assertSame(first, second);
    }

    // =========================================================
    // addProduct()
    // =========================================================

    // addProduct phải lưu item vào RAM
    @Test
    void addProductShouldStoreItemInMemory() {
        Item item = sampleItem("item-1");

        productManage.addProduct(item);

        assertSame(item, productManage.getProduct("item-1"));
        assertEquals(1, productManage.getAllProducts().size());
    }

    // addProduct phải tạo lastAccessedTime cho item
    @Test
    void addProductShouldCreateLastAccessedTime() throws Exception {
        Item item = sampleItem("item-1");

        productManage.addProduct(item);

        Map<String, LocalDateTime> lastAccessedTime = getLastAccessedTimeMap();

        assertTrue(lastAccessedTime.containsKey("item-1"));
        assertNotNull(lastAccessedTime.get("item-1"));
    }

    // addProduct(null) không được crash và không được thêm gì
    @Test
    void addProductShouldIgnoreNullItem() {
        assertDoesNotThrow(() -> {
            productManage.addProduct(null);
        });

        assertTrue(productManage.getAllProducts().isEmpty());
    }

    // addProduct trùng id không được ghi đè item cũ
    @Test
    void addProductShouldNotReplaceExistingItemWithSameId() {
        Item oldItem = sampleItem("item-1");
        Item newItem = sampleItem("item-1");
        newItem.setName("New Laptop Name");

        productManage.addProduct(oldItem);
        productManage.addProduct(newItem);

        Item result = productManage.getProduct("item-1");

        assertSame(oldItem, result);
        assertEquals("Laptop Dell", result.getName());
        assertEquals(1, productManage.getAllProducts().size());
    }

    // addProduct item có id null hiện tại có thể gây lỗi do ConcurrentHashMap không nhận null key
    @Test
    void addProductShouldThrowWhenItemIdIsNull() {
        Item item = sampleItem("item-null-id");
        item.setId(null);

        assertThrows(NullPointerException.class, () -> {
            productManage.addProduct(item);
        });
    }

    // =========================================================
    // getProduct()
    // =========================================================

    // getProduct trả đúng item theo id
    @Test
    void getProductShouldReturnItemWhenExists() {
        Item item = sampleItem("item-1");
        productManage.addProduct(item);

        Item result = productManage.getProduct("item-1");

        assertSame(item, result);
    }

    // getProduct trả null khi id không tồn tại
    @Test
    void getProductShouldReturnNullWhenItemDoesNotExist() {
        assertNull(productManage.getProduct("missing-item"));
    }

    // getProduct(null) trả null, không crash
    @Test
    void getProductShouldReturnNullWhenIdIsNull() {
        assertNull(productManage.getProduct(null));
    }

    // getProduct("") trả null, không crash
    @Test
    void getProductShouldReturnNullWhenIdIsEmpty() {
        assertNull(productManage.getProduct(""));
    }

    // getProduct phải update lastAccessedTime nếu item tồn tại
    @Test
    void getProductShouldUpdateLastAccessedTimeWhenItemExists() throws Exception {
        Item item = sampleItem("item-1");
        productManage.addProduct(item);

        Map<String, LocalDateTime> lastAccessedTime = getLastAccessedTimeMap();

        LocalDateTime oldTime = LocalDateTime.now().minusHours(1);
        lastAccessedTime.put("item-1", oldTime);

        productManage.getProduct("item-1");

        assertTrue(lastAccessedTime.get("item-1").isAfter(oldTime));
    }

    // getProduct thiếu id không được tạo lastAccessedTime rác
    @Test
    void getProductShouldNotCreateLastAccessedTimeWhenItemMissing() throws Exception {
        productManage.getProduct("missing-item");

        Map<String, LocalDateTime> lastAccessedTime = getLastAccessedTimeMap();

        assertFalse(lastAccessedTime.containsKey("missing-item"));
    }

    // =========================================================
    // updateProduct()
    // =========================================================

    // updateProduct phải cập nhật item đang có trong RAM
    @Test
    void updateProductShouldReplaceExistingItem() {
        Item oldItem = sampleItem("item-1");
        Item updatedItem = sampleItem("other-id");
        updatedItem.setName("Laptop Updated");

        productManage.addProduct(oldItem);

        boolean result = productManage.updateProduct("item-1", updatedItem);

        assertTrue(result);
        assertSame(updatedItem, productManage.getProduct("item-1"));
        assertEquals("item-1", updatedItem.getId());
        assertEquals("Laptop Updated", productManage.getProduct("item-1").getName());
    }

    // updateProduct phải update lastAccessedTime
    @Test
    void updateProductShouldUpdateLastAccessedTime() throws Exception {
        Item oldItem = sampleItem("item-1");
        Item updatedItem = sampleItem("other-id");

        productManage.addProduct(oldItem);

        Map<String, LocalDateTime> lastAccessedTime = getLastAccessedTimeMap();
        LocalDateTime oldTime = LocalDateTime.now().minusHours(1);
        lastAccessedTime.put("item-1", oldTime);

        productManage.updateProduct("item-1", updatedItem);

        assertTrue(lastAccessedTime.get("item-1").isAfter(oldTime));
    }

    // updateProduct với productId null trả false
    @Test
    void updateProductShouldReturnFalseWhenProductIdIsNull() {
        Item updatedItem = sampleItem("item-1");

        boolean result = productManage.updateProduct(null, updatedItem);

        assertFalse(result);
    }

    // updateProduct với updatedItem null trả false
    @Test
    void updateProductShouldReturnFalseWhenUpdatedItemIsNull() {
        boolean result = productManage.updateProduct("item-1", null);

        assertFalse(result);
    }

    // updateProduct với productId không tồn tại trả false
    @Test
    void updateProductShouldReturnFalseWhenProductDoesNotExist() {
        Item updatedItem = sampleItem("item-1");

        boolean result = productManage.updateProduct("missing-item", updatedItem);

        assertFalse(result);
        assertTrue(productManage.getAllProducts().isEmpty());
    }

    // updateProduct với productId rỗng hiện tại không crash, nhưng trả false vì không tồn tại
    @Test
    void updateProductShouldReturnFalseWhenProductIdIsEmpty() {
        Item updatedItem = sampleItem("item-1");

        boolean result = productManage.updateProduct("", updatedItem);

        assertFalse(result);
    }

    // =========================================================
    // deleteProduct()
    // =========================================================

    // deleteProduct phải xóa item khỏi RAM
    @Test
    void deleteProductShouldRemoveExistingItem() {
        Item item = sampleItem("item-1");
        productManage.addProduct(item);

        boolean result = productManage.deleteProduct("item-1");

        assertTrue(result);
        assertNull(productManage.getProduct("item-1"));
        assertTrue(productManage.getAllProducts().isEmpty());
    }

    // deleteProduct phải xóa cả lastAccessedTime
    @Test
    void deleteProductShouldRemoveLastAccessedTime() throws Exception {
        Item item = sampleItem("item-1");
        productManage.addProduct(item);

        productManage.deleteProduct("item-1");

        Map<String, LocalDateTime> lastAccessedTime = getLastAccessedTimeMap();

        assertFalse(lastAccessedTime.containsKey("item-1"));
    }

    // deleteProduct với id không tồn tại trả false
    @Test
    void deleteProductShouldReturnFalseWhenProductDoesNotExist() {
        boolean result = productManage.deleteProduct("missing-item");

        assertFalse(result);
    }

    // deleteProduct(null) trả false, không crash
    @Test
    void deleteProductShouldReturnFalseWhenIdIsNull() {
        boolean result = productManage.deleteProduct(null);

        assertFalse(result);
    }

    // deleteProduct("") trả false, không crash
    @Test
    void deleteProductShouldReturnFalseWhenIdIsEmpty() {
        boolean result = productManage.deleteProduct("");

        assertFalse(result);
    }

    // deleteProduct một item không được ảnh hưởng item khác
    @Test
    void deleteProductShouldOnlyRemoveTargetItem() {
        Item item1 = sampleItem("item-1");
        Item item2 = sampleItem("item-2");

        productManage.addProduct(item1);
        productManage.addProduct(item2);

        productManage.deleteProduct("item-1");

        assertNull(productManage.getProduct("item-1"));
        assertSame(item2, productManage.getProduct("item-2"));
        assertEquals(1, productManage.getAllProducts().size());
    }

    // =========================================================
    // getAllProducts()
    // =========================================================

    // getAllProducts trả list rỗng khi chưa có item nào
    @Test
    void getAllProductsShouldReturnEmptyListWhenNoProductExists() {
        List<Item> result = productManage.getAllProducts();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // getAllProducts trả tất cả item trong RAM
    @Test
    void getAllProductsShouldReturnAllItemsInMemory() {
        Item item1 = sampleItem("item-1");
        Item item2 = sampleItem("item-2");

        productManage.addProduct(item1);
        productManage.addProduct(item2);

        List<Item> result = productManage.getAllProducts();

        assertEquals(2, result.size());
        assertTrue(result.contains(item1));
        assertTrue(result.contains(item2));
    }

    // getAllProducts trả bản copy, sửa list result không làm mất dữ liệu RAM
    @Test
    void getAllProductsShouldReturnCopyList() {
        Item item = sampleItem("item-1");
        productManage.addProduct(item);

        List<Item> result = productManage.getAllProducts();
        result.clear();

        assertEquals(1, productManage.getAllProducts().size());
        assertSame(item, productManage.getProduct("item-1"));
    }

    // =========================================================
    // isProductExists()
    // =========================================================

    // isProductExists trả true nếu item tồn tại
    @Test
    void isProductExistsShouldReturnTrueWhenProductExists() {
        Item item = sampleItem("item-1");
        productManage.addProduct(item);

        assertTrue(productManage.isProductExists("item-1"));
    }

    // isProductExists trả false nếu item không tồn tại
    @Test
    void isProductExistsShouldReturnFalseWhenProductDoesNotExist() {
        assertFalse(productManage.isProductExists("missing-item"));
    }

    // isProductExists(null) trả false
    @Test
    void isProductExistsShouldReturnFalseWhenIdIsNull() {
        assertFalse(productManage.isProductExists(null));
    }

    // isProductExists("") trả false
    @Test
    void isProductExistsShouldReturnFalseWhenIdIsEmpty() {
        assertFalse(productManage.isProductExists(""));
    }

    // =========================================================
    // cleanupIdleProducts()
    // =========================================================

    // cleanupIdleProducts phải xóa item idle quá 15 phút
    @Test
    void cleanupIdleProductsShouldRemoveIdleProduct() throws Exception {
        Item item = sampleItem("item-idle");
        productManage.addProduct(item);

        Map<String, LocalDateTime> lastAccessedTime = getLastAccessedTimeMap();
        lastAccessedTime.put("item-idle", LocalDateTime.now().minusMinutes(16));

        productManage.cleanupIdleProducts();

        assertNull(productManage.getProduct("item-idle"));
        assertFalse(lastAccessedTime.containsKey("item-idle"));
    }

    // cleanupIdleProducts không xóa item chưa idle đủ 15 phút
    @Test
    void cleanupIdleProductsShouldKeepRecentlyAccessedProduct() throws Exception {
        Item item = sampleItem("item-recent");
        productManage.addProduct(item);

        Map<String, LocalDateTime> lastAccessedTime = getLastAccessedTimeMap();
        lastAccessedTime.put("item-recent", LocalDateTime.now().minusMinutes(14));

        productManage.cleanupIdleProducts();

        assertSame(item, productManage.getProduct("item-recent"));
        assertTrue(lastAccessedTime.containsKey("item-recent"));
    }

    // cleanupIdleProducts bỏ qua item không có lastAccessedTime
    @Test
    void cleanupIdleProductsShouldKeepProductWhenLastAccessedTimeMissing() throws Exception {
        Item item = sampleItem("item-no-time");
        productManage.addProduct(item);

        Map<String, LocalDateTime> lastAccessedTime = getLastAccessedTimeMap();
        lastAccessedTime.remove("item-no-time");

        productManage.cleanupIdleProducts();

        assertSame(item, productManage.getProduct("item-no-time"));
    }

    // cleanupIdleProducts chỉ xóa item idle, không xóa nhầm item khác
    @Test
    void cleanupIdleProductsShouldRemoveOnlyIdleProducts() throws Exception {
        Item idleItem = sampleItem("item-idle");
        Item recentItem = sampleItem("item-recent");

        productManage.addProduct(idleItem);
        productManage.addProduct(recentItem);

        Map<String, LocalDateTime> lastAccessedTime = getLastAccessedTimeMap();
        lastAccessedTime.put("item-idle", LocalDateTime.now().minusMinutes(16));
        lastAccessedTime.put("item-recent", LocalDateTime.now().minusMinutes(5));

        productManage.cleanupIdleProducts();

        assertNull(productManage.getProduct("item-idle"));
        assertSame(recentItem, productManage.getProduct("item-recent"));
    }

    // cleanupIdleProducts khi RAM rỗng không được crash
    @Test
    void cleanupIdleProductsShouldNotThrowWhenMemoryIsEmpty() {
        assertDoesNotThrow(() -> {
            productManage.cleanupIdleProducts();
        });
    }

    // =========================================================
    // CHECK INTERNAL CONSISTENCY
    // =========================================================

    // Sau nhiều thao tác, items và lastAccessedTime phải đồng bộ khi item còn tồn tại
    @Test
    void itemsAndLastAccessedTimeShouldStayConsistentAfterAddAndDelete() throws Exception {
        Item item = sampleItem("item-1");

        productManage.addProduct(item);
        productManage.deleteProduct("item-1");

        Map<String, Item> items = getItemsMap();
        Map<String, LocalDateTime> lastAccessedTime = getLastAccessedTimeMap();

        assertFalse(items.containsKey("item-1"));
        assertFalse(lastAccessedTime.containsKey("item-1"));
    }
}