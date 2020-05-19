package io.mbrc.newsfetch.util;

import com.google.gson.Gson;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Map;

public class NewsTypeSerializer implements Serializer<NewsType> {

    private Gson gson;
    private StringSerializer stringSerializer;

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        this.gson = new Gson();
        this.stringSerializer = new StringSerializer();
        stringSerializer.configure(configs, isKey);
    }

    @Override
    public byte[] serialize(String topic, NewsType news) {
        String json = gson.toJson(news);
        return stringSerializer.serialize(topic, json);
    }

}
