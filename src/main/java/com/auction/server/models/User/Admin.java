package com.auction.server.models.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Admin extends User{
    private List<String> actionLogs;
    public Admin(String username, String password, String email){
        super(username, password, email);
        this.actionLogs= new ArrayList<>();
    }

    // Ghi nhận 1 hành động của Admin
    public void logAction(String action){
        String time= LocalDateTime.now().toString();
        this.actionLogs.add("[" + time + "]" +action);
    }

    @Override
    public String getRole() {
        return "Admin";
    }

    public List<String> getActionLogs(){
        return actionLogs;
    }
}
