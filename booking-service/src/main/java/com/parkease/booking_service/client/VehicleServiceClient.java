package com.parkease.booking_service.client;

import com.parkease.booking_service.dtos.VehicleLookupResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "VEHICLE-SERVICE", path = "/api/vehicle")
public interface VehicleServiceClient {

    @GetMapping("/getByLicensePlate/{licensePlate}")
    VehicleLookupResponseDto getByLicensePlate(@PathVariable("licensePlate") String licensePlate);
}
