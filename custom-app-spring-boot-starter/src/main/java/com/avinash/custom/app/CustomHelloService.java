package com.avinash.custom.app;

import com.avinash.service.HelloService;

public class CustomHelloService implements HelloService {

	@Override
	public void sayHello() {
		System.out.println("===> Hello from Custom hello servie ");
		
	}

}
