package com.auction.models.User;


import com.auction.models.Item.Item;

import java.util.ArrayList;
import java.util.List;

public class Seller extends User {
    private transient List<Item> myItems;
    public Seller(String username, String email, String password){
        super(username,email,password, com.auction.enums.UserRole.SELLER);
        this.myItems = new ArrayList<>();
    }

    public Seller(String id,String username, String email, String password){
        super(id,username,email,password, com.auction.enums.UserRole.SELLER);
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

}
