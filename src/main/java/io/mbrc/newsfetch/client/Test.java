package io.mbrc.newsfetch.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;

@Slf4j
@Component
public class Test {

    private final ApiConfig apiConfig;
    private final ApiClient apiClient;

    public Test(ApiConfig apiConfig, ApiClient apiClient) {
        this.apiConfig = apiConfig;
        this.apiClient = apiClient;
    }

    void test() {
        QueryParameters params = QueryParameters.builder().query("*").limit(100).build();
        log.info("gen url: " + apiClient.buildURL(params).toString());

        apiClient.request(params,
                (resp) -> {
                    resp.forEach((x) -> {
                        log.info(x.metadata.readTime.toString());
                    });
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
}

