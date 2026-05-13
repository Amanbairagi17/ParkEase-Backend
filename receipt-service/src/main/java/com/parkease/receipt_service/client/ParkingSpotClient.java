package com.parkease.receipt_service.client;

import com.parkease.receipt_service.dtos.ParkingSpotResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "parkingspot-service", url = "${parkingspot-service.url:http://localhost:8084}/api/parking-spots")
public interface ParkingSpotClient {

    @GetMapping("/{spotId}")
    ParkingSpotResponseDto getSpot(@PathVariable("spotId") Long spotId);
}
