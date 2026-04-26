package com.parkease.parkinglot_service.mapper.Impl;

import com.parkease.parkinglot_service.dtos.ParkingLotRequestDto;
import com.parkease.parkinglot_service.entity.ParkingLot;
import com.parkease.parkinglot_service.mapper.Mapper;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParkingLotRequestMapper implements Mapper<ParkingLotRequestDto, ParkingLot> {

    private final ModelMapper modelMapper;

    @Override
    public ParkingLotRequestDto mapTo(ParkingLot parkingLot) {
        return modelMapper.map(parkingLot, ParkingLotRequestDto.class);
    }

    @Override
    public ParkingLot mapFrom(ParkingLotRequestDto parkingLotRequestDto) {
        return modelMapper.map(parkingLotRequestDto, ParkingLot.class);
    }
}
