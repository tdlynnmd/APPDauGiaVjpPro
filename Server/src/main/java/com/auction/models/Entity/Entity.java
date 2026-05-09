package com.auction.models.Entity;

import java.util.UUID;

public abstract class Entity {
    private String id;

    public Entity() {
        // Sinh ra một chuỗi ngẫu nhiên độc nhất
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
