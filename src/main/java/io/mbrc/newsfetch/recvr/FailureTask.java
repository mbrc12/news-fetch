package io.mbrc.newsfetch.recvr;

import com.google.gson.Gson;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.mbrc.newsfetch.util.KeyValuePair;
import io.mbrc.newsfetch.util.NewsType;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class FailureTask implements DisposableBean {

    private final Gson gson;
    private final MongoClient client;
    private final MongoCollection<Document> collection;
    private final ExecutorService pool;
    private final Long startDelay;
    private final Long ceilingDelay;
    private final Integer maxTries;

    FailureTask(
            @Autowired Gson gson,
            @Autowired String mongoURL,
            @Value("${recvr.mongo.db}") String dbName,
            @Value("${recvr.mongo.coll}") String collName,
            @Value("${recvr.failure.threads}") Integer nFailureTaskThreads,
            @Value("${recvr.failure.start}") Long startDelaySeconds,
            @Value("${recvr.failure.ceiling}") Long ceilingDelaySeconds,
            @Value("${recvr.failure.maxtries}") Integer maxTries
    ) {
        this.gson = gson;
        this.client = MongoClients.create(mongoURL);
        MongoDatabase database = client.getDatabase(dbName);
        this.collection = database.getCollection(collName);
        this.pool = Executors.newFixedThreadPool(nFailureTaskThreads);
        this.startDelay = startDelaySeconds;
        this.ceilingDelay = ceilingDelaySeconds;
        this.maxTries = maxTries;
    }

    synchronized public void submit(@NotNull KeyValuePair<String, NewsType> document) {
        document.acceptedBy((hash, news) -> {
            Document mongoDoc = new Document()
                    .append("hash", hash)
                    .append("obj", gson.toJson(news));
            pool.submit(() -> persistDocument(hash, mongoDoc));
        });
    }

    private void persistDocument(String hash, Document document) {
        long currentDelay = this.startDelay;
        int trialsLeft = this.maxTries;

        while (trialsLeft > 0) {
            trialsLeft--;
            try {
                synchronized (collection) {
                    collection.insertOne(document);
                }
                log.info("Persisted Document: " + hash);
            } catch (MongoException e) {
                log.error(String.format("Failed to persist %s. Retrying after some time. Stacktrace: ", hash));
                e.printStackTrace();
            }
            try {
                TimeUnit.SECONDS.sleep(currentDelay);
            } catch (InterruptedException e) {
                log.error("Interrupted. Couldn't persist document with hash: " + hash);
                return;
            }
            currentDelay = Math.min(currentDelay * 2L, this.ceilingDelay); // Exponential backoff with given ceiling.
        }
    }

    @Override
    public void destroy() {
        this.pool.shutdown();
        try {
            this.pool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Interrupted. Some objects might not be persisted yet.");
        }
        this.client.close();
    }
}
