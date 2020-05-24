package io.mbrc.newsfetch.recvr;

import io.mbrc.newsfetch.util.KeyValuePair;
import io.mbrc.newsfetch.util.NewsType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static io.mbrc.newsfetch.util.NewsTypeHelper.trimmedTitleOf;

@Slf4j
@Component
public class Recvr {

    private final KafkaReceiverFactory receiverFactory;
    private final ESClient esClient;
    private final Integer nConsumers;

    private Recvr(
            @Value("${recvr.nConsumers}") Integer nConsumers,
            ESClient esClient,
            KafkaReceiverFactory receiverFactory
    ) {
        this.receiverFactory = receiverFactory;
        this.esClient = esClient;
        this.nConsumers = nConsumers;
    }

    public void execute() {
        ExecutorService pool = Executors.newFixedThreadPool(nConsumers);
        IntStream.range(0, nConsumers).forEach(idx -> {
            pool.submit(() -> {
                log.info("Starting consumer #" + idx);
                receiverFactory.getInstance().initiate(Recvr.this::recordProcessor);
            });
        });
    }

    public void recordProcessor(String hash, NewsType news) {
        log.info(String.format("Rcvd item. Title: %s.", trimmedTitleOf(news)));
        esClient.indexDocument(KeyValuePair.pairOf(hash, news));
    }
}
