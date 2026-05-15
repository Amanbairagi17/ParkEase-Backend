package com.parkease.parkingspot_service.controller;

import com.parkease.parkingspot_service.dtos.ParkingSpotResponseDto;
import com.parkease.parkingspot_service.service.ParkingSpotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/parking-spots")
@RequiredArgsConstructor
public class InternalParkingSpotController {

    private final ParkingSpotService parkingSpotService;

    @GetMapping("/{spotId}")
    public ResponseEntity<ParkingSpotResponseDto> getSpot(
            @PathVariable Long spotId) {

        return ResponseEntity.ok(
                parkingSpotService.getSpotById(spotId)
        );
    }
}