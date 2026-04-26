package com.parkease.parkingspot_service.client;

import com.parkease.parkingspot_service.dtos.ParkingLotLookupResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "PARKINGLOT-SERVICE", path = "/api/parking-lots")
public interface ParkingLotServiceClient {

    @GetMapping("/{lotId}")
    ParkingLotLookupResponseDto getLotById(@PathVariable("lotId") Long lotId);
}
