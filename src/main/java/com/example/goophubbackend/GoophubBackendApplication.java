package com.example.goophubbackend;

import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

import com.example.goophubbackend.controller.UploadController;

@SpringBootApplication
@ImportResource("classpath:applicationContext.xml")
public class GoophubBackendApplication {

	public static void main(String[] args) {
		new File(UploadController.uploadDirectory).mkdir();
		SpringApplication.run(GoophubBackendApplication.class, args);
	}

}