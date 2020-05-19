package io.mbrc.newsfetch.client;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


public class Main {

    public static void main(String[] args) {
        ConfigurableApplicationContext context
                = new AnnotationConfigApplicationContext(SpringConfig.class);
        Client client = context.getBean("client", Client.class);
        client.execute();
        context.close();
    }
}
