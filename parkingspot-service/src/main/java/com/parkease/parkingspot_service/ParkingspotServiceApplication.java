package com.parkease.parkingspot_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.parkease.parkingspot_service.client")
public class ParkingspotServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ParkingspotServiceApplication.class, args);
	}

}
