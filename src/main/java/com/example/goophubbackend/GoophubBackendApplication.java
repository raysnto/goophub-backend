package com.example.goophubbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
@ImportResource("classpath:applicationContext.xml")
public class GoophubBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(GoophubBackendApplication.class, args);
	}

}