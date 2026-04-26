package com.parkease.parkingspot_service.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ParkingSpotBulkRequestDto {

    @NotEmpty(message = "At least one spot request is required")
    @Valid
    private List<ParkingSpotRequestDto> spots;
}
