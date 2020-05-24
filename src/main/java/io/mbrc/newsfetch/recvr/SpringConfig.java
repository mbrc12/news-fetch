package io.mbrc.newsfetch.recvr;

import org.springframework.context.annotation.*;

@Configuration
@ComponentScan(basePackages = {"io.mbrc.newsfetch.recvr", "io.mbrc.newsfetch.util"})
@PropertySource("application.properties")
public class SpringConfig {

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
