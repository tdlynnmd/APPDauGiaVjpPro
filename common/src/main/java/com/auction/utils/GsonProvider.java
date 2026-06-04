package com.auction.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lớp tiện ích cung cấp thực thể Gson dùng chung trong toàn bộ hệ thống.
 * Cấu hình sẵn Adapter chuyển đổi LocalDateTime sang định dạng ISO_LOCAL_DATE_TIME.
 */
public class GsonProvider {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> {
                String val = json.getAsString();
                try {
                    return LocalDateTime.parse(val, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (Exception e) {
                    try {
                        DateTimeFormatter backupFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        return LocalDateTime.parse(val, backupFormatter);
                    } catch (Exception ex) {
                        return LocalDateTime.parse(val.trim().replace(" ", "T"));
                    }
                }
            })
            .create();

    /**
     * Lấy thực thể Gson dùng chung đã cấu hình LocalDateTime adapter.
     * @return thực thể Gson
     */
    public static Gson getGson() {
        return gson;
    }
}
