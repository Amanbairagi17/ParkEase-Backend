package com.parkease.booking_service.client;

import com.parkease.booking_service.dtos.ParkingSpotLookupResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "PARKINGSPOT-SERVICE", path = "/api/parking-spots")
public interface ParkingSpotServiceClient {

    @GetMapping("/{spotId}")
    ParkingSpotLookupResponseDto getSpotById(@PathVariable("spotId") Long spotId);

    @PatchMapping("/{spotId}/reserve")
    ParkingSpotLookupResponseDto reserveSpot(@PathVariable("spotId") Long spotId);

    @PatchMapping("/{spotId}/occupy")
    ParkingSpotLookupResponseDto occupySpot(@PathVariable("spotId") Long spotId);

    @PatchMapping("/{spotId}/release")
    ParkingSpotLookupResponseDto releaseSpot(@PathVariable("spotId") Long spotId);
}