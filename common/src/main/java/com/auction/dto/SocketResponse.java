package com.auction.dto;

import com.auction.enums.ActionType;
import com.auction.utils.GsonProvider;
import com.google.gson.JsonElement;

/**
 * DTO giao thức mạng bọc dữ liệu phản hồi (Response) hoặc sự kiện (Event) truyền từ Server về Client.
 */
public class SocketResponse {
    public static final String TYPE_RESPONSE = "RESPONSE";
    public static final String TYPE_EVENT = "EVENT";

    private String requestId;
    private String type;
    private ActionType action;
    private boolean success;
    private String message;
    private String errorCode;
    private JsonElement body;

    private static final com.google.gson.Gson gson = GsonProvider.getGson();

    public SocketResponse() {
    }

    public SocketResponse(String requestId, String type, ActionType action,
                          boolean success, String message, String errorCode,
                          JsonElement body) {
        this.requestId = requestId;
        this.type = type;
        this.action = action;
        this.success = success;
        this.message = message;
        this.errorCode = errorCode;
        this.body = body;
    }

    public static SocketResponse success(String requestId, ActionType action, String message, Object body) {
        return new SocketResponse(
                requestId,
                TYPE_RESPONSE,
                action,
                true,
                message,
                null,
                body == null ? null : gson.toJsonTree(body)
        );
    }

    public static SocketResponse failure(String requestId, ActionType action, String message, String errorCode) {
        return new SocketResponse(
                requestId,
                TYPE_RESPONSE,
                action,
                false,
                message,
                errorCode,
                null
        );
    }

    public static SocketResponse event(ActionType action, String message, Object body) {
        return new SocketResponse(
                null,
                TYPE_EVENT,
                action,
                true,
                message,
                null,
                body == null ? null : gson.toJsonTree(body)
        );
    }

    public String getRequestId() {
        return requestId;
    }

    public String getType() {
        return type;
    }

    public String getAction() {
        return action.toString();
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public JsonElement getBody() {
        return body;
    }
}
