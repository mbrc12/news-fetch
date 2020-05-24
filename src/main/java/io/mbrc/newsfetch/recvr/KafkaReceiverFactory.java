package io.mbrc.newsfetch.recvr;


import io.mbrc.newsfetch.util.NewsType;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.function.BiConsumer;

@Slf4j
@Component
public class KafkaReceiverFactory {

    private final String topic;
    private final Properties properties;
    private final Duration pollTimeout;

    private KafkaReceiverFactory(
            @Value("${kafka.brokerURL}") String brokerURL,
            @Value("${kafka.topic}") String topic,
            @Value("${kafka.recvr.groupID}") String groupID,
            @Value("${kafka.recvr.pollTimeout}") Integer pollTimeout,
            String keyDeserializerClass,
            String valueDeserializerClass
    ) {
        this.topic = topic;
        this.properties = new Properties();
        this.pollTimeout = Duration.ofSeconds(pollTimeout);
        properties.setProperty("bootstrap.servers", brokerURL);
        properties.setProperty("group.id", groupID);
        properties.setProperty("key.deserializer", keyDeserializerClass);
        properties.setProperty("value.deserializer", valueDeserializerClass);
    }

    public KafkaReceiver getInstance() {
        return new KafkaReceiver();
    }

    public class KafkaReceiver {

        private final org.apache.kafka.clients.consumer.Consumer<String, NewsType> consumer;

        private KafkaReceiver() {
            this.consumer = new KafkaConsumer<>(properties);
            this.consumer.subscribe(List.of(topic));
            log.info("Subscribed to topic: " + topic);
        }

        public void initiate(BiConsumer<String, NewsType> onEach) {
            try {
                while (true) {
                    ConsumerRecords<String, NewsType> records = consumer.poll(pollTimeout);
                    records.forEach(record -> onEach.accept(record.key(), record.value()));
                }
            } finally {
                consumer.close();
            }
        }
    }
}
