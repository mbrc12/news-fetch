package io.mbrc.newsfetch.client;

import io.mbrc.newsfetch.util.NewsType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.function.BiConsumer;

@Slf4j
@Component
@Getter
public class KafkaPusherFactory {

    private final String brokerURL;
    private final String topic;
    private final String keySerializerClass;
    private final String valueSerializerClass;

    private KafkaPusherFactory(
            @Value("${kafka.brokerURL}") String brokerURL,
            @Value("${kafka.topic}") String topic,
            String keySerializerClass,
            String valueSerializerClass
    ) {
        this.brokerURL = brokerURL;
        this.topic = topic;
        log.info("KeySerializer = " + keySerializerClass);
        this.keySerializerClass = keySerializerClass;
        this.valueSerializerClass = valueSerializerClass;
    }

    public KafkaPusher getInstance() {
        return new KafkaPusher();
    }

    public class KafkaPusher {

        private final Producer<String, NewsType> producer;

        private KafkaPusher() {
            Properties props = new Properties();
            props.put("bootstrap.servers", brokerURL);
            props.put("key.serializer", keySerializerClass);
            props.put("value.serializer", valueSerializerClass);

            this.producer = new KafkaProducer<>(props);
        }

        public void send(String key, NewsType news,
                         @NotNull BiConsumer<RecordMetadata, Exception> consumer) {

            ProducerRecord<String, NewsType> record =
                    new ProducerRecord<>(topic, key, news);

            this.producer.send(record, consumer::accept);
        }

        public void sendSync(String key, NewsType news,
                             @NotNull BiConsumer<RecordMetadata, Exception> consumer) {
            ProducerRecord<String, NewsType> record =
                    new ProducerRecord<>(topic, key, news);
            try {
                RecordMetadata metadata = producer.send(record).get();
                consumer.accept(metadata, null);
            } catch (Exception e) {
                consumer.accept(null, e);
            }
        }
    }
}
