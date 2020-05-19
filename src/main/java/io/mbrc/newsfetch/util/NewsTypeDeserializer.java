package io.mbrc.newsfetch.util;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import org.apache.kafka.common.serialization.Deserializer;

public class NewsTypeDeserializer implements Deserializer<NewsTypeProtobuf.NewsType> {

    @Override
    public NewsTypeProtobuf.NewsType deserialize(String topic, byte[] bytes) {
        Message.Builder builder = NewsTypeProtobuf.NewsType.newBuilder();

        try {
            builder.mergeFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            return null;
        }

        return (NewsTypeProtobuf.NewsType) builder.build();
    }

}
