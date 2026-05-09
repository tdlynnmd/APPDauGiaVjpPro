package com.auction.models.User;


import com.auction.models.Item.Item;

import java.util.ArrayList;
import java.util.List;

public class Seller extends User {
    private double rating;
    private transient List<Item> myItems;
    public Seller(String username, String email, String password){
        super(username,email,password, com.auction.enums.UserRole.SELLER);
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

    // Getter cho rating
    public double getRating() {
        return rating;
    }
}
