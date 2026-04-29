package com.auction.server.service;

import com.auction.server.manage.AuctionManage;

import java.util.ArrayList;
import java.util.List;

public class NotificationService {
    private List<Subcriber> subcriberList;
    private static volatile NotificationService instance;

    private NotificationService(){
        this.subcriberList = new ArrayList<>();
    }

    public static NotificationService getInstance(){
        NotificationService temp = instance;
        if (temp == null){
            synchronized (AuctionManage.class){
                temp = instance;
                if(temp == null){
                    temp = instance  = new NotificationService();
                }
            }
        }
        return temp;
    }


    public void subcribe(Subcriber subcriber){
        subcriberList.add(subcriber);
    }

    public void unsubcribe(Subcriber subcriber){
        subcriberList.remove(subcriber);
    }

    public void notify(String context){
        subcriberList.forEach(subcriber -> subcriber.update(context));
    }
}
