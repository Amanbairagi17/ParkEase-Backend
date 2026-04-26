package com.parkease.parkinglot_service.service;

import com.parkease.parkinglot_service.dtos.ParkingLotRequestDto;
import com.parkease.parkinglot_service.dtos.ParkingLotResponseDto;

import java.util.List;

public interface ParkingLotService {

    ParkingLotResponseDto createLot(ParkingLotRequestDto requestDto);

    ParkingLotResponseDto getLotById(Integer lotId);

    List<ParkingLotResponseDto> getLotsByCity(String city);

    List<ParkingLotResponseDto> getNearbyLots(double latitude, double longitude, double radiusKm);

    List<ParkingLotResponseDto> getLotsByManager(Integer managerId);

    ParkingLotResponseDto updateLot(Integer lotId, ParkingLotRequestDto requestDto);

    void toggleOpen(Integer lotId);

    void deleteLot(Integer lotId);

    void decrementAvailable(Integer lotId);

    void incrementAvailable(Integer lotId);

    List<ParkingLotResponseDto> searchLots(String keyword);
}
