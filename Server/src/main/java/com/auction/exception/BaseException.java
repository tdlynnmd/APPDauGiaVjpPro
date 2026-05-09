package com.auction.exception;

//Đây là lớp cơ sở cho tất cả các ngoại lệ tùy chỉnh trong ứng dụng của bạn.
//Nó mở rộng lớp Exception của Java
//Và thêm các thuộc tính bổ sung như errorCode và timestamp để cung cấp thông tin chi tiết về lỗi.
public abstract class BaseException extends Exception {
    private final String errorCode;
    private final long timestamp;

    public BaseException(String message, String errorCode) {
        super(message); // Gửi message lên lớp Exception của Java
        this.errorCode = errorCode;
        this.timestamp = System.currentTimeMillis();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
