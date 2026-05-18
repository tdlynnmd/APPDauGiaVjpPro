package com.auction.dto;

import java.util.UUID;

import com.google.gson.JsonObject;

/**
 SocketRequest là phong bì chung cho mọi request gửi từ Client sang Server.
 Tạo SocketRequest để Sever nhìn Action là biết gọi Controller nào
 Co 2 phan:
 - action: Client muon lam gi: LOGIN, REGISTER, PLACE_BID
 - body: du lieu chi tiet cua action do duoi dang JSON
 - requestId dùng để ghép response trả về với request đã gửi.

 */

public class SocketRequest {
    private String requestId;
    private String action;
    private String body;

    /**
      Constructor rỗng cần cho Gson khi parse JSON thành object.
     */
    public SocketRequest() {
    }

    /**
      Dùng khi Client tạo request mới.
      requestId tự sinh để Client/Server có thể theo dõi request.
     */
    public SocketRequest(String action, JsonObject body) {
        this.requestId = UUID.randomUUID().toString();
        this.action = action;
        this.body = body.toString();
    }
    
    // Dùng khi body đã là JSON string.
     
    public SocketRequest(String action, String body) {
        this.requestId = UUID.randomUUID().toString();
        this.action = action;
        this.body = body;
    }


    public String getRequestId() {
        return requestId;
    }

    public String getAction() {
        return action;
    }

    public String getBody() {
        return body;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setBody(String body) {
        this.body = body;
    }
}