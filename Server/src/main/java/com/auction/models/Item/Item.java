package com.auction.models.Item;

import com.auction.models.Entity.Entity;
import com.auction.models.User.Seller;

import java.io.Serializable;

public abstract class Item extends Entity implements Serializable {
    private String sellerId;
    private String name;
    private double startingPrice;
    private String description ;
    private int yearCreated;

    public Item(String name, double startingPrice, String description, int yearCreated, String sellerId) {
        this.name = name;
        this.startingPrice = startingPrice;
        this.description = description;
        this.yearCreated = yearCreated;
        this.sellerId = sellerId;
    }


    // Constructor cho load từ DB (với ID)
    public Item(String id, String name, double startingPrice, String description,
                int yearCreated, String sellerId) {
        super(id);
        this.name = name;
        this.startingPrice = startingPrice;
        this.description = description;
        this.yearCreated = yearCreated;
        this.sellerId = sellerId;
    }



    public abstract String getInfo();

    protected String getName() {
        return this.name;
    }

    protected String getDescription() {
        return this.description;
    }

    public double getStartingPrice() {
        return this.startingPrice;
    }

    protected int getYearCreated() {
        return this.yearCreated;
    }
}
