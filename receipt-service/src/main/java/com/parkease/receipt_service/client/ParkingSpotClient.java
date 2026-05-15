package com.parkease.receipt_service.client;

import com.parkease.receipt_service.dtos.ParkingSpotResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "PARKINGSPOT-SERVICE", path = "/api/internal/parking-spots")
public interface ParkingSpotClient {

    @GetMapping("/{spotId}")
    ParkingSpotResponseDto getSpot(
            @PathVariable("spotId") Long spotId
    );
}