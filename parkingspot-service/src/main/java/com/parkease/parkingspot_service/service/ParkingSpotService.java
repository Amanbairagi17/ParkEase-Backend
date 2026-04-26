package com.parkease.parkingspot_service.service;

import com.parkease.parkingspot_service.dtos.ParkingSpotBulkRequestDto;
import com.parkease.parkingspot_service.dtos.ParkingSpotRequestDto;
import com.parkease.parkingspot_service.dtos.ParkingSpotResponseDto;
import com.parkease.parkingspot_service.entity.SpotType;
import com.parkease.parkingspot_service.entity.VehicleType;

import java.util.List;

public interface ParkingSpotService {

    ParkingSpotResponseDto addSpot(ParkingSpotRequestDto requestDto);

    List<ParkingSpotResponseDto> addBulkSpots(ParkingSpotBulkRequestDto requestDto);

    ParkingSpotResponseDto getSpotById(Long spotId);

    List<ParkingSpotResponseDto> getSpotsByLot(Long lotId);

    List<ParkingSpotResponseDto> getAvailableSpots(Long lotId);

    List<ParkingSpotResponseDto> getByTypeAndLot(Long lotId, SpotType spotType);

    List<ParkingSpotResponseDto> getByVehicleTypeAndLot(Long lotId, VehicleType vehicleType);

    List<ParkingSpotResponseDto> getEvChargingSpots(boolean enabled);

    ParkingSpotResponseDto reserveSpot(Long spotId);

    ParkingSpotResponseDto occupySpot(Long spotId);

    ParkingSpotResponseDto releaseSpot(Long spotId);

    ParkingSpotResponseDto updateSpot(Long spotId, ParkingSpotRequestDto requestDto);

    void deleteSpot(Long spotId);

    long countAvailable(Long lotId);
}
