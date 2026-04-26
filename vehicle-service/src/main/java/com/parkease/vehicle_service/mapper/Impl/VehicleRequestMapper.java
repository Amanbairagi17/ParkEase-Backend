package com.parkease.vehicle_service.mapper.Impl;

import com.parkease.vehicle_service.dtos.VehicleRequestDto;
import com.parkease.vehicle_service.entity.Vehicle;
import com.parkease.vehicle_service.mapper.Mapper;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VehicleRequestMapper implements Mapper<VehicleRequestDto, Vehicle> {
    private final ModelMapper mapper;

    @Override
    public VehicleRequestDto mapTo(Vehicle vehicle) {
        return mapper.map(vehicle, VehicleRequestDto.class);
    }

    @Override
    public Vehicle mapFrom(VehicleRequestDto vehicleRequestDto) {
        return mapper.map(vehicleRequestDto, Vehicle.class);
    }
}
