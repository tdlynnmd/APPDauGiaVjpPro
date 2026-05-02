package com.auction.client.util;

import com.auction.server.models.User.*;
import java.util.ArrayList;
import java.util.List;

public class DemoData {
    public static List<User> getSampleUsers() {
        List<User> users = new ArrayList<>();
        users.add(new Bidder("bidder1", "bidder1@example.com", "123456"));
        users.add(new Seller("seller1", "seller1@example.com", "123456"));
        users.add(new Admin("admin1", "admin1@example.com", "123456"));
        return users;
    }
}