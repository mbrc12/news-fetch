package io.mbrc.newsfetch.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Client {

    private final KafkaPusherFactory kafkaPusherFactory;
    private final ApiClient apiClient;

    private Client(KafkaPusherFactory kafkaPusherFactory,
                   ApiClient apiClient) {
        this.kafkaPusherFactory = kafkaPusherFactory;
        this.apiClient = apiClient;
    }

    public void execute() {

    }

}
