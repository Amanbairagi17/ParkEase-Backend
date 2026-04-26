package com.parkease.vehicle_service.mapper.Impl;

import com.parkease.vehicle_service.entity.Vehicle;
import com.parkease.vehicle_service.mapper.Mapper;
import com.parkease.vehicle_service.dtos.VehicleResponseDto;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VehicleResponseMapper implements Mapper<VehicleResponseDto, Vehicle> {
    private final ModelMapper mapper;

    @Override
    public VehicleResponseDto mapTo(Vehicle vehicle) {
        return mapper.map(vehicle, VehicleResponseDto.class);
    }

    @Override
    public Vehicle mapFrom(VehicleResponseDto vehicleResponseDto) {
        return mapper.map(vehicleResponseDto, Vehicle.class);
    }
}
