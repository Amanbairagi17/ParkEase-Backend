package com.parkease.parkinglot_service.service;

import com.parkease.parkinglot_service.dtos.ParkingLotRequestDto;
import com.parkease.parkinglot_service.dtos.ParkingLotResponseDto;

import java.util.List;

public interface ParkingLotService {

    ParkingLotResponseDto createLot(ParkingLotRequestDto requestDto);

    ParkingLotResponseDto getLotById(Long lotId);

    List<ParkingLotResponseDto> getLotsByCity(String city);

    List<ParkingLotResponseDto> getNearbyLots(double latitude, double longitude, double radiusKm);

    List<ParkingLotResponseDto> getLotsByManager(Long managerId);

    ParkingLotResponseDto updateLot(Long lotId, ParkingLotRequestDto requestDto);

    void toggleOpen(Long lotId);

    void deleteLot(Long lotId);

    void decrementAvailable(Long lotId);

    void incrementAvailable(Long lotId);

    List<ParkingLotResponseDto> searchLots(String keyword);
}
