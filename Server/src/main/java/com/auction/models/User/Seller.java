package com.auction.models.User;


import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;
import com.auction.models.Item.Item;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Seller extends User {
    private double rating;
    private transient List<Item> myItems;
    public Seller(String username, String email, String password){
        super(username,email,password, com.auction.enums.UserRole.SELLER);
        this.myItems = new ArrayList<>();
        this.rating = 0;
    }

    public Seller(String id, String username, String email, String password,
                  UserRole role, double availableBalance, double frozenBalance, UserStatus status,
                  LocalDateTime createdAt, LocalDateTime updatedAt, double rating) {
        super(id, username, email, password, role, availableBalance, frozenBalance, status, createdAt, updatedAt);
        this.rating = rating;
        this.myItems = new ArrayList<>();
    }

    //Thêm vật đấu giá
    public void addItem(Item item){
        this.myItems.add(item);
    }

    //Xoá vật đấu giá
    public void removeItem(Item item){
        this.myItems.remove(item);
    }


    public double getRating() {
        return this.rating;
    }
}
