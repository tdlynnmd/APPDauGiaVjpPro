package com.auction.models.User;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Admin extends User{
    private List<String> actionLogs;
    public Admin(String username, String email, String password){
        super(username, email, password, com.auction.enums.UserRole.ADMIN);
        this.actionLogs= new ArrayList<>();
    }

    public Admin(String id,String username, String email, String password){
        super(id,username, email, password, com.auction.enums.UserRole.ADMIN);
        this.actionLogs= new ArrayList<>();
    }

    // Ghi nhận 1 hành động của Admin
    public void logAction(String action){
        String time= LocalDateTime.now().toString();
        this.actionLogs.add("[" + time + "]" +action);
    }


    public String getActionLogsJson() {
        return new Gson().toJson(actionLogs);
    }

    // Parse JSON from DB back to List
    public void setActionLogsFromJson(String json) {
        this.actionLogs = new Gson().fromJson(json,
                new TypeToken<List<String>>(){}.getType());
    }
    public List<String> getActionLogs(){
        return actionLogs;
    }
}
