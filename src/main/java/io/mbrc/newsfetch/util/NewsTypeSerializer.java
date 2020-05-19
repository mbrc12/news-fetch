package io.mbrc.newsfetch.util;

import org.apache.kafka.common.serialization.Serializer;

public class NewsTypeSerializer implements Serializer<NewsTypeProtobuf.NewsType> {

    @Override
    public byte[] serialize(String topic, NewsTypeProtobuf.NewsType news) {
        return news.toByteArray();
    }

}
