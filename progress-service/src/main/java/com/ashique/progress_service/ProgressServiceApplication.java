package com.ashique.progress_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
		"com.ashique.progress_service",
		"com.ashique.learnsphere.jwt"
})
public class ProgressServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProgressServiceApplication.class, args);
	}

}
