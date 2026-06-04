package com.auction.models.User;

import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lớp biểu diễn thực thể Bidder trong hệ thống.
 */
public class Bidder extends User {
    private transient List<String> joinedAuctionIds;

    public Bidder(String username, String email, String password) {
        super(username, email, password, UserRole.BIDDER);
        this.joinedAuctionIds = new ArrayList<>();
    }

    public Bidder(String id, String username, String email, String password,
                  UserRole role, double availableBalance, double frozenBalance, UserStatus status,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id, username, email, password, role, availableBalance, frozenBalance, status, createdAt, updatedAt);
        this.joinedAuctionIds = new ArrayList<>();
    }

    public boolean addJoinedAuction(String auctionId) {
        if (!joinedAuctionIds.contains(auctionId)) {
            joinedAuctionIds.add(auctionId);
            return true;
        }
        return false;
    }

    public boolean removeJoinedAuction(String auctionId) {
        return joinedAuctionIds.remove(auctionId);
    }

    public List<String> getJoinedAuctionIds() {
        return Collections.unmodifiableList(joinedAuctionIds);
    }

    public void setJoinedAuctionIds(List<String> idsFromDB) {
        this.joinedAuctionIds = new ArrayList<>(idsFromDB);
    }
}