package com.bf.middleware;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class YarnAppConfiguration {
	@Bean
	public HelloPojo helloPojo() {
		return new HelloPojo();
	}
}
