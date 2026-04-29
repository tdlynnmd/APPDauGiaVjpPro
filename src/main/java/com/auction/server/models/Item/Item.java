package com.auction.server.models.Item;

import com.auction.server.models.Entity.Entity;

import java.io.Serializable;

public abstract class Item extends Entity implements Serializable {
    private String name;
    private double startingPrice;
    private String description= "Không có mô tả";
    private int yearCreated;
    public Item(String name, double startingPrice, String description,int yearCreated ) {
        this.name= name;
        this.startingPrice=startingPrice;
        this.description=description;
        this.yearCreated=yearCreated;
    }

    public abstract String getInfo();

    protected String getName() {
        return this.name;
    }

    protected String getDescription() {
        return this.description;
    }

    protected double getStartingPrice() {
        return this.startingPrice;
    }

    protected int getYearCreated(){
        return this.yearCreated;
    }
}
