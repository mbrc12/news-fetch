package io.mbrc.newsfetch.util;

import com.google.gson.Gson;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Map;

public class NewsTypeDeserializer implements Deserializer<NewsType> {

    private Gson gson;
    private StringDeserializer stringDeserializer;

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        this.gson = new Gson();
        this.stringDeserializer = new StringDeserializer();
        stringDeserializer.configure(configs, isKey);
    }

    @Override
    public NewsType deserialize(String topic, byte[] bytes) {
        String json = stringDeserializer.deserialize(topic, bytes);
        return gson.fromJson(json, NewsType.class);
    }

}
