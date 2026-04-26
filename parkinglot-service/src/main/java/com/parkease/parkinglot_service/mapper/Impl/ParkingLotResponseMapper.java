package com.parkease.parkinglot_service.mapper.Impl;

import com.parkease.parkinglot_service.dtos.ParkingLotResponseDto;
import com.parkease.parkinglot_service.entity.ParkingLot;
import com.parkease.parkinglot_service.mapper.Mapper;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParkingLotResponseMapper implements Mapper<ParkingLotResponseDto, ParkingLot> {

    private final ModelMapper modelMapper;

    @Override
    public ParkingLotResponseDto mapTo(ParkingLot parkingLot) {
        return modelMapper.map(parkingLot, ParkingLotResponseDto.class);
    }

    @Override
    public ParkingLot mapFrom(ParkingLotResponseDto parkingLotResponseDto) {
        return modelMapper.map(parkingLotResponseDto, ParkingLot.class);
    }
}
