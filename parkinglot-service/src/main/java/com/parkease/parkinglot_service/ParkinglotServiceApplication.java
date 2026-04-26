package com.parkease.parkinglot_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.parkease.parkinglot_service.client")
public class ParkinglotServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ParkinglotServiceApplication.class, args);
	}

}
