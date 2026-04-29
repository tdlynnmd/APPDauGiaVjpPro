package com.auction.server.models.User;

import com.auction.server.models.Item.Item;

import java.util.ArrayList;
import java.util.List;

public class Seller extends User {
    private double rating;
    private transient List<Item> myItems;
    public Seller(String username, String password, String email){
        super(username,password,email);
        this.rating = 5.0;
        this.myItems = new ArrayList<>();
    }

    //Thêm vật đấu giá
    public void addItem(Item item){
        this.myItems.add(item);
    }

    // cập nhật điểm uy tín
    public void updateRating(double newRating){
        this.rating = (this.rating + newRating) / 2.0;
    }
    @Override
    public String getRole() {
        return "Seller";
    }
}
