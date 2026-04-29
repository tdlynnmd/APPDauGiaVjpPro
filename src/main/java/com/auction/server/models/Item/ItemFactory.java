package com.auction.server.models.Item;

import com.auction.server.models.Entity.Entity;
import java.util.HashMap;
import java.util.Map;

public abstract class ItemFactory extends Entity {
    private static final Map<String, ItemFactory> registry = new HashMap<>();

    public static void register(String type, ItemFactory factory) {
        registry.put(type.toUpperCase(), factory);
    }
    //Đây là factory method, chỉ quan tâm tạo, ko quan tâm loại
    protected abstract Item createItem(Map<String, Object> data);

    //Đây là hàm tạo Item bên ngoài, ko cần phải if else bên ngoài main, dễ dàng mở rộng chỉ cần tạo class factory mới extends ItemFactory , đăng nhập ở ngoài main 1 lan
    public static Item createItem(String type,Map<String, Object> data ){
        return registry.get(type).createItem(data);
    }
}
