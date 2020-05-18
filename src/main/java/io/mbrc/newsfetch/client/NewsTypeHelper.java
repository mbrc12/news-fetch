package io.mbrc.newsfetch.client;


import com.google.gson.Gson;
import okio.BufferedSource;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;

@Component
public class NewsTypeHelper {

    public static Gson gson;

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
