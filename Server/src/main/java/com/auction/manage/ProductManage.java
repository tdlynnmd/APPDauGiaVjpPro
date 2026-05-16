package com.auction.manage;

import com.auction.models.Item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductManage {
    private static volatile ProductManage instance;
    private final Map<String, Item> items;
    private ProductManage(){
        this.items = new HashMap<>();
    }

    public static ProductManage getInstance(){
        ProductManage temp = instance;
        if (temp == null){
            synchronized (ProductManage.class){
                temp = instance;
                if(temp == null){
                    temp = instance  = new ProductManage();
                }
            }
        }
        return temp;
    }

    //additem
    public synchronized boolean addProduct(Item item) {
        if (item == null) {
            System.out.println("Lỗi: Sản phẩm không được null");
            return false;
        }

        if (items.containsKey(item.getId())) {
            System.out.println("Lỗi: Sản phẩm với ID '" + item.getId() + "' đã tồn tại");
            return false;
        }

        items.put(item.getId(), item);
        System.out.println("Thêm sản phẩm thành công: " + item.getId());
        return true;
    }

    //Lấy item theo ID
    public Item getProduct(String productId) {
        if (productId == null || productId.isEmpty()) {
            System.out.println("Lỗi: ID sản phẩm không hợp lệ");
            return null;
        }
        return items.get(productId);
    }

    //Sửa thông tin items
    public synchronized boolean updateProduct(String productId, Item updatedItem) {
        if (productId == null || updatedItem == null) {
            System.out.println("Lỗi: ID sản phẩm hoặc sản phẩm mới không được null");
            return false;
        }

        if (!items.containsKey(productId)) {
            System.out.println("Lỗi: Sản phẩm với ID '" + productId + "' không tồn tại");
            return false;
        }

        updatedItem.setId(productId); // Giữ nguyên ID
        items.put(productId, updatedItem);
        System.out.println("Cập nhật sản phẩm thành công: " + productId);
        return true;
    }

    //Xoá item theo ID
    public synchronized boolean deleteProduct(String productId) {
        if (productId == null || productId.isEmpty()) {
            System.out.println("Lỗi: ID sản phẩm không hợp lệ");
            return false;
        }

        if (!items.containsKey(productId)) {
            System.out.println("Lỗi: Sản phẩm với ID '" + productId + "' không tồn tại");
            return false;
        }

        items.remove(productId);
        System.out.println("Xóa sản phẩm thành công: " + productId);
        return true;
    }

    //Lấy tất cả item
    public List<Item> getAllProducts() {
        return new ArrayList<>(items.values());
    }

    //Kiểm tra items có tồn tại ko
    public boolean isProductExists(String productId) {
        return productId != null && items.containsKey(productId);
    }

}
