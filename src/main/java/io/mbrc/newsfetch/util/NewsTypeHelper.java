package io.mbrc.newsfetch.util;


import com.google.gson.Gson;
import okio.BufferedSource;

import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class NewsTypeHelper {

    private static NewsTypeHelper instance;

    public static NewsTypeHelper getInstance(Gson gson) {
        if (instance == null) {
            synchronized (NewsTypeHelper.class) {
                if (instance == null)
                    instance = new NewsTypeHelper(gson);
            }
        }
        return instance;
    }

    public Gson gson;

    private NewsTypeHelper(Gson gson) {
        this.gson = gson;
    }

    public NewsType deserialize(BufferedSource source) throws Exception {
        return gson.fromJson(
                source.readString(Charset.defaultCharset()),
                NewsType.class);
    }

    public String serialize(NewsType newsType) {
        return gson.toJson(newsType);
    }

    public static Instant instantMax(Instant instant_1, Instant instant_2) {
        if (instant_1 == null) return instant_2;
        if (instant_2 == null) return instant_1;
        if (instant_1.compareTo(instant_2) < 0) return instant_2;
        return instant_1;
    }

    public static Instant endOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant();
    }

    public static Instant startOfDay(LocalDate date) {
        return date.atTime(LocalTime.MIN).atZone(ZoneId.systemDefault()).toInstant();
    }

    public static String dateFormat(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    public static String trimmedTitleOf(NewsType news) {
        String title = news.getTitle();
        if (title.length() <= 20) return title;
        return title.substring(0, 16) + " ...";
    }
}
