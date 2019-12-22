package com.avinash.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.avinash.service.HelloService;
import com.avinash.service.HelloServiceImpl;

@Configuration
@ConditionalOnClass(HelloService.class)
public class HelloServiceAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public HelloService helloService() {
		return new HelloServiceImpl();
	}
}
