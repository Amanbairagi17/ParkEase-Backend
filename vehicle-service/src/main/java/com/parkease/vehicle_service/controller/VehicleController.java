package com.parkease.vehicle_service.controller;

import com.parkease.vehicle_service.dtos.VehicleRequestDto;
import com.parkease.vehicle_service.service.Impl.VehicleServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.parkease.vehicle_service.dtos.VehicleResponseDto;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(path = "/api/vehicle")
@RequiredArgsConstructor
@Slf4j
public class VehicleController {

    private final VehicleServiceImpl vehicleService;

    @PostMapping("/register")
    public ResponseEntity<VehicleResponseDto> registerVehicle(@RequestBody VehicleRequestDto requestDto){
        log.info("Request received to register vehicle. licensePlate={}", requestDto.getLicensePlate());
        VehicleResponseDto response = vehicleService.createVehicle(requestDto);
        log.info("Returning response for vehicle registration. vehicleId={}", response.getVehicleId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/getById/{vehicleId}")
    public ResponseEntity<VehicleResponseDto> getVehicleById(@PathVariable Long vehicleId){
        VehicleResponseDto response = vehicleService.findVehicleById(vehicleId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/getVehiclesByOwner/{ownerId}")
    public ResponseEntity<List<VehicleResponseDto>> getVehiclesByOwner(@PathVariable Long ownerId){
        log.info("Request received to fetch vehicles. ownerId={}", ownerId);
        List<VehicleResponseDto> response = vehicleService.getVehiclesByOwner(ownerId);
        log.info("Returning response for fetch vehicles. ownerId={}, vehicleCount={}", ownerId, response.size());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/getByLicensePlate/{licensePlate}")
    public ResponseEntity<VehicleResponseDto> getByLicensePlate(@PathVariable String licensePlate){
        VehicleResponseDto response = vehicleService.getByLicensePlate(licensePlate);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/update/{vehicleId}")
    public ResponseEntity<VehicleResponseDto> updateVehicle(@PathVariable Long vehicleId, @RequestBody VehicleRequestDto requestDto){
        VehicleResponseDto response = vehicleService.updateVehicle(vehicleId, requestDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/delete/{vehicleId}")
    public ResponseEntity<String> deleteVehicle(@PathVariable Long vehicleId){
        log.info("Request received to delete vehicle. vehicleId={}", vehicleId);
        vehicleService.deleteVehicle(vehicleId);
        log.info("Returning response for delete vehicle. vehicleId={}", vehicleId);
        return ResponseEntity.status(HttpStatus.OK).body("Vehicle deleted successfully");
    }

    @GetMapping("/getVehicleType/{vehicleId}")
    public ResponseEntity<String> getVehicleType(@PathVariable Long vehicleId){
        String response = vehicleService.getVehicleType(vehicleId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/isEVVehicle/{vehicleId}")
    public ResponseEntity<Boolean> isEVVehicle(@PathVariable Long vehicleId){
        Boolean response = vehicleService.isEVVehicle(vehicleId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/getAllVehicles")
    public ResponseEntity<List<VehicleResponseDto>> getAllVehicles(){
        List<VehicleResponseDto> response = vehicleService.getAllVehicles();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/types")
    public ResponseEntity<List<String>> getAvailableVehicleTypes() {
        return ResponseEntity.status(HttpStatus.OK).body(List.of("2W", "3W", "4W", "HEAVY"));
    }

}
