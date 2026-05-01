package com.parkease.parkinglot_service.controller;

import com.parkease.parkinglot_service.dtos.ParkingLotRequestDto;
import com.parkease.parkinglot_service.dtos.ParkingLotResponseDto;
import com.parkease.parkinglot_service.service.ParkingLotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(path = "/api/parking-lots")
@RequiredArgsConstructor
@Slf4j
public class ParkingLotController {

    private final ParkingLotService parkingLotService;

    // Create lot (Manager only)
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @PostMapping
    public ResponseEntity<ParkingLotResponseDto> createLot(@Valid @RequestBody ParkingLotRequestDto requestDto) {
        log.info("Creating parking lot. managerId={}, city={}", requestDto.getManagerId(), requestDto.getCity());

        ParkingLotResponseDto response = parkingLotService.createLot(requestDto);

        log.info("Parking lot created successfully. lotId={}", response.getLotId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Public / Authenticated
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{lotId}")
    public ResponseEntity<ParkingLotResponseDto> getLotById(@PathVariable Long lotId) {
        log.info("Fetching parking lot by id. lotId={}", lotId);

        ParkingLotResponseDto response = parkingLotService.getLotById(lotId);

        log.info("Returning parking lot details. lotId={}", lotId);
        return ResponseEntity.ok(response);
    }

    //  Public browsing
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/city/{city}")
    public ResponseEntity<List<ParkingLotResponseDto>> getLotsByCity(@PathVariable String city) {
        log.info("Fetching parking lots by city. city={}", city);

        List<ParkingLotResponseDto> response = parkingLotService.getLotsByCity(city);

        log.info("Found {} parking lots in city={}", response.size(), city);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/nearby")
    public ResponseEntity<List<ParkingLotResponseDto>> getNearbyLots(@RequestParam double latitude,
                                                                     @RequestParam double longitude,
                                                                     @RequestParam(defaultValue = "5") double radiusKm) {

        log.info("Fetching nearby lots. lat={}, long={}, radius={}", latitude, longitude, radiusKm);

        List<ParkingLotResponseDto> response = parkingLotService.getNearbyLots(latitude, longitude, radiusKm);

        log.info("Nearby lots found: {}", response.size());
        return ResponseEntity.ok(response);
    }

    //  Manager owns data OR Admin
    @PreAuthorize("hasRole('ADMIN') or #managerId == authentication.principal")
    @GetMapping("/manager/{managerId}")
    public ResponseEntity<List<ParkingLotResponseDto>> getLotsByManager(@PathVariable Long managerId) {

        log.info("Fetching lots for managerId={}", managerId);

        List<ParkingLotResponseDto> response = parkingLotService.getLotsByManager(managerId);

        log.info("Found {} lots for managerId={}", response.size(), managerId);
        return ResponseEntity.ok(response);
    }

    // Update lot (Manager/Admin)
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @PutMapping("/{lotId}")
    public ResponseEntity<ParkingLotResponseDto> updateLot(@PathVariable Long lotId,
                                                           @Valid @RequestBody ParkingLotRequestDto requestDto) {

        log.info("Updating parking lot. lotId={}", lotId);

        ParkingLotResponseDto response = parkingLotService.updateLot(lotId, requestDto);

        log.info("Parking lot updated successfully. lotId={}", lotId);
        return ResponseEntity.ok(response);
    }

    // Toggle open (Manager/Admin)
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @PatchMapping("/{lotId}/toggle-open")
    public ResponseEntity<Void> toggleOpen(@PathVariable Long lotId) {

        log.info("Toggling open status. lotId={}", lotId);

        parkingLotService.toggleOpen(lotId);

        log.info("Open status toggled. lotId={}", lotId);
        return ResponseEntity.noContent().build();
    }

    // Delete lot (Admin only)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{lotId}")
    public ResponseEntity<Void> deleteLot(@PathVariable Long lotId) {

        log.info("Deleting parking lot. lotId={}", lotId);

        parkingLotService.deleteLot(lotId);

        log.info("Parking lot deleted. lotId={}", lotId);
        return ResponseEntity.noContent().build();
    }

    //  INTERNAL (only system/admin)
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{lotId}/decrement-available")
    public ResponseEntity<Void> decrementAvailable(@PathVariable Long lotId) {

        log.info("Decrementing available spots. lotId={}", lotId);

        parkingLotService.decrementAvailable(lotId);

        log.info("Decrement completed. lotId={}", lotId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{lotId}/increment-available")
    public ResponseEntity<Void> incrementAvailable(@PathVariable Long lotId) {

        log.info("Incrementing available spots. lotId={}", lotId);

        parkingLotService.incrementAvailable(lotId);

        log.info("Increment completed. lotId={}", lotId);
        return ResponseEntity.noContent().build();
    }

    // Public search
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/search")
    public ResponseEntity<List<ParkingLotResponseDto>> searchLots(@RequestParam String keyword) {

        log.info("Searching parking lots. keyword={}", keyword);

        List<ParkingLotResponseDto> response = parkingLotService.searchLots(keyword);

        log.info("Search results count={}", response.size());
        return ResponseEntity.ok(response);
    }
}