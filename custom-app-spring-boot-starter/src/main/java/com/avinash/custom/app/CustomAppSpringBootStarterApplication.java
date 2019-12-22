package com.avinash.custom.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.avinash.service.HelloService;

@SpringBootApplication
public class CustomAppSpringBootStarterApplication implements CommandLineRunner {

	@Autowired
	private HelloService helloService;
	
	
	public static void main(String[] args) {
		SpringApplication.run(CustomAppSpringBootStarterApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		// TODO Auto-generated method stub
		helloService.sayHello();
	}
	
	@Bean
	public HelloService helloService() {
		return new CustomHelloService();
	}
	

}
