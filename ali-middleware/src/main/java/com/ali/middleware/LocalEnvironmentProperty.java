package com.ali.middleware;

import org.bf.framework.boot.spi.EnvironmentPropertyPostProcessor;

import java.util.Map;

public class LocalEnvironmentProperty implements EnvironmentPropertyPostProcessor {
    @Override
    public Map<String, Object> simpleProperties() {
        return null;
    }
    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public String classPathConfigFileKey(){
        return "middleware";
    }
}
