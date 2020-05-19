package io.mbrc.newsfetch.client;

import io.mbrc.newsfetch.util.NewsType;
import io.mbrc.newsfetch.util.NewsTypeHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@Slf4j
@Component
public class Test {

    // This autowiring is not recommended, but it is still here just
    // for testing purposes
    @Autowired
    private NewsTypeHelper newsTypeHelper;

    private final ApiConfig apiConfig;
    private final ApiClient apiClient;

    public Test(ApiConfig apiConfig, ApiClient apiClient) {
        this.apiConfig = apiConfig;
        this.apiClient = apiClient;
    }

    void test() {
        QueryParameters params = QueryParameters.builder().query("*").limit(10).build();
        log.info("gen url: " + apiClient.buildURL(params).toString());

        apiClient.request(params,
                (resp) -> {
                    resp.forEach(news -> news.acceptedBy((hash, data) ->
                            log.info("Title: " + data.title)));
                },
                (code, src) -> {
                    log.info(Integer.toString(code));
                    try {
                        log.info(src.readString(Charset.defaultCharset()));
                        src.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                },
                IOException::printStackTrace);
    }

    void test2() {
        NewsType newsType = new NewsType();
        newsType.setDiscoverDate(Date.from(
                LocalDateTime.now().toInstant(ZoneOffset.ofHoursMinutes(5, 30))));
        log.info(newsTypeHelper.serialize(newsType));
    }
}

