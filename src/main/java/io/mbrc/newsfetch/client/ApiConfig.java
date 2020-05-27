package io.mbrc.newsfetch.client;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class ApiConfig {
   private final String apiToken;
   private final String scheme;
   private final String host;
   private final String path;

   public ApiConfig(
           @Autowired String apiToken,
           @Value("${client.scheme}") String scheme,
           @Value("${client.host}") String host,
           @Value("${client.path}") String path
   ) {
      this.apiToken = apiToken;
      this.scheme = scheme;
      this.host = host;
      this.path = path;
   }
}
