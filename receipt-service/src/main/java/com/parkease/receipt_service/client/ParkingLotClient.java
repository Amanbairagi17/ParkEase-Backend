package com.parkease.receipt_service.client;

import com.parkease.receipt_service.dtos.ParkingLotResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "PARKINGLOT-SERVICE", path = "/api/internal/parking-lots")
public interface ParkingLotClient {

    @GetMapping("/{lotId}")
    ParkingLotResponseDto getLot(@PathVariable("lotId") Long lotId);
}