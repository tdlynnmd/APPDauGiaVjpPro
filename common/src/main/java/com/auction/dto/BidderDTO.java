package com.auction.dto;

import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;

import java.math.BigDecimal; // Đổi sang BigDecimal
import java.util.ArrayList;
import java.util.List;

public class BidderDTO extends UserDTO {
    private static final long serialVersionUID = 1L;

    private double balance; // Đồng bộ với Model
    private List<String> joinedAuctionIds;

    public BidderDTO(String id, String username, String email, UserRole role, UserStatus status,
                     double balance, List<String> joinedAuctionIds) {
        super(id, username, email, role, status);
        this.balance = balance;
        this.joinedAuctionIds = joinedAuctionIds != null ? new ArrayList<>(joinedAuctionIds) : new ArrayList<>();
    }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public List<String> getJoinedAuctionIds() { return joinedAuctionIds; }
    public void setJoinedAuctionIds(List<String> joinedAuctionIds) { this.joinedAuctionIds = joinedAuctionIds; }

    public void addJoinedAuctionId(String auctionId) {
        if (!this.joinedAuctionIds.contains(auctionId)) {
            this.joinedAuctionIds.add(auctionId);
        }
    }
}