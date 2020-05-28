package io.mbrc.newsfetch.recvr;

import com.google.gson.Gson;
import io.mbrc.newsfetch.util.KeyValuePair;
import io.mbrc.newsfetch.util.NewsType;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static io.mbrc.newsfetch.util.NewsTypeHelper.trimmedTitleOf;

@Slf4j
@Component
public class ESClient implements DisposableBean {
    private final RestHighLevelClient client;
    private final String index;
    private final Gson gson;
    private final FailureTask failureTask;
    private final AtomicInteger id;

    private ESClient(
            @Value("${recvr.esRequestScheme}") String requestScheme,
            @Value("${recvr.esRequestHost}") String requestHost,
            @Value("${recvr.esRequestPort}") Integer requestPort,
            @Value("${recvr.esIndex}") String esIndex,
            @Autowired FailureTask failureTask,
            @Autowired String mappingJson,
            @Autowired Gson gson
    ) throws IOException {
        this.id = new AtomicInteger(1);
        this.failureTask = failureTask;
        this.gson = gson;
        this.client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(requestHost, requestPort, requestScheme)));
        this.index = esIndex;
        GetIndexRequest request = new GetIndexRequest(this.index);
        boolean exists = client.indices().exists(request, RequestOptions.DEFAULT);
        if (!exists) {
            CreateIndexRequest createIndexRequest = new CreateIndexRequest(this.index);
            try {
                CreateIndexResponse response =
                        client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
                log.info("Created index " + this.index);

                // Now add the mapping

                log.info("Mapping: " + mappingJson);

                PutMappingRequest mappingRequest = new PutMappingRequest(esIndex);
                mappingRequest.source(mappingJson, XContentType.JSON);

                AcknowledgedResponse mappingResponse =
                        client.indices().putMapping(mappingRequest, RequestOptions.DEFAULT);

                boolean acknowledged = mappingResponse.isAcknowledged();
                if (!acknowledged) throw new ElasticsearchException("Mapping request not acknowledged.");

            } catch (ElasticsearchException e) {
                log.error("Failed to create index " + this.index);
                e.printStackTrace();
            } catch (IOException e) {
                log.error("Some IOException occurred. Stacktrace:");
                e.printStackTrace();
            }
        } else {
            log.info("Index " + this.index + " already exists.");
        }
    }

    public void indexDocument(@NotNull KeyValuePair<String, NewsType> document) {
        document.acceptedBy((hash, news) -> {
            GetRequest getRequest = new GetRequest(this.index, hash);
            getRequest.fetchSourceContext(new FetchSourceContext(false));
            getRequest.storedFields("_none_");

            try {
                boolean exists = client.exists(getRequest, RequestOptions.DEFAULT);
                if (exists) {
                    log.info("Document exists already. Not indexing.");
                    return;
                }
            } catch (IOException e) {
                persist(document); // Persist document because we couldn't process it.
                log.error("Couldn't verify existence of document.");
                return;
            }

            IndexRequest request = new IndexRequest(this.index);
            request.id(this.getNextID());
            request.source(gson.toJson(news), XContentType.JSON);

            try {
                IndexResponse response = client.index(request, RequestOptions.DEFAULT);
                log.info(String.format("Indexed document. Title: %s", trimmedTitleOf(news)));
            } catch (IOException e) {
                persist(document); // Persist document
                log.error(String.format("Failed to index document. Hash: %s", hash));
            }
        });
    }

    // Persist the current document
    private void persist(KeyValuePair<String, NewsType> document) {
        failureTask.submit(document);
    }

    @NotNull
    private String getNextID() {
        return Integer.toString(this.id.getAndIncrement());
    }

    @Override
    public void destroy() {
        try {
            this.client.close();
            log.info("Closed ES client");
        } catch (Exception e) {
            log.error("Failed to close ES client.");
        }
    }
}
