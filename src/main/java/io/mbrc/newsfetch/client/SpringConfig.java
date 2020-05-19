package io.mbrc.newsfetch.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.mbrc.newsfetch.util.GsonUTCDateAdapter;
import io.mbrc.newsfetch.util.NewsTypeHelper;
import org.springframework.context.annotation.*;

import java.util.Date;

@Configuration
@ComponentScan(basePackages = "io.mbrc.newsfetch")
@PropertySource("application.properties")
public class SpringConfig {

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
