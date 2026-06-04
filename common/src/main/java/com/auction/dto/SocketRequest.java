package com.auction.dto;

import java.util.UUID;

import com.auction.enums.ActionType;
import com.google.gson.JsonObject;

/**
 * DTO giao thức mạng bọc các yêu cầu gửi từ Client sang Server qua kết nối TCP Socket.
 */
public class SocketRequest {
    private String requestId;
    private ActionType action;
    private String body;

    public SocketRequest() {
    }

    public SocketRequest(ActionType action, JsonObject body) {
        this.requestId = UUID.randomUUID().toString();
        this.action = action;
        this.body = body.toString();
    }

    public SocketRequest(ActionType action, String body) {
        this.requestId = UUID.randomUUID().toString();
        this.action = action;
        this.body = body;
    }

    public String getRequestId() {
        return requestId;
    }

    public ActionType getAction() {
        return action;
    }

    public String getBody() {
        return body;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setAction(ActionType action) {
        this.action = action;
    }

    public void setBody(String body) {
        this.body = body;
    }
}