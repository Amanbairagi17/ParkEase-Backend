package com.parkease.parkingspot_service.mapper.Impl;

import com.parkease.parkingspot_service.dtos.ParkingSpotRequestDto;
import com.parkease.parkingspot_service.entity.ParkingSpot;
import com.parkease.parkingspot_service.mapper.Mapper;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParkingSpotRequestMapper implements Mapper<ParkingSpotRequestDto, ParkingSpot> {

    private final ModelMapper modelMapper;

    @Override
    public ParkingSpotRequestDto mapTo(ParkingSpot parkingSpot) {
        return modelMapper.map(parkingSpot, ParkingSpotRequestDto.class);
    }

    @Override
    public ParkingSpot mapFrom(ParkingSpotRequestDto parkingSpotRequestDto) {
        return modelMapper.map(parkingSpotRequestDto, ParkingSpot.class);
    }
}
