package io.mbrc.newsfetch.recvr;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.mbrc.newsfetch.util.GsonUTCDateAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.stream.Collectors;

@Configuration
@ComponentScan(basePackages = {"io.mbrc.newsfetch.recvr", "io.mbrc.newsfetch.util"})
@PropertySource("application.properties")
public class SpringConfig {

    @Value("classpath:es/mapping.json")
    Resource mappingResource;

    @Bean
    @Scope("singleton")
    public String mappingJson() throws IOException {
        InputStream stream = mappingResource.getInputStream();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String mapping = reader.lines().collect(Collectors.joining("\n"));
            return mapping;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
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
    public String keyDeserializerClass() {
        return org.apache.kafka.common.serialization.StringDeserializer.class.getName();
    }

    @Bean
    @Scope("singleton")
    public String valueDeserializerClass() {
        return io.mbrc.newsfetch.util.NewsTypeDeserializer.class.getName();
    }
}
