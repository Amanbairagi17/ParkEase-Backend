package com.parkease.parkingspot_service.controller;

import com.parkease.parkingspot_service.dtos.ParkingSpotBulkRequestDto;
import com.parkease.parkingspot_service.dtos.ParkingSpotRequestDto;
import com.parkease.parkingspot_service.dtos.ParkingSpotResponseDto;
import com.parkease.parkingspot_service.entity.SpotType;
import com.parkease.parkingspot_service.entity.VehicleType;
import com.parkease.parkingspot_service.service.ParkingSpotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(path = "/api/parking-spots")
@RequiredArgsConstructor
@Slf4j
public class ParkingSpotController {

    private final ParkingSpotService parkingSpotService;

    //MANAGER / ADMIN
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @PostMapping
    public ResponseEntity<ParkingSpotResponseDto> addSpot(@Valid @RequestBody ParkingSpotRequestDto requestDto) {

        log.info("Add spot request received | lotId={} | spotNumber={}", requestDto.getLotId(), requestDto.getSpotNumber());

        ParkingSpotResponseDto response = parkingSpotService.addSpot(requestDto);

        log.info("Spot created successfully | spotId={}", response.getSpotId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //MANAGER / ADMIN
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @PostMapping("/bulk")
    public ResponseEntity<List<ParkingSpotResponseDto>> addBulkSpots(@Valid @RequestBody ParkingSpotBulkRequestDto requestDto) {

        log.info("Bulk spot creation request | totalSpots={}", requestDto.getSpots().size());

        List<ParkingSpotResponseDto> response = parkingSpotService.addBulkSpots(requestDto);

        log.info("Bulk spots created successfully | count={}", response.size());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // AUTHENTICATED USERS (Driver/Manager/Admin)
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{spotId}")
    public ResponseEntity<ParkingSpotResponseDto> getSpotById(@PathVariable Long spotId) {

        log.info("Fetching spot by id | spotId={}", spotId);

        ParkingSpotResponseDto response = parkingSpotService.getSpotById(spotId);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/lot/{lotId}")
    public ResponseEntity<List<ParkingSpotResponseDto>> getSpotsByLot(@PathVariable Long lotId) {

        log.info("Fetching spots for lot | lotId={}", lotId);

        return ResponseEntity.ok(parkingSpotService.getSpotsByLot(lotId));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/lot/{lotId}/available")
    public ResponseEntity<List<ParkingSpotResponseDto>> getAvailableSpots(@PathVariable Long lotId) {

        log.info("Fetching available spots | lotId={}", lotId);

        return ResponseEntity.ok(parkingSpotService.getAvailableSpots(lotId));
    }

    // DRIVER / ADMIN
    @PreAuthorize("hasAnyRole('DRIVER','ADMIN')")
    @PatchMapping("/{spotId}/reserve")
    public ResponseEntity<ParkingSpotResponseDto> reserveSpot(@PathVariable Long spotId) {

        log.info("Reserve spot request | spotId={}", spotId);

        return ResponseEntity.ok(parkingSpotService.reserveSpot(spotId));
    }

    // DRIVER / ADMIN
    @PreAuthorize("hasAnyRole('DRIVER','ADMIN')")
    @PatchMapping("/{spotId}/occupy")
    public ResponseEntity<ParkingSpotResponseDto> occupySpot(@PathVariable Long spotId) {

        log.info("Occupy spot request | spotId={}", spotId);

        return ResponseEntity.ok(parkingSpotService.occupySpot(spotId));
    }

    // DRIVER / ADMIN
    @PreAuthorize("hasAnyRole('DRIVER','ADMIN')")
    @PatchMapping("/{spotId}/release")
    public ResponseEntity<ParkingSpotResponseDto> releaseSpot(@PathVariable Long spotId) {

        log.info("Release spot request | spotId={}", spotId);

        return ResponseEntity.ok(parkingSpotService.releaseSpot(spotId));
    }

    // OWNER OR ADMIN
    @PreAuthorize("hasRole('ADMIN') or @parkingSpotSecurity.isOwner(#spotId)")
    @PutMapping("/{spotId}")
    public ResponseEntity<ParkingSpotResponseDto> updateSpot(@PathVariable Long spotId,
                                                             @Valid @RequestBody ParkingSpotRequestDto requestDto) {

        log.info("Update spot request | spotId={}", spotId);

        return ResponseEntity.ok(parkingSpotService.updateSpot(spotId, requestDto));
    }

    // OWNER OR ADMIN
    @PreAuthorize("hasRole('ADMIN') or @parkingSpotSecurity.isOwner(#spotId)")
    @DeleteMapping("/{spotId}")
    public ResponseEntity<Void> deleteSpot(@PathVariable Long spotId) {

        log.info("Delete spot request | spotId={}", spotId);

        parkingSpotService.deleteSpot(spotId);

        log.info("Spot deleted successfully | spotId={}", spotId);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/lot/{lotId}/count-available")
    public ResponseEntity<Long> countAvailable(@PathVariable Long lotId) {

        log.info("Counting available spots | lotId={}", lotId);

        return ResponseEntity.ok(parkingSpotService.countAvailable(lotId));
    }
}
