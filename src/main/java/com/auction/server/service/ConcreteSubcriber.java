package com.auction.server.service;

import com.auction.server.models.User.Bidder;

public class ConcreteSubcriber implements Subcriber{
    private Bidder bidder;
    ConcreteSubcriber(Bidder bidder){
        this.bidder=bidder;
    }

    @Override
    public void update(String context) {
        System.out.println("Thông báo: "+context);
    }
}
