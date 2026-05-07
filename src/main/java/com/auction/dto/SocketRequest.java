package com.auction.dto;

import com.google.gson.JsonObject;

/**
 SocketRequest là phong bì chung cho mọi request gửi từ Client sang Server.
 Tạo SocketRequest để Sever nhìn Action là biết gọi Controller nào
 Co 2 phan:
 - action: Client muon lam gi: LOGIN, REGISTER, PLACE_BID
 - body: du lieu chi tiet cua action do duoi dang JSON
 */
public class SocketRequest {
    private String action;
    private String body;

    public SocketRequest(String login, JsonObject loginBody) {
    }

    public SocketRequest(String action, String body) {
        this.action = action;
        this.body = body;
    }

    public String getAction() {
        return action;
    }

    public String getBody() {
        return body;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
