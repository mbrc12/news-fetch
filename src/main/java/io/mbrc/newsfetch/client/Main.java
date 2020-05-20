package io.mbrc.newsfetch.client;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.LocalDate;


public class Main {

    public static void main(String[] args) {
        ConfigurableApplicationContext context
                = new AnnotationConfigApplicationContext(SpringConfig.class);
        Client client = context.getBean("client", Client.class);
        // Get data for today and yesterday
        client.execute(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
//        context.getBean("test", Test.class).test();
        context.close();
    }
}
