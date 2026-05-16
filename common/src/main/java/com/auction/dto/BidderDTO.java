package com.auction.dto;

import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;

import java.util.ArrayList;
import java.util.List;

public class BidderDTO extends UserDTO {
    private static final long serialVersionUID = 1L;

    private List<String> joinedAuctionIds;

    public BidderDTO(String id, String username, String email, UserRole role, UserStatus status,
                     double availableBalance, double frozenBalance, List<String> joinedAuctionIds) {
        // Truyền 2 trường số dư lên lớp cha UserDTO
        super(id, username, email, role, status, availableBalance, frozenBalance);
        this.joinedAuctionIds = joinedAuctionIds != null ? new ArrayList<>(joinedAuctionIds) : new ArrayList<>();
    }

    public List<String> getJoinedAuctionIds() { return joinedAuctionIds; }

    public void setJoinedAuctionIds(List<String> joinedAuctionIds) {
        this.joinedAuctionIds = joinedAuctionIds;
    }

    public void addJoinedAuctionId(String auctionId) {
        if (!this.joinedAuctionIds.contains(auctionId)) {
            this.joinedAuctionIds.add(auctionId);
        }
    }
}