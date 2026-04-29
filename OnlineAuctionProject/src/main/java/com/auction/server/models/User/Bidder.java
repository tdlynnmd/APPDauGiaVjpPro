package com.auction.server.models.User;
import com.auction.server.models.Auction.Auction;
import java.util.ArrayList;
import java.util.List;

public class Bidder extends User {
    private double balance; // số dư
    private List<String> joinedAuctionIds;
    public Bidder(String username,String password, String email, double initialBalance){
        super(username, password, email);
        this.balance=initialBalance;
        this.joinedAuctionIds=new ArrayList<>();
    }

    @Override
    public String getRole() {
        return "Bidder";
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

    /*// uỷ quền placeBid cho auction
    public boolean placeBid(Auction auction, double amount){
        //1. Kiểm tra người dùng
        if (amount > balance){
            System.out.println("Lỗi: Số dư tài khoản không đủ đặt giá này.");
            return false;
        }

        //2. Kiểm tra bên Auction
        boolean isSuccess = auction.placeBid(this, amount);

        if(isSuccess){
            System.out.println("Thông báo: Bạn đã đặt giá thành công.");
            deductBalance(amount);
        }
        return isSuccess;
    }*/
    //nen dua vo auctionService
}
