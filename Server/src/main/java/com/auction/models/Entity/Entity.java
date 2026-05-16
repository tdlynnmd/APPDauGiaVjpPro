package com.auction.models.Entity;

import java.util.UUID;

public abstract class Entity {
    private String id;

    public Entity() {
        // Sinh ra một chuỗi ngẫu nhiên độc nhất
        this.id = UUID.randomUUID().toString();
    }

    // Constructor cho load từ DB (ID cho trước)
    public Entity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
