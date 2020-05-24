package io.mbrc.newsfetch.recvr;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.mbrc.newsfetch.util.GsonUTCDateAdapter;
import org.springframework.context.annotation.*;

import java.util.Date;

@Configuration
@ComponentScan(basePackages = {"io.mbrc.newsfetch.recvr", "io.mbrc.newsfetch.util"})
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
    public String keyDeserializerClass() {
        return org.apache.kafka.common.serialization.StringDeserializer.class.getName();
    }

    @Bean
    @Scope("singleton")
    public String valueDeserializerClass() {
        return io.mbrc.newsfetch.util.NewsTypeDeserializer.class.getName();
    }
}
