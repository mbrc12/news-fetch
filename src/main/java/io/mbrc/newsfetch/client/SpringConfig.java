package io.mbrc.newsfetch.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.mbrc.newsfetch.util.GsonUTCDateAdapter;
import io.mbrc.newsfetch.util.NewsTypeHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

import java.time.Duration;
import java.util.Date;

@Slf4j
@Configuration
@ComponentScan(basePackages = {"io.mbrc.newsfetch.client", "io.mbrc.newsfetch.util"})
@PropertySource("application.properties")
public class SpringConfig {

    @Value("${client.apiToken.env}")
    private String apiTokenEnv;

    @Bean
    @Scope("singleton")
    public String apiToken() {
        log.info("ApiToken = " + System.getenv(apiTokenEnv));
        return System.getenv(apiTokenEnv);
    }

    @Bean
    @Scope("singleton")
    public Gson gson() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                .create();
        return gson;
    }

    @Bean
    @Scope("singleton")
    public NewsTypeHelper newsTypeHelper() {
        return NewsTypeHelper.getInstance(gson());
    }

    // A rate limiter that allows 200 calls per 15 minutes.
    // Each time, Spring will generate a new instance, so prototype scope.
    @Bean
    @Scope("prototype")
    public RateLimiter rateLimiter() {
        return new RateLimiter(200, Duration.ofMinutes(15));
    }

    @Bean
    @Scope("singleton")
    public String keySerializerClass() {
        return org.apache.kafka.common.serialization.StringSerializer.class.getName();
    }

    @Bean
    @Scope("singleton")
    public String valueSerializerClass() {
        return io.mbrc.newsfetch.util.NewsTypeSerializer.class.getName();
    }
}
