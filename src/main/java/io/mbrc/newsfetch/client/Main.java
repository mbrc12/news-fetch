package io.mbrc.newsfetch.client;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


public class Main {

    public static void main(String[] args) {
        ConfigurableApplicationContext context
                = new AnnotationConfigApplicationContext(SpringConfig.class);
        Test test = context.getBean("test", Test.class);
        test.test();
        context.close();
    }
}
