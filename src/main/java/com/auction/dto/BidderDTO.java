package com.auction.dto;

import com.auction.server.models.User.UserRole;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object cho Bidder
 * Chứa thông tin cơ bản của người mua và số dư tài khoản của họ
 * Không chứa thông tin nhạy cảm như mật khẩu
 */
public class BidderDTO extends UserDTO{
    private double balance;
    private List<String> joinedAuctionIds;

    public BidderDTO(String id, String username, String email, UserRole role, double balance, List<String> joinedAuctionIds) {
        super(id, username, email, role);
        this.balance = balance;
        this.joinedAuctionIds = joinedAuctionIds != null ? new ArrayList<>(joinedAuctionIds) : new ArrayList<>();
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public List<String> getJoinedAuctionIds() {
        return joinedAuctionIds;
    }

    public void setJoinedAuctionIds(List<String> joinedAuctionIds) {
        this.joinedAuctionIds = joinedAuctionIds;
    }

    public void addJoinedAuctionId(String auctionId) {
        if (!this.joinedAuctionIds.contains(auctionId)) {
            this.joinedAuctionIds.add(auctionId);
        }
    }
}

