package com.parkease.parkinglot_service.controller;

import com.parkease.parkinglot_service.dtos.ParkingLotResponseDto;
import com.parkease.parkinglot_service.service.ParkingLotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/parking-lots")
@RequiredArgsConstructor
public class InternalParkingLotController {

    private final ParkingLotService parkingLotService;

    @GetMapping("/{lotId}")
    public ResponseEntity<ParkingLotResponseDto> getLot(
            @PathVariable Long lotId) {

        return ResponseEntity.ok(
                parkingLotService.getLotById(lotId)
        );
    }
}