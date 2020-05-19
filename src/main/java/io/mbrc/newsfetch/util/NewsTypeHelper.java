package io.mbrc.newsfetch.util;


import com.google.gson.Gson;
import okio.BufferedSource;

import java.nio.charset.Charset;

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

}
