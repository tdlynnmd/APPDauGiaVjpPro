package com.auction.models.User;

import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;
import com.auction.observer.Subscriber;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Bidder extends User implements Subscriber {
    // Chỉ lưu trên RAM, DAO sẽ tự động query bảng phụ để đắp vào đây khi load
    private transient List<String> joinedAuctionIds;

    // Constructor 1: Đăng ký mới
    public Bidder(String username, String email, String password) {
        super(username, email, password, UserRole.BIDDER);
        this.joinedAuctionIds = new ArrayList<>();
    }

    // Constructor 2: Load từ DB (Đã xóa tham số double rating thừa)
    public Bidder(String id, String username, String email, String password,
                  UserRole role, double availableBalance, double frozenBalance, UserStatus status,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id, username, email, password, role, availableBalance, frozenBalance, status, createdAt, updatedAt);
        this.joinedAuctionIds = new ArrayList<>();
    }

    @Override
    public void update(String context) {
        System.out.println("Thông báo (Real-time) tới Bidder [" + this.getUsername() + "]: " + context);
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

    // Trả về Unmodifiable List để bảo vệ dữ liệu không bị sửa bậy từ bên ngoài
    public List<String> getJoinedAuctionIds() {
        return Collections.unmodifiableList(joinedAuctionIds);
    }

    // Hàm này dành cho DAO bơm dữ liệu từ bảng trung gian lên
    public void setJoinedAuctionIds(List<String> idsFromDB) {
        this.joinedAuctionIds = new ArrayList<>(idsFromDB);
    }
}