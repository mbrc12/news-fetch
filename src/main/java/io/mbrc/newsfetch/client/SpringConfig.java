package io.mbrc.newsfetch.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.context.annotation.*;

import java.util.Date;

@Configuration
@ComponentScan
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

}
