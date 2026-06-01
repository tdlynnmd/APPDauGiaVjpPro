package com.auction.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    public static Gson getGson() {
        return gson;
    }
}
