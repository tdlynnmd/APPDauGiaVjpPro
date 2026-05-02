package com.auction.server.models.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Admin extends User{
    private List<String> actionLogs;
    public Admin(String username, String password, String email){
        super(username, email, password,UserRole.ADMIN);
        this.actionLogs= new ArrayList<>();
    }

    @Override
    public Admin createUser(String username, String email, String password, UserRole role) {
        return new Admin(username,email,password);
    }

    // Ghi nhận 1 hành động của Admin
    public void logAction(String action){
        String time= LocalDateTime.now().toString();
        this.actionLogs.add("[" + time + "]" +action);
    }


    public List<String> getActionLogs(){
        return actionLogs;
    }
}
