package com.auction.models.Auction;

import com.auction.models.Entity.Entity;
import java.io.Serializable;
import java.time.LocalDateTime;

public class AutoBid extends Entity implements Serializable {
    private String userId;
    private String auctionId;
    private double maxBid;
    private double increment;
    private boolean isActive;
    private LocalDateTime createdAt;

    /**
     * Constructor for creating a new AutoBid
     */
    public AutoBid(String userId, String auctionId, double maxBid, double increment) {
        super();
        this.userId = userId;
        this.auctionId = auctionId;
        this.maxBid = maxBid;
        this.increment = increment;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor for loading from DB (hydration)
     */
    public AutoBid(String id, String userId, String auctionId, double maxBid, double increment, boolean isActive, LocalDateTime createdAt) {
        super(id);
        this.userId = userId;
        this.auctionId = auctionId;
        this.maxBid = maxBid;
        this.increment = increment;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public double getMaxBid() {
        return maxBid;
    }

    public void setMaxBid(double maxBid) {
        this.maxBid = maxBid;
    }

    public double getIncrement() {
        return increment;
    }

    public void setIncrement(double increment) {
        this.increment = increment;
    }

    public boolean canCover(double minimumBidRequired) {
        return isActive && maxBid >= minimumBidRequired;
    }

    public double calculateNextBidAmount(double currentPrice, double minimumBidRequired) {
        double preferredBid = currentPrice + increment;
        return Math.min(maxBid, Math.max(minimumBidRequired, preferredBid));
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
