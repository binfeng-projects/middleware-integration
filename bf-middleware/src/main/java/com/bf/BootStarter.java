package com.bf;

import org.bf.framework.common.util.CollectionUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
//@EnableDubbo
public class BootStarter {
    /**
     * 用List包装下，方便传参
     */
    public static ConfigurableApplicationContext run(Class<?> c, List<String> args) {
        if(args == null) {
            args = new ArrayList<>();
        }
        return run(c, CollectionUtils.toArray(args,String.class));
    }

    public static ConfigurableApplicationContext run(Class<?> c, String... args) {
        if (c == null) {
            c = BootStarter.class;
        }
        return SpringApplication.run(c, args);
    }
//    @Configuration
//    @ImportResource(locations = { "classpath:org/springframework/batch/samples/misc/groovy/job/groovy-mytest.xml"})
//    static class AppTestConfiguration {
//
//    }

}
