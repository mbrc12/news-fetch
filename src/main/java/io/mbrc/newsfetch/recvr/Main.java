package io.mbrc.newsfetch.recvr;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {
    public static void main(String[] args) {
        ConfigurableApplicationContext context =
                new AnnotationConfigApplicationContext(SpringConfig.class);
        Recvr recvr = context.getBean("recvr", Recvr.class);
        recvr.execute();
        context.close();
    }
}
