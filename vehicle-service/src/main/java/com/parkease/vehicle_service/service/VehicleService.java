package com.parkease.vehicle_service.service;

import com.parkease.vehicle_service.dtos.VehicleRequestDto;
import com.parkease.vehicle_service.dtos.VehicleResponseDto;

import java.util.List;

public interface VehicleService {
    VehicleResponseDto createVehicle(VehicleRequestDto requestDto);

    VehicleResponseDto findVehicleById(Long vehicleId);

    List<VehicleResponseDto> getVehiclesByOwner(Long ownerId);

    VehicleResponseDto getByLicensePlate(String licensePlate);

    VehicleResponseDto updateVehicle(Long vehicleId, VehicleRequestDto requestDto);

    void deleteVehicle(Long vehicleId);

    String getVehicleType(Long vehicleId);

    Boolean isEVVehicle(Long vehicleId);

    List<VehicleResponseDto> getAllVehicles();
}
