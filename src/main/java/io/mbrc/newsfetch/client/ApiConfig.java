package io.mbrc.newsfetch.client;

import lombok.Getter;
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
           @Value("${newsfetch.apiToken}") String apiToken,
           @Value("${newsfetch.scheme}") String scheme,
           @Value("${newsfetch.host}") String host,
           @Value("${newsfetch.path}") String path) {
      this.apiToken = apiToken;
      this.scheme = scheme;
      this.host = host;
      this.path = path;
   }
}
