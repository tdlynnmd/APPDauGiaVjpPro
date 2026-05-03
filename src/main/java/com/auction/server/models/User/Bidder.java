package com.auction.server.models.User;

import com.auction.server.observer.Subscriber;

import java.util.ArrayList;
import java.util.List;

public class Bidder extends User implements Subscriber {
    private double balance=0; // số dư
    private List<String> joinedAuctionIds;
    public Bidder(String username,String email, String password){
        super(username, email, password,UserRole.BIDDER);
        this.joinedAuctionIds=new ArrayList<>();
    }

    @Override
    public void update(String context) {
        System.out.println("Thông báo cho "+this.getUsername()+": "+context);
    }

    //Nạp tiền
    public boolean topUp(double amount){
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

    //Hoàn tiền (khi có người khác bid cao hơn)
    public synchronized void refund(double amount){
        this.balance += amount;
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
}
