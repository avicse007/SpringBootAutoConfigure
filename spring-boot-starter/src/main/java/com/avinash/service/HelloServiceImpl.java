package com.avinash.service;

import org.springframework.stereotype.Service;

@Service
public class HelloServiceImpl implements HelloService{

	@Override
	public void sayHello() {
		System.out.println("Hello from the default service");
		
	}

}
