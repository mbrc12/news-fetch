package io.mbrc.newsfetch.client;

import io.mbrc.newsfetch.util.KeyValuePair;
import io.mbrc.newsfetch.util.NewsType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static io.mbrc.newsfetch.util.NewsTypeHelper.*;

@Slf4j
@Component
public class Client {

    private final KafkaPusherFactory.KafkaPusher kafkaPusher;
    private final ApiClient apiClient;
    private final RateLimiter rateLimiter;
    private final Integer nThreads;
    private final Long retryMillis;
    private final Integer fetchLimitPerThread;
    private final Integer nKafkaSenderThreads;

    private Client(@NotNull KafkaPusherFactory kafkaPusherFactory,
                   ApiClient apiClient,
                   RateLimiter rateLimiter,
                   @Value("${client.nThreads}") Integer nThreads,
                   @Value("${client.retryMillis}") Long retryMillis,
                   @Value("${client.fetchLimitPerThread}") Integer fetchLimitPerThread,
                   @Value("${client.nKafkaSenderThreads}") Integer nKafkaSenderThreads) {
        this.kafkaPusher = kafkaPusherFactory.getInstance();
        this.apiClient = apiClient;
        this.rateLimiter = rateLimiter;
        this.nThreads = nThreads;
        this.retryMillis = retryMillis;
        this.fetchLimitPerThread = fetchLimitPerThread;
        this.nKafkaSenderThreads = nKafkaSenderThreads;
    }

    public static String queryFormat(Instant instant_1, Instant instant_2) {
        return String.format("discoverDate: [%s TO %s] AND language:en",
                dateFormat(instant_1), dateFormat(instant_2));
    }

    public void execute(@NotNull LocalDate startDate, LocalDate endDate) {

        ExecutorService threadPool = Executors.newFixedThreadPool(this.nThreads);
        List<LocalDate> dateList = startDate.datesUntil(endDate).collect(Collectors.toList());
        CountDownLatch latch = new CountDownLatch(dateList.size());

        dateList.forEach(date -> {
            log.info("Spawning worker for " + date.toString());
            String dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            QueryParameters params = QueryParameters.builder()
                    .limit(1)
                    .sortBy(QueryParameters.SortBy.DISCOVER_DATE)
                    .sortOrder(QueryParameters.SortOrder.ASC)
                    .build();

//            Future<FetchStatistics> result = threadPool.submit(new ClientAgent(params, date));

            ClientAgent agent = new ClientAgent(
                    params,
                    date,
                    this.retryMillis,
                    this.fetchLimitPerThread,
                    this.nKafkaSenderThreads);

            try {
                CompletableFuture<FetchStatistics> result = CompletableFuture.supplyAsync(agent::call, threadPool);
                result.thenAccept(stats -> {
                    log.info(String.format("Date: %s. Stats = %s", dateString, stats.toString()));
                    latch.countDown();
                });
            } catch (Exception e) {
                log.error(String.format("Thread for date %s failed.", dateString));
            }

//            try {
//                FetchStatistics stats = result.get();
//                log.info(String.format("Date: %s. Stats = %s", dateString, stats.toString()));
//            } catch (Exception e) {
//                e.printStackTrace();
//                log.error(String.format("Thread for date %s failed.", dateString));
//            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("Interrupted Client.");
            e.printStackTrace();
        }

        // Shutdown thread pool after all work is done.
        threadPool.shutdown();
    }

    @Data
    static class FetchStatistics {
        public int count;
        public Instant lastFetched;

        public void addOne() {
            this.count++;
        }
    }

    class ClientAgent implements Callable<FetchStatistics> {

        private final QueryParameters params;
        private final FetchStatistics stats;
        private final LocalDate date;
        private final long retryMillis;
        private final int fetchLimit;
        private final int nKafkaSenderThreads;

        ClientAgent(QueryParameters params,
                    LocalDate date,
                    long retryMillis,
                    int fetchLimit,
                    int nKafkaSenderThreads) {
            this.params = params;
            this.stats = new FetchStatistics();
            this.date = date;
            this.retryMillis = retryMillis;
            this.fetchLimit = fetchLimit;
            this.nKafkaSenderThreads = nKafkaSenderThreads;

            stats.setCount(0);
            stats.setLastFetched(null);
        }

        public void sendViaKafka(@NotNull KeyValuePair<String, NewsType> newsItem) {
            String hash = newsItem.getKey();
            NewsType news = newsItem.getValue();

            synchronized (stats) {
                stats.addOne();
                stats.setLastFetched(instantMax(stats.getLastFetched(), news.getDiscoverDate().toInstant()));
            }

            kafkaPusher.sendSync(hash, news,
                    (metadata, e) -> {
                        if (metadata != null) {
                            log.info(String.format("Sent item. Title = %s", trimmedTitleOf(news)));
                        } else {
                            log.info("Error in sending to Kafka.");
                            e.printStackTrace();
                        }
                    });
        }

        public FetchStatistics call() {

            AtomicBoolean failed = new AtomicBoolean(false);

            Instant endOfDay = endOfDay(date);
            Instant startOfDay = startOfDay(date);

            Instant current = startOfDay;

            while (!failed.get() && this.stats.getCount() < this.fetchLimit) {

                // Uncomment this to restrict flow speed to the server.

                /*
                try {
                    // This is not a busy wait. We wait for some time before making
                    // another call to the server.
                    Thread.sleep(this.retryMillis);
                } catch (InterruptedException e) {
                    log.info("Thread interrupted.");
                    return this.stats;
                }
                */

                params.setQuery(queryFormat(current, endOfDay));

                if (rateLimiter.tick()) {

                    // Multiple Kafka threads should send, so we use a ForkJoinPool
                    final ForkJoinPool senderPool = new ForkJoinPool(nKafkaSenderThreads);

                    log.info("Trying to query for range: " + params.getQuery());

                    // Synchronously request because we can't do anything useful
                    // before this returns.

                    apiClient.requestSync(params,
                            newsItems -> {
                                newsItems.forEach(item -> senderPool.execute(() -> sendViaKafka(item)));

                                /*
                                In case we need a synchronous system instead of a ForkJoinPool
                                to send Kafka information (this seems better in the current scenario,
                                because a small amount of work is being done in the sendViaKafka method)
                                uncomment the next line, and remove the try { senderPool.shutdown(); .. }
                                at the end of this loop.

                                newsItems.forEach(this::sendViaKafka);
                                 */

                                if (newsItems.isEmpty()) {
                                    failed.set(true);
                                }
                            },
                            (code, body) -> {
                                try {
                                    log.error(String.format("Rejected Request. Code: %d. Body = %s",
                                            code,
                                            body.readString(Charset.defaultCharset())));
                                } catch (IOException e) {
                                    log.error("Rejected Request. Body could not be parsed.");
                                } finally {
                                    failed.set(true);
                                }
                            },
                            (IOException e) -> {
                                log.error("Failed to get data. Stacktrace: ");
                                e.printStackTrace();
                                failed.set(true);
                            });

                    // Remove this try catch block if we use synchronous sendViaKafka.
                    // See comment above.
                    try {
                        senderPool.shutdown();
                        senderPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        log.error("Thread interrupted. Terminating Thread.");
                        return this.stats;
                    }

                    // Update next interval to fetch from
                    Instant lastFetched = this.stats.getLastFetched();
                    if (lastFetched == null) current = startOfDay;
                    else current = this.stats.getLastFetched().plusSeconds(1);
                }
            }

            return this.stats;
        }
    }
}
