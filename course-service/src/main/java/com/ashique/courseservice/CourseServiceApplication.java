package com.ashique.courseservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.ashique.courseservice", "com.ashique.learnsphere.jwt"})
public class CourseServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CourseServiceApplication.class, args);
	}

}
