package io.mbrc.newsfetch.recvr;

import io.mbrc.newsfetch.util.KeyValuePair;
import io.mbrc.newsfetch.util.NewsType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static io.mbrc.newsfetch.util.NewsTypeHelper.trimmedTitleOf;

@Slf4j
@Component
public class Recvr implements DisposableBean {

    private final KafkaReceiverFactory receiverFactory;
    private final ESClient esClient;
    private final Integer nConsumers;
    private final ExecutorService pool;

    private Recvr(
            @Value("${recvr.nConsumers}") Integer nConsumers,
            ESClient esClient,
            KafkaReceiverFactory receiverFactory
    ) {
        this.receiverFactory = receiverFactory;
        this.esClient = esClient;
        this.nConsumers = nConsumers;
        this.pool = Executors.newFixedThreadPool(nConsumers);
    }

    public void execute() {
        IntStream.range(0, nConsumers).forEach(idx -> pool.submit(() -> {
            log.info("Starting consumer #" + idx);
            receiverFactory.getInstance().initiate(Recvr.this::recordProcessor);
        }));
    }

    public void recordProcessor(String hash, NewsType news) {
        log.info(String.format("Rcvd item. Title: %s.", trimmedTitleOf(news)));
        esClient.indexDocument(KeyValuePair.pairOf(hash, news));
    }

    @Override
    public void destroy() {
        pool.shutdown();
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
            log.info("Consumer shut down.");
        } catch (InterruptedException e) {
            log.error("Interrupted execution. Stopping.");
        }
    }
}
