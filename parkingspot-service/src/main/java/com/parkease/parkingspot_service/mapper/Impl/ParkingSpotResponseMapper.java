package com.parkease.parkingspot_service.mapper.Impl;

import com.parkease.parkingspot_service.dtos.ParkingSpotResponseDto;
import com.parkease.parkingspot_service.entity.ParkingSpot;
import com.parkease.parkingspot_service.mapper.Mapper;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParkingSpotResponseMapper implements Mapper<ParkingSpotResponseDto, ParkingSpot> {

    private final ModelMapper modelMapper;

    @Override
    public ParkingSpotResponseDto mapTo(ParkingSpot parkingSpot) {
        return modelMapper.map(parkingSpot, ParkingSpotResponseDto.class);
    }

    @Override
    public ParkingSpot mapFrom(ParkingSpotResponseDto parkingSpotResponseDto) {
        return modelMapper.map(parkingSpotResponseDto, ParkingSpot.class);
    }
}
