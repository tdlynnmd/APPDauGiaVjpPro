package com.auction.models.User;


import com.auction.observer.Subscriber;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class Bidder extends User implements Subscriber {
    private double balance=0; // số dư
    private List<String> joinedAuctionIds;
    public Bidder(String username,String email, String password){
        super(username, email, password, com.auction.enums.UserRole.BIDDER);
        this.joinedAuctionIds=new ArrayList<>();
    }

    public Bidder(String id,String username,String email, String password){
        super(id,username, email, password, com.auction.enums.UserRole.BIDDER);
        this.joinedAuctionIds=new ArrayList<>();
    }

    @Override
    public void update(String context) {
        System.out.println("Thông báo cho "+this.getUsername()+": "+context);
    }

    //Nạp tiền
    public synchronized boolean addBalance(double amount){
        if(amount>0){
            this.balance += amount;
            return true;
        }
        return false;
    }

    //Trừ tiền khi đặt giá
    public synchronized boolean deductBalance(double amount){
        if(this.balance >= amount){
            this.balance -= amount;
            return true;
        }
        return false;
    }


    // Ghi nhận tham gia thêm 1 phiên đấu giá
    public boolean addJoinedAuction(String auctionId){
        if(!joinedAuctionIds.contains(auctionId)){
            joinedAuctionIds.add(auctionId);
            return true;
        }
        return false;
    }

    // Getter cho balance
    public double getBalance() {
        return balance;
    }

    // Getter cho joinedAuctionIds
    public List<String> getJoinedAuctionIds() {
        return new ArrayList<>(joinedAuctionIds);
    }

    public String getJoinedAuctionIdsJson() {
        return new Gson().toJson(joinedAuctionIds);
    }

    // Parse JSON from DB back to List
    public void setJoinedAuctionIdsFromJson(String json) {
        this.joinedAuctionIds = new Gson().fromJson(json,
                new TypeToken<List<String>>(){}.getType());
    }
}
