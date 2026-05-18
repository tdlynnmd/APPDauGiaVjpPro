package com.auction.dto;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

/**
 * SocketResponse là phong bì chung cho mọi message Server gửi về Client.
 *
 * Có 2 loại:
 * - RESPONSE: phản hồi trực tiếp cho một SocketRequest.
 * - EVENT: message realtime do Server tự đẩy, ví dụ BID_UPDATED, AUCTION_ENDED.
 *
 * body là dữ liệu thật sự của từng action.
 * Ví dụ:
 * - LOGIN thành công: body là LoginResultDTO hoặc sau này là LoginResultDTO.
 * - GET_AUCTION_DETAIL: body là AuctionDetailDTO.
 * - LOGOUT: body có thể null.
 */
public class SocketResponse {
    public static final String TYPE_RESPONSE = "RESPONSE";
    public static final String TYPE_EVENT = "EVENT";

    private String requestId;
    private String type;
    private String action;
    private boolean success;
    private String message;
    private String errorCode;
    private JsonElement body;

    private static final Gson gson = new Gson();

    /**
     * Constructor rỗng cần cho Gson.
     */
    public SocketResponse() {
    }

    public SocketResponse(String requestId, String type, String action,
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

    /**
     * Tạo response thành công cho một request cụ thể.
     */
    public static SocketResponse success(String requestId, String action, String message, Object body) {
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

    /**
     * Tạo response thất bại cho một request cụ thể.
     */
    public static SocketResponse failure(String requestId, String action, String message, String errorCode) {
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

    /**
     * Tạo realtime event do Server tự gửi.
     * Event không thuộc request cụ thể nào nên requestId = null.
     */
    public static SocketResponse event(String action, String message, Object body) {
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
        return action;
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
